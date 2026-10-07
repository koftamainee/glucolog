package com.koftamainee.glucolog.data.backup

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class DriveAuthException(message: String) : Exception(message)

data class DriveBackup(
    val id: String,
    val name: String,
    val createdTime: String,
    val sizeBytes: Long,
)

data class DriveAuth(
    val accessToken: String,
    val email: String?,
)

class GoogleDriveClient {

    fun uploadBackup(accessToken: String, fileName: String, json: String, keepCount: Int) {
        val folderId = findOrCreateFolder(accessToken)
        uploadNewFile(accessToken, folderId, fileName, json)
        pruneOldBackups(accessToken, folderId, keepCount)
    }

    fun listBackups(accessToken: String): List<DriveBackup> {
        val folderId = findFolder(accessToken)
        if (folderId == null) {
            Log.d(TAG, "listBackups: folder not found")
            return emptyList()
        }
        val raw = listFiles(accessToken, backupsQuery(folderId))
        val result = raw.filter { isBackupFileName(it.name) }
            .sortedByDescending { it.createdTime }
        Log.d(TAG, "listBackups: folder=$folderId raw=${raw.size} filtered=${result.size} " +
            "names=${raw.take(5).map { it.name }}")
        return result
    }

    private fun backupsQuery(folderId: String): String =
        "'$folderId' in parents and trashed=false"

    private fun isBackupFileName(name: String): Boolean =
        name.startsWith("backup-") && name.endsWith(".json")

    fun downloadBackup(accessToken: String, fileId: String): String {
        val conn = openConn("$API/files/$fileId?alt=media", "GET", accessToken)
        try {
            val code = conn.responseCode
            if (code == 401 || code == 403) throw DriveAuthException("Drive: HTTP $code")
            if (code !in 200..299) throw IOException("Drive: HTTP $code ${readError(conn)}")
            return readBody(conn)
        } finally {
            conn.disconnect()
        }
    }

    private fun findFolder(accessToken: String): String? {
        val query = "name='$FOLDER_NAME' and mimeType='$FOLDER_MIME' and trashed=false"
        val found = listFiles(accessToken, query).firstOrNull()
        Log.d(TAG, "findFolder: ${found?.id ?: "not found"}")
        return found?.id
    }

    private fun findOrCreateFolder(accessToken: String): String {
        findFolder(accessToken)?.let { return it }

        val body = JSONObject()
            .put("name", FOLDER_NAME)
            .put("mimeType", FOLDER_MIME)
        val conn = openConn("$API/files", "POST", accessToken)
        try {
            writeJson(conn, body.toString())
            val code = conn.responseCode
            if (code == 401 || code == 403) throw DriveAuthException("Drive: HTTP $code")
            if (code !in 200..299) throw IOException("Drive: HTTP $code ${readError(conn)}")
            val id = JSONObject(readBody(conn)).optString("id")
            if (id.isNullOrEmpty()) throw IOException("Drive: не удалось создать папку")
            return id
        } finally {
            conn.disconnect()
        }
    }

    private fun uploadNewFile(
        accessToken: String,
        folderId: String,
        fileName: String,
        json: String,
    ) {
        val meta = JSONObject()
            .put("name", fileName)
            .put("parents", JSONArray().put(folderId))
        val boundary = "glucolog-${System.currentTimeMillis()}"
        val conn = openConn("$UPLOAD/files?uploadType=multipart", "POST", accessToken)
        try {
            conn.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
            conn.doOutput = true
            conn.outputStream.use { out ->
                out.write("--$boundary\r\n".toByteArray())
                out.write("Content-Type: application/json; charset=UTF-8\r\n\r\n".toByteArray())
                out.write(meta.toString().toByteArray(Charsets.UTF_8))
                out.write("\r\n--$boundary\r\n".toByteArray())
                out.write("Content-Type: application/json; charset=UTF-8\r\n\r\n".toByteArray())
                out.write(json.toByteArray(Charsets.UTF_8))
                out.write("\r\n--$boundary--\r\n".toByteArray())
            }
            val code = conn.responseCode
            if (code == 401 || code == 403) throw DriveAuthException("Drive: HTTP $code")
            if (code !in 200..299) throw IOException("Drive: HTTP $code ${readError(conn)}")
        } finally {
            conn.disconnect()
        }
    }

    private fun pruneOldBackups(accessToken: String, folderId: String, keepCount: Int) {
        if (keepCount < 1) return
        val files = listFiles(accessToken, backupsQuery(folderId))
            .filter { isBackupFileName(it.name) }
        if (files.size <= keepCount) return
        files.sortedByDescending { it.createdTime }
            .drop(keepCount)
            .forEach { file ->
                val conn = openConn("$API/files/${file.id}", "DELETE", accessToken)
                try {
                    val code = conn.responseCode
                    if (code == 401 || code == 403) throw DriveAuthException("Drive: HTTP $code")
                    if (code !in 200..299) {
                        throw IOException("Drive: HTTP $code ${readError(conn)}")
                    }
                } finally {
                    conn.disconnect()
                }
            }
    }

    private fun listFiles(accessToken: String, query: String): List<DriveBackup> {
        val fields = "files(id,name,createdTime,size)"
        val url = "$API/files?q=${URLEncoder.encode(query, "UTF-8")}" +
            "&fields=${URLEncoder.encode(fields, "UTF-8")}" +
            "&pageSize=100&spaces=drive"
        val conn = openConn(url, "GET", accessToken)
        try {
            val code = conn.responseCode
            if (code == 401 || code == 403) throw DriveAuthException("Drive: HTTP $code")
            if (code !in 200..299) throw IOException("Drive: HTTP $code ${readError(conn)}")
            val files = JSONObject(readBody(conn)).optJSONArray("files") ?: return emptyList()
            val result = mutableListOf<DriveBackup>()
            for (i in 0 until files.length()) {
                val o = files.optJSONObject(i) ?: continue
                val id = o.optString("id")
                if (id.isNullOrEmpty()) continue
                result.add(
                    DriveBackup(
                        id = id,
                        name = o.optString("name"),
                        createdTime = o.optString("createdTime"),
                        sizeBytes = o.optLong("size", 0L),
                    )
                )
            }
            return result
        } finally {
            conn.disconnect()
        }
    }

    private fun openConn(url: String, method: String, accessToken: String): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.setRequestProperty("Authorization", "Bearer $accessToken")
        conn.connectTimeout = 20_000
        conn.readTimeout = 60_000
        return conn
    }

    private fun writeJson(conn: HttpURLConnection, body: String) {
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.doOutput = true
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
    }

    private fun readBody(conn: HttpURLConnection): String =
        (conn.inputStream ?: conn.errorStream).use { stream ->
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
        }

    private fun readError(conn: HttpURLConnection): String = try {
        conn.errorStream?.use { stream ->
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText().take(500)
        } ?: ""
    } catch (e: Exception) {
        ""
    }

    companion object {
        private const val TAG = "GlucologDrive"
        const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.file"
        const val FOLDER_NAME = "Glucolog"
        private const val FOLDER_MIME = "application/vnd.google-apps.folder"
        private const val API = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"

        fun buildAuthorizationRequest(): AuthorizationRequest =
            GoogleAuthFlow.buildRequest()

        fun accessTokenFrom(result: AuthorizationResult): String? =
            result.accessToken?.takeIf { it.isNotEmpty() }
                ?: result.tokenResponseParams?.getString("access_token")?.takeIf { it.isNotEmpty() }

        fun revokeToken(accessToken: String) {
            val conn = URL("https://oauth2.googleapis.com/revoke")
                .openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                conn.doOutput = true
                conn.outputStream.use { out ->
                    out.write("token=$accessToken".toByteArray(Charsets.UTF_8))
                }
                conn.responseCode
            } finally {
                conn.disconnect()
            }
        }

        fun resolveEmail(accessToken: String): String? = try {
            val conn = URL("https://oauth2.googleapis.com/tokeninfo?access_token=$accessToken")
                .openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            try {
                val body = (conn.inputStream ?: conn.errorStream).use { stream ->
                    BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
                }
                JSONObject(body).optString("email").takeIf { it.isNotEmpty() }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            null
        }

        suspend fun authorizeSilently(context: Context): DriveAuth =
            withContext(Dispatchers.IO) {
                freshCachedAuth()?.let {
                    Log.d(TAG, "silent: using cached token")
                    return@withContext it
                }
                Log.d(TAG, "silent: GMS authorize…")
                val client = Identity.getAuthorizationClient(context)
                val result = try {
                    var r = Tasks.await(client.authorize(buildAuthorizationRequest()))
                    if (accessTokenFrom(r).isNullOrEmpty() && r.hasResolution()) {
                        Log.d(TAG, "silent: no token, retrying in 700ms")
                        delay(700)
                        r = Tasks.await(client.authorize(buildAuthorizationRequest()))
                    }
                    r
                } catch (e: Exception) {
                    Log.e(TAG, "silent: GMS authorize failed", e)
                    throw DriveAuthException("Требуется вход в Google")
                }
                val token = accessTokenFrom(result)
                if (token.isNullOrEmpty()) {
                    Log.w(TAG, "silent: no token hasResolution=${result.hasResolution()}")
                    if (result.hasResolution()) {
                        throw DriveAuthException("Требуется вход в Google")
                    }
                    throw DriveAuthException("Нет токена доступа")
                }
                Log.d(TAG, "silent: token ok")
                DriveAuth(
                    accessToken = token,
                    email = emailFromResult(result) ?: resolveEmail(token),
                ).also { cacheAuth(it) }
            }

        @Volatile
        private var cachedAuth: DriveAuth? = null

        @Volatile
        private var cachedAt: Long = 0L

        fun cacheAuth(auth: DriveAuth) {
            cachedAuth = auth
            cachedAt = System.currentTimeMillis()
        }

        fun clearAuthCache() {
            cachedAuth = null
            cachedAt = 0L
        }

        private fun freshCachedAuth(): DriveAuth? {
            val auth = cachedAuth ?: return null
            if (System.currentTimeMillis() - cachedAt > 45 * 60_000L) return null
            return auth
        }

        fun emailFromResult(result: AuthorizationResult): String? {
            val idToken = result.tokenResponseParams?.getString("id_token")
            if (idToken.isNullOrBlank()) return null
            return try {
                val parts = idToken.split(".")
                if (parts.size < 2) return null
                val payload = Base64.decode(
                    parts[1],
                    Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
                )
                JSONObject(String(payload, Charsets.UTF_8))
                    .optString("email")
                    .takeIf { it.isNotEmpty() }
            } catch (e: Exception) {
                null
            }
        }

        fun backupFileName(): String {
            val ts = java.time.Instant.now()
                .toString()
                .replace(':', '-')
                .replace('.', '-')
            return "backup-$ts.json"
        }
    }
}
