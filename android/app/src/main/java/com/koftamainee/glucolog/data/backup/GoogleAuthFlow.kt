package com.koftamainee.glucolog.data.backup

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.util.Log
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import com.koftamainee.glucolog.data.SettingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class GoogleAuthFlow(
    private val appContext: Context,
    private val settings: SettingsDataStore,
) {

    sealed class Outcome {
        data class Success(val email: String?, val accessToken: String?) : Outcome()
        data class NeedsResolution(val sender: IntentSender) : Outcome()
        data class Failure(val message: String) : Outcome()
    }

    suspend fun authorize(): Outcome = withContext(Dispatchers.IO) {
        try {
            val client = Identity.getAuthorizationClient(appContext)
            val request = buildRequest(currentPrompt())
            logRequest(request)
            val result = Tasks.await(client.authorize(request))
            outcomeFrom(result, "initial")
        } catch (e: Exception) {
            Log.e(TAG, "authorize failed", e)
            Outcome.Failure("Не удалось авторизоваться")
        }
    }

    suspend fun completeResolution(resultCode: Int?, data: Intent?): Outcome =
        withContext(Dispatchers.IO) {
            try {
                val client = Identity.getAuthorizationClient(appContext)
                if (data != null && resultCode == -1) {
                    try {
                        val result = client.getAuthorizationResultFromIntent(data)
                        logResult("from-intent", result)
                        if (!GoogleDriveClient.accessTokenFrom(result).isNullOrEmpty()) {
                            return@withContext outcomeFrom(result, "from-intent")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "getAuthorizationResultFromIntent failed", e)
                    }
                }
                val request = buildRequest(currentPrompt())
                var result = Tasks.await(client.authorize(request))
                logResult("after-resolution", result)
                if (result.hasResolution() &&
                    GoogleDriveClient.accessTokenFrom(result).isNullOrEmpty()
                ) {
                    delay(700)
                    result = Tasks.await(client.authorize(request))
                    logResult("after-retry", result)
                }
                outcomeFrom(result, "after-resolution")
            } catch (e: Exception) {
                Log.e(TAG, "completeResolution failed", e)
                Outcome.Failure("Вход в Google не выполнен")
            }
        }

    suspend fun signOut() {
        withContext(Dispatchers.IO) {
            val client = Identity.getAuthorizationClient(appContext)
            var token: String? = null
            try {
                val result = Tasks.await(client.authorize(buildRequest(AuthorizationRequest.Prompt.NOT_SET)))
                token = GoogleDriveClient.accessTokenFrom(result)
                if (!token.isNullOrEmpty()) {
                    Tasks.await(
                        client.clearToken(ClearTokenRequest.builder().setToken(token).build())
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "clearToken failed", e)
            }
            if (!token.isNullOrEmpty()) {
                try {
                    GoogleDriveClient.revokeToken(token)
                } catch (e: Exception) {
                    Log.w(TAG, "revokeToken failed", e)
                }
            }
            GoogleDriveClient.clearAuthCache()
        }
        settings.setBackupGoogleEmail(null)
        settings.setBackupEnabled(false)
        settings.setBackupLastError(null)
        settings.setGoogleForcePicker(true)
    }

    suspend fun applySuccess(email: String?): String? {
        settings.setBackupGoogleEmail(email)
        settings.setBackupLastError(null)
        settings.setGoogleForcePicker(false)
        return email
    }

    private suspend fun currentPrompt(): Int =
        if (settings.googleForcePicker.first()) {
            AuthorizationRequest.Prompt.SELECT_ACCOUNT
        } else {
            AuthorizationRequest.Prompt.NOT_SET
        }

    private suspend fun outcomeFrom(result: AuthorizationResult, stage: String): Outcome {
        logResult(stage, result)
        val token = GoogleDriveClient.accessTokenFrom(result)
            if (!token.isNullOrEmpty()) {
                var email = GoogleDriveClient.emailFromResult(result)
                if (email.isNullOrEmpty()) {
                    email = GoogleDriveClient.resolveEmail(token)
                }
                return Outcome.Success(email, token)
            }
        if (result.hasResolution()) {
            val sender = result.pendingIntent?.intentSender
            if (sender != null) return Outcome.NeedsResolution(sender)
        }
        return Outcome.Failure("Доступ к Google Диску не предоставлен")
    }

    private fun logResult(stage: String, result: AuthorizationResult) {
        Log.d(
            TAG,
            "[$stage] token=${GoogleDriveClient.accessTokenFrom(result) != null} " +
                "hasResolution=${result.hasResolution()} " +
                "granted=${result.grantedScopes} " +
                "params=${result.tokenResponseParams?.keySet()}"
        )
    }

    companion object {
        private const val TAG = "GlucologAuth"

        fun requestedScopes(): List<Scope> = listOf(
            Scope("openid"),
            Scope("email"),
            Scope(GoogleDriveClient.DRIVE_SCOPE),
        )

        fun buildRequest(
            prompt: Int = AuthorizationRequest.Prompt.NOT_SET,
        ): AuthorizationRequest {
            val builder = AuthorizationRequest.builder()
                .setRequestedScopes(requestedScopes())
            if (prompt != AuthorizationRequest.Prompt.NOT_SET) {
                builder.setPrompt(prompt)
            }
            return builder.build()
        }

        private fun logRequest(request: AuthorizationRequest) {
            Log.d(TAG, "[request] prompt=${request.prompt} scopes=${request.requestedScopes}")
        }
    }
}
