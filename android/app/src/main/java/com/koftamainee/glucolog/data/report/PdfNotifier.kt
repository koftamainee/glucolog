package com.koftamainee.glucolog.data.report

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.koftamainee.glucolog.R
import java.io.File

object PdfNotifier {

    private const val CHANNEL_ID = "pdf_report"
    private const val NOTIFICATION_ID = 1001
    private const val REQUEST_CODE = 1001
    private const val FILE_PATH = "reports"
    private const val FILE_NAME = "report.pdf"

    fun notifyPdfSaved(context: Context, bytes: ByteArray, fileName: String) {
        val file = File(File(context.cacheDir, FILE_PATH), FILE_NAME)
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)

        ensureChannel(context)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/pdf")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.clipData = ClipData.newRawUri(intent.type, uri)

        val contentIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_glucose)
            .setContentTitle(context.getString(R.string.pdf_report_notification_title))
            .setContentText(fileName)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.pdf_report_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            )
            channel.setShowBadge(false)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}