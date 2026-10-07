package com.koftamainee.glucolog.data.backup

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.koftamainee.glucolog.data.SettingsDataStore
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

class BackupScheduler(
    private val context: Context,
    private val settings: SettingsDataStore,
) {

    fun scheduleNext(delayMs: Long) = enqueue(delayMs, ExistingWorkPolicy.REPLACE)

    fun scheduleNextFromWorker(delayMs: Long) =
        enqueue(delayMs, ExistingWorkPolicy.APPEND_OR_REPLACE)

    fun runNow() = enqueue(0L, ExistingWorkPolicy.REPLACE)

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    private fun enqueue(delayMs: Long, policy: ExistingWorkPolicy) {
        val request = OneTimeWorkRequestBuilder<BackupWorker>()
            .setInitialDelay(delayMs.coerceAtLeast(0L), TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS,
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            policy,
            request,
        )
    }

    suspend fun rescheduleIfEnabled() {
        if (settings.backupEnabled.first()) {
            val hours = settings.backupIntervalHours.first().coerceIn(1, MAX_INTERVAL_HOURS)
            scheduleNext(hours * HOUR_MS)
        } else {
            cancel()
        }
    }

    companion object {
        const val WORK_NAME = "drive_backup"
        const val MAX_INTERVAL_HOURS = 24 * 365
        const val HOUR_MS = 3_600_000L
        const val MIN_KEEP_COUNT = 1
        const val MAX_KEEP_COUNT = 30
    }
}
