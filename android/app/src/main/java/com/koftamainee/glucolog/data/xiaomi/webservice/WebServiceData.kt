package com.koftamainee.glucolog.data.xiaomi.webservice

import android.os.Bundle
import com.google.gson.Gson
import com.koftamainee.glucolog.data.xiaomi.BgData

class WebServiceData(
    bgData: BgData,
    bgDataBundle: Bundle,
    includeGraph: Boolean,
    lowRange: String,
    inRange: String,
    highRange: String,
    battery: Int,
) {

    var status: WebServiceStatus = WebServiceStatus(bgData.isDoMgdl(), battery)

    var bg: WebServiceBgInfo = WebServiceBgInfo(bgData, lowRange, inRange, highRange)

    var treatment: WebServiceTreatment = WebServiceTreatment(bgDataBundle)

    var pump: WebServicePump = WebServicePump(bgDataBundle)

    var external: WebServiceExternalStatus = WebServiceExternalStatus(bgDataBundle)

    var graph: WebServiceGraphData? = null

    init {
        if (includeGraph) {
            graph = WebServiceGraphData(bgDataBundle)
        }
    }

    fun toJson(): String = Gson().toJson(this)
}
