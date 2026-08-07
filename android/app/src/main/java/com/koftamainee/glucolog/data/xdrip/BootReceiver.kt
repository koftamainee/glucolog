package com.koftamainee.glucolog.data.xdrip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.d(TAG, "boot completed, restarting monitor service")
        XdripMonitorService.start(context)
    }

    companion object {
        private const val TAG = "GlucologBootReceiver"
    }
}
