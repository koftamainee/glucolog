package com.koftamainee.glucolog.data.xiaomi

import android.os.Bundle

class BgData(bundle: Bundle) {

    private val deltaName: String?
    private val isBgHigh: Boolean
    private val isBgLow: Boolean
    private val noBgData: Boolean
    private val valueMgdl: Double
    private val deltaMgdl: Double
    private val timeStamp: Long
    private val isStale: Boolean
    private val doMgdl: Boolean

    init {
        valueMgdl = bundle.getDouble("bg.valueMgdl", -1000.0)
        noBgData = valueMgdl == -1000.0
        deltaMgdl = bundle.getDouble("bg.deltaValueMgdl", 0.0)
        timeStamp = bundle.getLong("bg.timeStamp", -1L)
        isStale = bundle.getBoolean("bg.isStale", false)
        doMgdl = bundle.getBoolean("doMgdl", true)
        deltaName = bundle.getString("bg.deltaName")
        isBgHigh = bundle.getBoolean("bg.isHigh", false)
        isBgLow = bundle.getBoolean("bg.isLow", false)
    }

    fun isNoBgData(): Boolean = noBgData

    fun isBgHigh(): Boolean = isBgHigh

    fun isBgLow(): Boolean = isBgLow

    fun isDoMgdl(): Boolean = doMgdl

    fun getTimeStamp(): Long = timeStamp

    fun isStale(): Boolean = isStale

    fun getValueMgdl(): Double = valueMgdl

    fun getDeltaMgdl(): Double = deltaMgdl

    fun unitizedDelta(): String = BgUnits.unitizedDeltaStringRaw(false, true, deltaMgdl, doMgdl)

    fun unitizedBgValue(): String = BgUnits.unitizedString(valueMgdl, doMgdl).replace(',', '.')

    fun getDeltaName(): String? = deltaName
}
