package com.koftamainee.glucolog.data.xiaomi

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.koftamainee.glucolog.R
import test.invoke.sdk.XiaomiWatchHelper

class XiaomiWatchService : Service() {

    private lateinit var xiaomiWatchHelper: XiaomiWatchHelper
    private val handler = Handler(Looper.getMainLooper())
    private var sendConfirmed = false
    private var connected = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        xiaomiWatchHelper = XiaomiWatchHelper.getInstance(this)
        xiaomiWatchHelper.setReceiver { _, message ->
            val text = message?.toString(Charsets.UTF_8)
            Log.w(TAG, "got data: $text")
            text?.let { parseTimestamp(it) }?.let { lastConfirmedMs = it }
        }
        xiaomiWatchHelper.registerMessageReceiver()
        xiaomiWatchHelper.sendUpdateMessageToWear()
        Log.d(TAG, "service started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_UPDATE_BG -> {
                intent.getStringExtra(EXTRA_JSON)?.let {
                    lastJson = it
                    updateWearBg(it)
                }
            }

            ACTION_UPDATE_BG_FORCE -> updateWearBg(lastJson)

            null -> updateWearBg(lastJson)
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val restart = Intent(applicationContext, XiaomiWatchService::class.java)
        restart.action = ACTION_UPDATE_BG_FORCE
        ContextCompat.startForegroundService(applicationContext, restart)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        xiaomiWatchHelper.unRegisterWatchHelper()
        super.onDestroy()
    }

    private fun updateWearBg(json: String?) {
        if (json.isNullOrEmpty()) return
        Log.w(TAG, "updateWearBg connected=$connected")
        sendConfirmed = false
        scheduleRetry()
        if (connected) {
            runCatching { xiaomiWatchHelper.launchApp(WATCH_APP_PACKAGE) {} }
        } else {
            reconnect()
        }
        sendMessage(json)
    }

    private fun reconnect() {
        Log.w(TAG, "reconnecting to watch")
        xiaomiWatchHelper.setReCheckConnectDevice()
        xiaomiWatchHelper.sendUpdateMessageToWear()
    }

    private fun sendMessage(json: String) {
        xiaomiWatchHelper.sendMessageToWear(json) { status ->
            if (status.isSuccess) {
                connected = true
                lastSentMs = System.currentTimeMillis()
                if (!sendConfirmed) {
                    sendConfirmed = true
                    handler.removeCallbacksAndMessages(null)
                }
                runCatching { xiaomiWatchHelper.launchApp(WATCH_APP_PACKAGE) {} }
            } else {
                connected = false
                Log.w(TAG, "sendMessageToWear failed code=${status.code} (watch not connected?)")
            }
        }
    }

    private fun scheduleRetry() {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed(
            {
                if (!sendConfirmed) {
                    Log.w(TAG, "retry send last data")
                    updateWearBg(lastJson)
                }
            },
            RETRY_DELAY_MS,
        )
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.xiaomi_service_channel_name),
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
            .setContentTitle(getString(R.string.xiaomi_service_title))
            .setContentText(getString(R.string.xiaomi_service_text))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    companion object {
        private const val TAG = "XiaomiWatchService"
        private const val CHANNEL_ID = "xiaomi_watch"
        private const val NOTIFICATION_ID = 2
        private const val ACTION_UPDATE_BG = "update_bg"
        private const val ACTION_UPDATE_BG_FORCE = "update_bg_force"
        private const val EXTRA_JSON = "json"
        private const val WATCH_APP_PACKAGE = "com.application.watch.watchdrip"
        private const val RETRY_DELAY_MS = 10_000L

        @Volatile
        var lastJson: String? = null
            private set

        @Volatile
        var lastSentMs: Long = 0L
            private set

        @Volatile
        var lastConfirmedMs: Long = 0L
            private set

        fun sendJson(context: Context, json: String) {
            lastJson = json
            val intent = Intent(context, XiaomiWatchService::class.java)
                .setAction(ACTION_UPDATE_BG)
                .putExtra(EXTRA_JSON, json)
            ContextCompat.startForegroundService(context, intent)
        }

        fun start(context: Context) {
            val intent = Intent(context, XiaomiWatchService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, XiaomiWatchService::class.java))
        }

        private fun parseTimestamp(text: String): Long? {
            return runCatching {
                val start = text.indexOf("\"data\":\"") + "\"data\":\"".length
                val end = text.indexOf('"', start)
                text.substring(start, end).toLong()
            }.getOrNull()
        }
    }
}
