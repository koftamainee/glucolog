package com.koftamainee.glucolog.data.xdrip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.koftamainee.glucolog.GlucologApp
import com.koftamainee.glucolog.data.xiaomi.XiaomiWatchService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootRestartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_RESTART) return
        Log.d(TAG, "scheduled restart fired")
        val app = context.applicationContext as GlucologApp
        CoroutineScope(Dispatchers.IO).launch {
            try {
                XdripMonitorService.start(context)
            } catch (e: Exception) {
                Log.w(TAG, "monitor restart failed", e)
            }
            if (app.container.settingsDataStore.isXiaomiServiceEnabled()) {
                try {
                    XiaomiWatchService.start(context)
                } catch (e: Exception) {
                    Log.w(TAG, "xiaomi restart failed", e)
                }
            }
        }
    }

    companion object {
        const val ACTION_RESTART = "com.koftamainee.glucolog.action.BOOT_RESTART"
        private const val TAG = "GlucologBootRestart"
    }
}
