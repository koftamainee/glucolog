package com.koftamainee.glucolog.data.xiaomi.webservice

import android.os.Bundle

class WebServiceTreatment(bundle: Bundle) {

    var insulin: Double? = bundle.getDouble("treatment.insulin", -1.0).takeIf { it != -1.0 }

    var carbs: Double? = bundle.getDouble("treatment.carbs", -1.0).takeIf { it != -1.0 }

    var time: Long? = bundle.getLong("treatment.timeStamp", -1L).takeIf { it != -1L }

    var predictIOB: String? = bundle.getString("predict.IOB").let { value ->
        if (value == null || value.isEmpty()) {
            null
        } else {
            value.replace(",", ".") + "u"
        }
    }

    var predictBWP: String? = bundle.getString("predict.BWP").let { value ->
        if (value == null || value.isEmpty()) {
            null
        } else {
            value
                .replace(",", ".")
                .replace("\u224F", "")
                .replace("\u26A0", "!")
        }
    }
}
