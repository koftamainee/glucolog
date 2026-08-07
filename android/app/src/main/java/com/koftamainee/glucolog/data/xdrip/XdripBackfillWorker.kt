package com.koftamainee.glucolog.data.xdrip

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.koftamainee.glucolog.GlucologApp

class XdripBackfillWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as GlucologApp
        val readings = try {
            XdripWebClient().fetchSgv(BACKFILL_COUNT)
        } catch (e: Exception) {
            return Result.success()
        }
        app.container.dayRepository.insertXdripReadings(readings)
        app.container.settingsDataStore.setXdripConnected(true)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "xdrip_backfill"
        const val BACKFILL_COUNT = 1000
    }
}
