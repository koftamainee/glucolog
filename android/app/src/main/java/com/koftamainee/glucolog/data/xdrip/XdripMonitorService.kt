package com.koftamainee.glucolog.data.xdrip

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.koftamainee.glucolog.R

class XdripMonitorService : Service() {

    private var receiver: XdripBroadcastReceiver? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        registerXdripReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val restart = Intent(applicationContext, XdripMonitorService::class.java)
        restart.action = ACTION_RESTART
        ContextCompat.startForegroundService(applicationContext, restart)
    }

    override fun onDestroy() {
        receiver?.let { unregisterReceiver(it) }
        receiver = null
        super.onDestroy()
    }

    private fun registerXdripReceiver() {
        val r = XdripBroadcastReceiver()
        val filter = IntentFilter(XdripBroadcastReceiver.ACTION_BG_ESTIMATE)
        ContextCompat.registerReceiver(
            this,
            r,
            filter,
            ContextCompat.RECEIVER_EXPORTED,
        )
        receiver = r
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.xdrip_service_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            )
            channel.setShowBadge(false)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_glucose)
            .setContentTitle(getString(R.string.xdrip_service_title))
            .setContentText(getString(R.string.xdrip_service_text))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    companion object {
        private const val TAG = "XdripMonitorService"
        private const val CHANNEL_ID = "xdrip_monitor"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_RESTART = "com.koftamainee.glucolog.action.RESTART_MONITOR"

        fun start(context: Context) {
            val intent = Intent(context, XdripMonitorService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
