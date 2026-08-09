package com.koftamainee.glucolog.data.xiaomi.webservice

import com.koftamainee.glucolog.data.xiaomi.BgData

class WebServiceBgInfo(bgData: BgData, lowRange: String, inRange: String, highRange: String) {

    var tir: String = inRange

    var tirLow: String = lowRange

    var tirHigh: String = highRange

    var `val`: String = bgData.unitizedBgValue()

    var delta: String = bgData.unitizedDelta()

    var trend: String? = bgData.getDeltaName()

    var isHigh: Boolean = bgData.isBgHigh()

    var isLow: Boolean = bgData.isBgLow()

    var time: Long = bgData.getTimeStamp()

    var isStale: Boolean = bgData.isStale()
}
