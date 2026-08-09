package com.koftamainee.glucolog.data.xdrip

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.d(TAG, "boot completed, restarting services")
        CoroutineScope(Dispatchers.IO).launch {
            var blocked = false
            try {
                XdripMonitorService.start(context)
            } catch (e: Exception) {
                Log.w(TAG, "monitor start blocked", e)
                blocked = true
            }
            if (blocked) scheduleRestart(context)
        }
    }

    private fun scheduleRestart(context: Context) {
        val pending = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, BootRestartReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + RESTART_DELAY_MS
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                alarm.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        } catch (e: SecurityException) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    companion object {
        private const val TAG = "GlucologBootReceiver"
        private const val RESTART_DELAY_MS = 30_000L
    }
}
