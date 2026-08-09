package com.koftamainee.glucolog.data.xiaomi

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object BgUnits {

    const val MMOLL_TO_MGDL = 18.0182
    const val MGDL_TO_MMOLL = 1.0 / MMOLL_TO_MGDL

    fun unitized(value: Double, doMgdl: Boolean): Double =
        if (doMgdl) value else value * MGDL_TO_MMOLL

    fun unitizedString(value: Double, doMgdl: Boolean): String {
        val df = DecimalFormat("#")
        return when {
            value >= 400 -> "HIGH"
            value >= 40 -> {
                if (doMgdl) {
                    df.maximumFractionDigits = 0
                    df.format(value)
                } else {
                    df.maximumFractionDigits = 1
                    df.minimumFractionDigits = 1
                    df.format(unitized(value, doMgdl))
                }
            }
            value > 12 -> "LOW"
            else -> when (value.toInt()) {
                0 -> "??0"
                1 -> "?SN"
                2 -> "??2"
                3 -> "?NA"
                5 -> "?NC"
                6 -> "?CD"
                9 -> "?AD"
                12 -> "?RF"
                else -> "???"
            }
        }
    }

    fun unitizedDeltaStringRaw(
        showUnit: Boolean,
        highGranularity: Boolean,
        value: Double,
        doMgdl: Boolean,
    ): String {
        if (Math.abs(value) > 100) return "ERR"
        val df = DecimalFormat("#", DecimalFormatSymbols(Locale.ENGLISH))
        val deltaSign = if (value > 0) "+" else ""
        if (doMgdl) {
            df.maximumFractionDigits = if (highGranularity) 1 else 0
            return deltaSign + df.format(unitized(value, doMgdl)) +
                if (showUnit) " mg/dl" else ""
        } else {
            if (highGranularity && Math.abs(value) < MMOLL_TO_MGDL * 0.1) {
                df.maximumFractionDigits = 2
            } else {
                df.maximumFractionDigits = 1
            }
            df.minimumFractionDigits = 1
            df.minimumIntegerDigits = 1
            return deltaSign + df.format(unitized(value, doMgdl)) +
                if (showUnit) " mmol/l" else ""
        }
    }
}
