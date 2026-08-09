package com.koftamainee.glucolog.data.xiaomi.webservice

class WebServiceStatus(isMgdl: Boolean, battery: Int) {

    var now: Long = System.currentTimeMillis()

    var isMgdl: Boolean = isMgdl

    var bat: Int = battery
}
