package com.koftamainee.glucolog.data.xiaomi.webservice

import android.os.Bundle
import com.eveningoutpost.dexdrip.services.broadcastservice.models.GraphLine
import com.koftamainee.glucolog.data.xiaomi.BgGraphComponents

class WebServiceGraphData(bundle: Bundle) {

    var lines: List<WebServiceGraphLine> = ArrayList()

    var start: Long = 0L

    var end: Long = 0L

    var fuzzer: Long = 0L

    init {
        val graphComponent = BgGraphComponents(bundle)
        val doMgdl = graphComponent.doMgdl
        addLine("high", graphComponent.highValues, doMgdl)
        addLine("inRange", graphComponent.inRangeValues, doMgdl)
        addLine("low", graphComponent.lowValues, doMgdl)
        addLine("predict", graphComponent.predictedBgValues, doMgdl)
        addLine("lineLow", graphComponent.lowLineValues, doMgdl)
        addLine("lineHigh", graphComponent.highLineValues, doMgdl)
        addLine("treatment", graphComponent.treatmentValues, doMgdl)
        start = graphComponent.getStart()
        end = graphComponent.getEnd()
        fuzzer = graphComponent.getFuzzer().toLong()
    }

    private fun addLine(name: String, line: GraphLine, doMgdl: Boolean) {
        if (line.values.isNotEmpty()) {
            lines = lines + WebServiceGraphLine(name, line, doMgdl)
        }
    }
}
