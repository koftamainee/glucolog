package com.koftamainee.glucolog.data.xdrip

import android.content.Context
import android.content.Intent
import com.eveningoutpost.dexdrip.services.broadcastservice.models.Settings
import com.koftamainee.glucolog.R

object XdripBroadcast {

    const val ACTION_WATCH_COMMUNICATION_RECEIVER =
        "com.eveningoutpost.dexdrip.watch.wearintegration.BROADCAST_SERVICE_RECEIVER"
    const val ACTION_WATCH_COMMUNICATION_SENDER =
        "com.eveningoutpost.dexdrip.watch.wearintegration.BROADCAST_SERVICE_SENDER"

    const val INTENT_FUNCTION_KEY = "FUNCTION"
    const val INTENT_PACKAGE_KEY = "PACKAGE"
    const val INTENT_SETTINGS = "SETTINGS"
    const val INTENT_REPLY_MSG = "REPLY_MSG"
    const val INTENT_REPLY_CODE = "REPLY_CODE"

    const val REPLY_CODE_OK = "OK"
    const val REPLY_CODE_ERROR = "ERROR"
    const val REPLY_CODE_NOT_REGISTERED = "NOT_REGISTERED"

    const val CMD_START = "start"
    const val CMD_UPDATE_BG = "update_bg"
    const val CMD_UPDATE_BG_FORCE = "update_bg_force"
    const val CMD_REPLY_MSG = "reply_msg"

    const val BG_VALUE_MGDL = "bg.valueMgdl"
    const val BG_TIMESTAMP = "bg.timeStamp"

    fun register(context: Context) {
        val settings = Settings().apply {
            apkName = context.getString(R.string.app_name)
            graphStart = 0
            graphEnd = 0
            displayGraph = false
        }
        val intent = Intent(ACTION_WATCH_COMMUNICATION_RECEIVER).apply {
            putExtra(INTENT_FUNCTION_KEY, CMD_UPDATE_BG_FORCE)
            putExtra(INTENT_PACKAGE_KEY, context.packageName)
            putExtra(INTENT_SETTINGS, settings)
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        }
        context.sendBroadcast(intent)
    }
}
