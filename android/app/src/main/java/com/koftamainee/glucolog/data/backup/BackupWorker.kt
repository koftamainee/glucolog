package com.koftamainee.glucolog.data.backup

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.koftamainee.glucolog.GlucologApp
import kotlinx.coroutines.flow.first

class BackupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as GlucologApp
        val container = app.container
        val settings = container.settingsDataStore

        if (!settings.backupEnabled.first()) return Result.success()

        if (settings.backupGoogleEmail.first() == null) {
            settings.setBackupLastError("Требуется вход в Google")
            container.backupScheduler.cancel()
            return Result.success()
        }

        return try {
            Log.d(TAG, "authorizing…")
            val auth = GoogleDriveClient.authorizeSilently(applicationContext)
            Log.d(TAG, "auth ok, email=${auth.email}")
            val includeDays = settings.backupDays.first()
            val includeProducts = settings.backupProducts.first()
            val days = if (includeDays) container.dayRepository.allDays() else null
            val products =
                if (includeProducts) container.productRepository.getAllProducts() else null
            Log.d(TAG, "days=${days?.size ?: "skip"} products=${products?.size ?: "skip"}")
            val json = BackupPayload.build(days = days, products = products)
            if (includeDays || includeProducts) {
                val keepCount = settings.backupKeepCount.first()
                    .coerceIn(BackupScheduler.MIN_KEEP_COUNT, BackupScheduler.MAX_KEEP_COUNT)
                Log.d(TAG, "uploading ${json.length} bytes, keep=$keepCount…")
                container.driveClient.uploadBackup(
                    accessToken = auth.accessToken,
                    fileName = GoogleDriveClient.backupFileName(),
                    json = json,
                    keepCount = keepCount,
                )
                Log.d(TAG, "upload ok")
            }
            settings.setBackupLastTime(System.currentTimeMillis())
            settings.setBackupLastError(null)
            val hours = settings.backupIntervalHours.first()
                .coerceIn(1, BackupScheduler.MAX_INTERVAL_HOURS)
            container.backupScheduler.scheduleNextFromWorker(hours * BackupScheduler.HOUR_MS)
            Log.d(TAG, "done, next in ${hours}h")
            Result.success()
        } catch (e: DriveAuthException) {
            Log.e(TAG, "auth error", e)
            GoogleDriveClient.clearAuthCache()
            settings.setBackupLastError("Требуется вход в Google")
            settings.setBackupGoogleEmail(null)
            settings.setBackupEnabled(false)
            container.backupScheduler.cancel()
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "backup failed", e)
            settings.setBackupLastError(e.message ?: "Ошибка бэкапа")
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "GlucologBackup"
    }
}
