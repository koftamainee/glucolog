package com.koftamainee.glucolog.data.xiaomi.webservice

import android.os.Bundle

class WebServiceExternalStatus(bundle: Bundle) {

    var time: Long? = bundle.getLong("external.timeStamp", -1L).takeIf { it != -1L }

    var statusLine: String? = bundle.getString("external.statusLine").orEmpty().takeIf { it.isNotEmpty() }
}
