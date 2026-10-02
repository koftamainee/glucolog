package com.koftamainee.glucolog

import com.koftamainee.glucolog.domain.GlucosePoint
import com.koftamainee.glucolog.domain.InsulinPoint
import com.koftamainee.glucolog.domain.MealEntry
import com.koftamainee.glucolog.domain.PortableDay
import com.koftamainee.glucolog.domain.RangeBandKind
import com.koftamainee.glucolog.domain.ReportReading
import com.koftamainee.glucolog.domain.ReportStats
import com.koftamainee.glucolog.domain.bandPercent
import com.koftamainee.glucolog.domain.buildReport
import com.koftamainee.glucolog.domain.gmiFromMeanMmol
import com.koftamainee.glucolog.domain.hba1cFromMeanMmol
import com.koftamainee.glucolog.domain.hba1cPercentToMmolMol
import com.koftamainee.glucolog.domain.hyperEpisodes
import com.koftamainee.glucolog.domain.hypoEpisodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReportStatsTest {

    private fun day(
        date: String,
        glucose: List<Pair<Float, Float>>,
        source: String = "xdrip",
        insulin: List<InsulinPoint> = emptyList(),
        meals: List<MealEntry> = emptyList(),
    ) = PortableDay(
        date = date,
        glucose = glucose.map { GlucosePoint(it.first, it.second, source) },
        insulin = insulin,
        meals = meals,
    )

    @Test
    fun `hba1c from mean glucose matches ADAG`() {
        // ADAG: eAG(mg/dL) = 28.7 * A1c - 46.7 -> A1c = (eAG + 46.7) / 28.7
        // 154 mg/dL = 8.56 mmol/L is the canonical "A1c ~7%" point.
        assertEquals(7.0f, hba1cFromMeanMmol(8.56f), 0.05f)
        assertEquals(6.0f, hba1cFromMeanMmol(7.0f), 0.05f)
        assertEquals(8.0f, hba1cFromMeanMmol(10.1f), 0.05f)
    }

    @Test
    fun `gmi matches Bergenstal equation`() {
        // GMI(%) = 3.31 + 0.02392 * meanGlucose(mg/dL)
        assertEquals(6.99f, gmiFromMeanMmol(154f / ReportStats.MMOL_TO_MGDL), 0.05f)
        assertEquals(3.31f, gmiFromMeanMmol(0f), 0.001f)
    }

    @Test
    fun `hba1c percent converts to mmol per mol`() {
        // 7.0% -> (7.0 - 2.15) * 10.929 = 53.0 mmol/mol
        assertEquals(53f, hba1cPercentToMmolMol(7f), 0.2f)
    }

    @Test
    fun `band percentages sum to one hundred`() {
        val readings = listOf(
            ReportReading("2026-10-01", 8f, 2.5f, "xdrip"),
            ReportReading("2026-10-01", 9f, 3.5f, "xdrip"),
            ReportReading("2026-10-01", 10f, 6f, "xdrip"),
            ReportReading("2026-10-01", 11f, 12f, "xdrip"),
            ReportReading("2026-10-01", 12f, 18f, "xdrip"),
        )
        val bands = bandPercent(readings, 4f, 8f)
        assertEquals(5, bands.size)
        assertEquals(100f, bands.sumOf { it.percent.toDouble() }.toFloat(), 0.01f)

        assertEquals(RangeBandKind.VERY_LOW, bands[0].kind)
        assertEquals(1, bands[0].readings)
        assertEquals(RangeBandKind.LOW, bands[1].kind)
        assertEquals(RangeBandKind.TARGET, bands[2].kind)
        assertEquals(RangeBandKind.HIGH, bands[3].kind)
        assertEquals(RangeBandKind.VERY_HIGH, bands[4].kind)
    }

    @Test
    fun `band percentage converts to minutes per day`() {
        // 25% of a 24h day = 6h = 360 min
        val readings = List(4) {
            ReportReading("2026-10-01", 8f, 6f, "xdrip")
        } + List(12) {
            ReportReading("2026-10-01", 9f, 3.5f, "xdrip")
        }
        val target = bandPercent(readings, 4f, 8f)[2]
        assertEquals(360, target.perDayMinutes)
    }

    @Test
    fun `hypo episodes merge inside tolerance and split outside`() {
        val readings = listOf(
            ReportReading("2026-10-01", 2.0f, 3.5f, "xdrip"),
            ReportReading("2026-10-01", 2.5f, 3.0f, "xdrip"),
            ReportReading("2026-10-01", 6.0f, 3.2f, "xdrip"),
        )
        // gap 3.5h > 1.25h tolerance -> two episodes
        val episodes = hypoEpisodes(readings, lo = 4f)
        assertEquals(2, episodes.size)
        assertEquals(3.0f, episodes[0].minG, 0.001f)
    }

    @Test
    fun `hypo episode ends when glucose returns to range`() {
        val readings = listOf(
            ReportReading("2026-10-01", 2.0f, 3.5f, "xdrip"),
            ReportReading("2026-10-01", 2.25f, 6.0f, "xdrip"),
            ReportReading("2026-10-01", 2.5f, 3.6f, "xdrip"),
        )
        val episodes = hypoEpisodes(readings, lo = 4f)
        assertEquals(2, episodes.size)
        assertEquals(2.0f, episodes[0].startH, 0.001f)
        assertEquals(2.25f, episodes[0].endH, 0.001f)
        assertEquals(2.5f, episodes[1].startH, 0.001f)
    }

    @Test
    fun `hypo across midnight stays one episode and is flagged nocturnal`() {
        val readings = listOf(
            ReportReading("2026-10-01", 23.5f, 3.4f, "xdrip"),
            ReportReading("2026-10-02", 0.25f, 2.9f, "xdrip"),
        )
        val episodes = hypoEpisodes(readings, lo = 4f)
        assertEquals(1, episodes.size)
        assertTrue(episodes[0].night)
        assertTrue(episodes[0].level2)
        assertEquals(2.9f, episodes[0].minG, 0.001f)
    }

    @Test
    fun `daytime hypo is not flagged nocturnal`() {
        val readings = listOf(
            ReportReading("2026-10-01", 14.0f, 3.4f, "xdrip"),
            ReportReading("2026-10-01", 14.5f, 4.5f, "xdrip"),
        )
        val episodes = hypoEpisodes(readings, lo = 4f)
        assertEquals(1, episodes.size)
        assertFalse(episodes[0].night)
    }

    @Test
    fun `hyper episodes split outside tolerance and flag nocturnal`() {
        val readings = listOf(
            ReportReading("2026-10-01", 10f, 9.5f, "xdrip"),
            ReportReading("2026-10-01", 14f, 7.5f, "xdrip"),
            ReportReading("2026-10-01", 23.5f, 9.9f, "xdrip"),
        )
        val episodes = hyperEpisodes(readings, hi = 8f)
        assertEquals(2, episodes.size)
        assertEquals(9.5f, episodes[0].minG, 0.001f)
        assertTrue(episodes[1].night)
        assertEquals(9.9f, episodes[1].minG, 0.001f)
    }

    @Test
    fun `report is invalid below seventy percent coverage`() {
        val days = (1..5).map { i ->
            day("2026-10-%02d".format(i), listOf(8f to 6f))
        }
        val report = buildReport(days, LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-10"))
        assertTrue(report != null)
        assertFalse(report!!.clinicallyValid)
        assertEquals(50f, report.coveragePercent, 0.01f)
        assertEquals(5, report.daysWithData)
        assertEquals(10, report.periodDays)
    }

    @Test
    fun `report is valid at fourteen full days`() {
        val days = (1..14).map { i ->
            day("2026-09-%02d".format(i), listOf(3f to 5.5f, 8f to 6f, 20f to 9f))
        }
        val report = buildReport(days, LocalDate.parse("2026-09-01"), LocalDate.parse("2026-09-14"))!!
        assertTrue(report.clinicallyValid)
        assertEquals(100f, report.coveragePercent, 0.01f)
        assertEquals(42, report.readings)
        assertEquals(3.0f, report.readingsPerDay, 0.001f)
    }

    @Test
    fun `empty input yields null`() {
        assertNull(buildReport(emptyList(), LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-14")))
    }

    @Test
    fun `insulin carbs and sources are aggregated per day`() {
        val days = (1..14).map { i ->
            day(
                date = "2026-09-%02d".format(i),
                glucose = listOf(8f to 6f, 8f to 5f),
                source = if (i % 2 == 0) "xdrip" else "manual",
                insulin = listOf(
                    InsulinPoint(22f, b = 6f),
                    InsulinPoint(8f, b = 4f, ba = 20f),
                ),
                meals = listOf(
                    MealEntry("m1", time = "08:00", carbs = 50),
                    MealEntry("m2", time = "13:00", carbs = 70),
                ),
            )
        }
        val report = buildReport(days, LocalDate.parse("2026-09-01"), LocalDate.parse("2026-09-14"))!!
        assertEquals(14, report.cgmReadings)
        assertEquals(14, report.manualReadings)
        assertEquals(30f, report.tddPerDay, 0.001f)
        assertEquals(20f, report.basalPerDay, 0.001f)
        assertEquals(10f, report.bolusPerDay, 0.001f)
        assertEquals(120f, report.carbsPerDay, 0.001f)
        assertEquals(10f / 120f, report.bolusCarbRatio!!, 0.0001f)
    }

    @Test
    fun `report is valid at seven fully covered days`() {
        val days = (1..7).map { i ->
            day("2026-10-%02d".format(i), listOf(3f to 5.5f, 8f to 6f, 20f to 9f))
        }
        val report = buildReport(days, LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-07"))!!
        assertTrue(report.clinicallyValid)
        assertEquals(7, report.daysWithData)
        assertEquals(7, report.requiredDays)
        assertEquals(100f, report.coveragePercent, 0.01f)
    }

    @Test
    fun `postprandial average aggregates all meals into one stat`() {
        val glucose = listOf(
            8f to 6f,   // meal time itself — not in any postprandial window
            10f to 12f, // 2h after 08:00 meal
            11f to 9f,  // 3h after 08:00 — outside the 1–3h window
            15f to 13f, // 2h after 13:00 meal
        )
        val days = (1..14).map { i ->
            day(
                date = "2026-09-%02d".format(i),
                glucose = glucose,
                meals = listOf(
                    MealEntry("a", time = "08:00", carbs = 50),
                    MealEntry("b", time = "13:00", carbs = 70),
                ),
            )
        }
        val report = buildReport(days, LocalDate.parse("2026-09-01"), LocalDate.parse("2026-09-14"))!!
        val pp = report.postprandial!!
        assertEquals("После еды", pp.title)
        assertEquals(28, pp.readings)
        assertEquals(12.5f, pp.meanG!!, 0.001f)
        assertEquals(13f, pp.maxG!!, 0.001f)
    }

    @Test
    fun `fasting stat uses only the early morning window`() {
        val days = (1..14).map { i ->
            day("2026-09-%02d".format(i), listOf(3.5f to 5f, 6f to 14f, 20f to 9f))
        }
        val report = buildReport(days, LocalDate.parse("2026-09-01"), LocalDate.parse("2026-09-14"))!!
        assertEquals(28, report.fasting!!.readings)
        assertEquals(9.5f, report.fasting!!.meanG!!, 0.001f)
    }

    @Test
    fun `target range from settings is applied to bands`() {
        val readings = List(10) { ReportReading("2026-10-01", 8f, 6f, "xdrip") }
        val tight = bandPercent(readings, 5f, 7f)[2]
        val wide = bandPercent(readings, 4f, 8f)[2]
        assertEquals(100f, tight.percent, 0.01f)
        assertEquals(100f, wide.percent, 0.01f)

        val report = buildReport(
            days = (1..14).map { day("2026-09-%02d".format(it), listOf(8f to 6f)) },
            from = LocalDate.parse("2026-09-01"),
            to = LocalDate.parse("2026-09-14"),
            targetLo = 5f,
            targetHi = 7f,
        )!!
        assertEquals(5f, report.targetLo, 0.001f)
        assertEquals(7f, report.targetHi, 0.001f)
        assertEquals(100f, report.inRangePercent, 0.01f)
    }
}
