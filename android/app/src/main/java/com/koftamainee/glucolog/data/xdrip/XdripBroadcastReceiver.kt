package com.koftamainee.glucolog.data.xdrip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.koftamainee.glucolog.GlucologApp
import com.koftamainee.glucolog.domain.GlucoseSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

class XdripBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive action=${intent.action}")
        if (intent.action != ACTION_BG_ESTIMATE) return
        val mgdl = intent.getDoubleExtra(EXTRA_BG_ESTIMATE, Double.NaN)
        val timeMs = intent.getLongExtra(EXTRA_TIME, -1L)
        if (mgdl.isNaN() || timeMs <= 0L) return

        val local = Instant.ofEpochMilli(timeMs).atZone(ZoneId.systemDefault())
        val mmol = (mgdl / MGDL_PER_MMOL).toFloat()
        val date = local.toLocalDate()
        val h = local.hour + local.minute / 60f

        Log.d(TAG, "xdrip bg received: ${date} ${h} -> ${mmol}")
        val app = context.applicationContext as GlucologApp
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.container.dayRepository.addGlucose(date, h, mmol, GlucoseSource.XDRIP)
                app.container.settingsDataStore.setXdripConnected(true)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_BG_ESTIMATE = "com.eveningoutpost.dexdrip.BgEstimate"
        private const val EXTRA_BG_ESTIMATE = "com.eveningoutpost.dexdrip.Extras.BgEstimate"
        private const val EXTRA_TIME = "com.eveningoutpost.dexdrip.Extras.Time"
        private const val MGDL_PER_MMOL = 18.016
        private const val TAG = "XdripReceiver"
    }
}
