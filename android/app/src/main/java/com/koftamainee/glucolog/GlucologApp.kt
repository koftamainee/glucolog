package com.koftamainee.glucolog

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.koftamainee.glucolog.data.xdrip.XdripBackfillWorker
import com.koftamainee.glucolog.di.AppContainer
import java.util.concurrent.TimeUnit

class GlucologApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        scheduleXdripBackfill()
    }

    private fun scheduleXdripBackfill() {
        val request = PeriodicWorkRequestBuilder<XdripBackfillWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            XdripBackfillWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
