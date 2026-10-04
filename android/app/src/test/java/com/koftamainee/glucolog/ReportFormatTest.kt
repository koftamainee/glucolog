package com.koftamainee.glucolog

import com.koftamainee.glucolog.domain.fmtG
import com.koftamainee.glucolog.domain.fmtG2
import com.koftamainee.glucolog.domain.fmtPct
import org.junit.Assert.assertEquals
import org.junit.Test

class ReportFormatTest {

    @Test
    fun `insulin to carb ratio keeps two decimals`() {
        assertEquals("0,05", fmtG2(10.9f / 228.1f))
        assertEquals("0,10", fmtG2(0.1f))
        assertEquals("1,20", fmtG2(1.2f))
        assertEquals("1,24", fmtG2(1.235f))
    }

    @Test
    fun `single decimal formatter collapses small ratios to zero`() {
        assertEquals("0,0", fmtG(10.9f / 228.1f))
        assertEquals("5,5", fmtG(5.45f))
        assertEquals("12,0", fmtG(11.96f))
    }

    @Test
    fun `missing values render as dash`() {
        assertEquals("—", fmtG(null))
        assertEquals("—", fmtG2(null))
    }

    @Test
    fun `percent rounds to whole number`() {
        assertEquals("47%", fmtPct(47.0f))
        assertEquals("47%", fmtPct(46.6f))
        assertEquals("0%", fmtPct(0.4f))
    }
}
