package com.koftamainee.glucolog.data.xiaomi.webservice

import com.eveningoutpost.dexdrip.services.broadcastservice.models.GraphLine
import com.eveningoutpost.dexdrip.services.broadcastservice.models.GraphPoint

class WebServiceGraphLine(name: String, line: GraphLine, doMgdl: Boolean) {

    var points: MutableList<Array<Any?>> = ArrayList()

    var color: String = String.format("0x%06X", 0xFFFFFF and line.color)

    private var name: String = name

    init {
        for (point: GraphPoint in line.values) {
            points.add(
                arrayOf<Any?>(
                    point.x.toLong(),
                    if (doMgdl) point.y.toLong() else (Math.floor(point.y * 10.0) / 10.0).toFloat(),
                )
            )
        }
    }
}
