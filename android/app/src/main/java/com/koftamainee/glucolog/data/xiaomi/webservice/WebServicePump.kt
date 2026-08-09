package com.koftamainee.glucolog.data.xiaomi.webservice

import android.os.Bundle
import org.json.JSONObject

class WebServicePump(bundle: Bundle) {

    var reservoir: Double = 0.0

    var iob: Double = 0.0

    var bat: Double = 0.0

    init {
        bundle.getString("pumpJSON")?.let { pumpJson ->
            runCatching {
                val json = JSONObject(pumpJson)
                reservoir = json.optDouble("reservoir", 0.0)
                iob = json.optDouble("bolusiob", 0.0)
                bat = json.optDouble("battery", 0.0)
            }
        }
    }
}
