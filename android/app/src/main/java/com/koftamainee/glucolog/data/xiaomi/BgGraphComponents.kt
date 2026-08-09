package com.koftamainee.glucolog.data.xiaomi

import android.os.Bundle
import com.eveningoutpost.dexdrip.services.broadcastservice.models.GraphLine

class BgGraphComponents(bundle: Bundle) {

    companion object {
        const val FUZZER = 500 * 15 * 5
    }

    var doMgdl: Boolean = bundle.getBoolean("doMgdl", true)

    val lowLineValues: GraphLine
    val highLineValues: GraphLine
    val inRangeValues: GraphLine
    val lowValues: GraphLine
    val highValues: GraphLine
    val iobValues: GraphLine
    val treatmentValues: GraphLine
    val predictedBgValues: GraphLine
    val cobValues: GraphLine
    val polyBgValues: GraphLine

    private val fuzzer: Int
    private val start: Long
    private val end: Long

    init {
        fuzzer = bundle.getInt("fuzzer").takeIf { it != 0 } ?: FUZZER
        end = bundle.getLong("end").takeIf { it != 0L }
            ?: (System.currentTimeMillis() / fuzzer)
        start = bundle.getLong("start").takeIf { it != 0L }
            ?: (end - (60000 * 180 / fuzzer))
        lowLineValues = parseGraphLine(bundle, "graph.lowLine")
        highLineValues = parseGraphLine(bundle, "graph.highLine")
        inRangeValues = parseGraphLine(bundle, "graph.inRange")
        lowValues = parseGraphLine(bundle, "graph.low")
        highValues = parseGraphLine(bundle, "graph.high")
        iobValues = parseGraphLine(bundle, "graph.iob")
        treatmentValues = parseGraphLine(bundle, "graph.treatment")
        predictedBgValues = parseGraphLine(bundle, "graph.predictedBg")
        cobValues = parseGraphLine(bundle, "graph.cob")
        polyBgValues = parseGraphLine(bundle, "graph.polyBg")
    }

    @Suppress("DEPRECATION")
    private fun parseGraphLine(bundle: Bundle, key: String): GraphLine =
        bundle.getParcelable<GraphLine>(key) ?: GraphLine()

    fun getFuzzer(): Int = fuzzer

    fun getStart(): Long = start / fuzzer

    fun getEnd(): Long = end / fuzzer
}
