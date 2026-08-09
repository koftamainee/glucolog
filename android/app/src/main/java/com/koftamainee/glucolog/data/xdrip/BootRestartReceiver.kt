package com.koftamainee.glucolog.data.xdrip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootRestartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_RESTART) return
        Log.d(TAG, "scheduled restart fired")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                XdripMonitorService.start(context)
            } catch (e: Exception) {
                Log.w(TAG, "monitor restart failed", e)
            }
        }
    }

    companion object {
        const val ACTION_RESTART = "com.koftamainee.glucolog.action.BOOT_RESTART"
        private const val TAG = "GlucologBootRestart"
    }
}
