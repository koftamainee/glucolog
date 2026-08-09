package com.koftamainee.glucolog.data.xdrip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.util.Log
import com.koftamainee.glucolog.GlucologApp
import com.koftamainee.glucolog.data.db.GlucoseEntity
import com.koftamainee.glucolog.data.xiaomi.BgData
import com.koftamainee.glucolog.data.xiaomi.TirCalculator
import com.koftamainee.glucolog.data.xiaomi.XiaomiWatchService
import com.koftamainee.glucolog.data.xiaomi.webservice.WebServiceData
import com.koftamainee.glucolog.domain.GlucoseSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class XdripBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != XdripBroadcast.ACTION_WATCH_COMMUNICATION_SENDER) return
        val function = intent.getStringExtra(XdripBroadcast.INTENT_FUNCTION_KEY) ?: return

        if (function != XdripBroadcast.CMD_START &&
            intent.getPackage() != context.packageName
        ) {
            return
        }

        Log.d(TAG, "onReceive function=$function")
        when (function) {
            XdripBroadcast.CMD_START -> XdripBroadcast.register(context)

            XdripBroadcast.CMD_UPDATE_BG, XdripBroadcast.CMD_UPDATE_BG_FORCE ->
                handleBg(context, intent)

            XdripBroadcast.CMD_REPLY_MSG -> {
                val code = intent.getStringExtra(XdripBroadcast.INTENT_REPLY_CODE)
                if (code == XdripBroadcast.REPLY_CODE_NOT_REGISTERED ||
                    code == XdripBroadcast.REPLY_CODE_ERROR
                ) {
                    XdripBroadcast.register(context)
                }
            }
        }
    }

    private fun handleBg(context: Context, intent: Intent) {
        val mgdl = intent.getDoubleExtra(XdripBroadcast.BG_VALUE_MGDL, Double.NaN)
        val timeMs = intent.getLongExtra(XdripBroadcast.BG_TIMESTAMP, -1L)
        if (mgdl.isNaN() || timeMs <= 0L) return

        val local = Instant.ofEpochMilli(timeMs).atZone(ZoneId.systemDefault())
        val mmol = (mgdl / MGDL_PER_MMOL).toFloat()
        val date = local.toLocalDate()
        val h = local.hour + local.minute / 60f

        Log.d(TAG, "xdrip bg received: $date ${h} -> ${mmol}")
        val app = context.applicationContext as GlucologApp
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.container.dayRepository.addGlucose(date, h, mmol, GlucoseSource.XDRIP)
                app.container.settingsDataStore.setXdripConnected(true)
                XdripMonitorService.onBgReceived()
                if (app.container.settingsDataStore.isXiaomiServiceEnabled()) {
                    pushToWatch(app, context, intent)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun pushToWatch(app: GlucologApp, context: Context, intent: Intent) {
        val bundle = intent.extras ?: return
        val bgData = BgData(bundle)
        val tir = TirCalculator.calculate(last24hReadings(app))
        val json = WebServiceData(
            bgData = bgData,
            bgDataBundle = bundle,
            includeGraph = true,
            lowRange = tir.low.toString(),
            inRange = tir.inRange.toString(),
            highRange = tir.high.toString(),
            battery = batteryLevel(context),
        ).toJson()
        Log.d(TAG, "xiaomi push: $json")
        XiaomiWatchService.sendJson(context, json)
    }

    private suspend fun last24hReadings(app: GlucologApp): List<GlucoseEntity> {
        val now = LocalDateTime.now()
        return app.container.dayRepository.getGlucoseRange(
            now.toLocalDate().minusDays(1),
            now.toLocalDate(),
        )
    }

    private fun batteryLevel(context: Context): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return -1
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    companion object {
        private const val MGDL_PER_MMOL = 18.016
        private const val TAG = "XdripReceiver"
    }
}
