package com.koftamainee.glucolog.data.report

import android.graphics.pdf.PdfDocument
import com.koftamainee.glucolog.domain.DayReport
import com.koftamainee.glucolog.domain.DateKeys
import com.koftamainee.glucolog.domain.RangeBand
import com.koftamainee.glucolog.domain.RangeBandKind
import com.koftamainee.glucolog.domain.ReportModel
import com.koftamainee.glucolog.domain.ReportStats
import com.koftamainee.glucolog.domain.floatToTime
import com.koftamainee.glucolog.domain.fmtDateDayMonth
import com.koftamainee.glucolog.domain.fmtDateShort
import com.koftamainee.glucolog.domain.fmtDateWeekday
import com.koftamainee.glucolog.domain.fmtDurationMinutes
import com.koftamainee.glucolog.domain.fmtG
import com.koftamainee.glucolog.domain.fmtInt
import com.koftamainee.glucolog.domain.fmtPct
import com.koftamainee.glucolog.domain.parseDateKey
import com.koftamainee.glucolog.domain.pluralDays
import java.io.Closeable
import java.io.OutputStream
import java.time.LocalDate

object PdfReport {

    fun writeSummary(model: ReportModel, out: OutputStream) {
        Doc(model).use { doc ->
            doc.summary()
            doc.writeTo(out)
        }
    }

    fun summaryBytes(model: ReportModel): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        writeSummary(model, out)
        return out.toByteArray()
    }

    fun suggestedFileName(model: ReportModel, prefix: String): String =
        "$prefix-${model.from}_${model.to}.pdf"
}

private const val PAGE_W = 595f
private const val PAGE_H = 842f
private const val MARGIN_X = 42f
private const val MARGIN_TOP = 40f
private const val MARGIN_BOTTOM = 46f

private const val SIZE_TITLE = 15f
private const val SIZE_SUB = 8f
private const val SIZE_SECTION = 10f
private const val SIZE_LABEL = 6.5f
private const val SIZE_VALUE = 11f
private const val SIZE_CELL = 7.2f
private const val SIZE_NOTE = 7.2f

private const val ROW_H = 12.5f
private const val TH_H = 14f

private const val AVG_LABEL = "Среднее"
private const val MAX_EPISODE_ROWS = 40

private class Doc(private val model: ReportModel) : Closeable {

    private val document = PdfDocument()
    private var page: PdfDocument.Page? = null
    private var pdf: PdfCanvas? = null
    private var cursor = 0f
    private var pageNumber = 0

    private val left = MARGIN_X
    private val right = PAGE_W - MARGIN_X
    private val width = right - left
    private val bottom = PAGE_H - MARGIN_BOTTOM

    private val g: PdfCanvas get() = pdf ?: beginPage()

    override fun close() {
        document.close()
    }

    fun writeTo(out: OutputStream) {
        finishPage()
        document.writeTo(out)
    }

    private fun beginPage(): PdfCanvas {
        finishPage()
        pageNumber += 1
        val info = PdfDocument.PageInfo
            .Builder(PAGE_W.toInt(), PAGE_H.toInt(), pageNumber)
            .create()
        val started = document.startPage(info)
        page = started
        cursor = MARGIN_TOP
        pdf = PdfCanvas(started.canvas)
        return pdf!!
    }

    private fun finishPage() {
        val current = page ?: return
        pdf?.let { c ->
            val ruleY = PAGE_H - MARGIN_BOTTOM + 14f
            c.line(left, ruleY, right, ruleY, PdfColors.GRID, 0.5f)
            c.text("Glucolog", left, ruleY + 12f, 6.5f, PdfColors.TEXT_TER)
            c.text("Стр. $pageNumber", right, ruleY + 12f, 6.5f, PdfColors.TEXT_TER, align = PdfAlign.RIGHT)
        }
        document.finishPage(current)
        page = null
        pdf = null
    }

    private fun ensure(height: Float) {
        if (page == null) beginPage()
        if (cursor + height > bottom) beginPage()
    }

    private fun space(height: Float) {
        ensure(height)
        cursor += height
    }

    // ---------- shared blocks ----------

    private fun header(title: String, subtitle: String) {
        ensure(54f)
        val c = g
        c.text(title, left, cursor + 13f, SIZE_TITLE, PdfColors.TEXT, bold = true, maxWidth = width * 0.55f)
        c.text(subtitle, left, cursor + 27f, SIZE_SUB, PdfColors.TEXT_SEC, maxWidth = width * 0.55f)
        c.text(
            "${fmtDateShort(model.from)} — ${fmtDateShort(model.to)}",
            right, cursor + 13f, SIZE_SUB, PdfColors.TEXT_SEC, align = PdfAlign.RIGHT,
        )
        c.text(
            "${model.daysWithData} ${pluralDays(model.daysWithData)} с данными",
            right, cursor + 27f, SIZE_SUB, PdfColors.TEXT_SEC, align = PdfAlign.RIGHT,
        )
        cursor += 34f
        c.line(left, cursor, right, cursor, PdfColors.GREEN, 1f)
        cursor += 12f
    }

    private fun section(text: String) {
        ensure(30f)
        cursor += 4f
        g.text(text, left, cursor + 8f, SIZE_SECTION, PdfColors.GREEN, bold = true, maxWidth = width)
        cursor += 13f
    }

    private fun note(text: String) {
        val lines = g.wrap(text, width, SIZE_NOTE)
        val h = lines.size * SIZE_NOTE * 1.35f
        ensure(h + 6f)
        val c = g
        lines.forEachIndexed { i, line ->
            c.text(line, left, cursor + SIZE_NOTE + i * SIZE_NOTE * 1.35f, SIZE_NOTE, PdfColors.TEXT_SEC)
        }
        cursor += h + 6f
    }

    private fun callout(text: String, bg: Int, fg: Int) {
        val lines = g.wrap(text, width - 16f, SIZE_NOTE, bold = true)
        val h = lines.size * SIZE_NOTE * 1.35f + 12f
        ensure(h)
        val c = g
        c.fillRect(left, cursor, right, cursor + h, bg)
        c.fillRect(left, cursor, left + 2.5f, cursor + h, fg)
        lines.forEachIndexed { i, line ->
            c.text(line, left + 8f, cursor + 11f + i * SIZE_NOTE * 1.35f, SIZE_NOTE, fg, bold = true)
        }
        cursor += h + 8f
    }

    private fun metrics(items: List<Pair<String, String>>, columns: Int = 3) {
        if (items.isEmpty()) return
        val rows = (items.size + columns - 1) / columns
        val cellW = width / columns
        val cellH = 30f
        ensure(rows * cellH)
        val c = g
        items.forEachIndexed { i, (label, value) ->
            val x = left + (i % columns) * cellW
            val y = cursor + (i / columns) * cellH
            c.text(label, x, y + 8f, SIZE_LABEL, PdfColors.TEXT_TER, maxWidth = cellW - 10f)
            c.text(value, x, y + 22f, SIZE_VALUE, PdfColors.TEXT, bold = true, maxWidth = cellW - 10f)
        }
        cursor += rows * cellH + 4f
    }

    private fun table(
        headers: List<String>,
        weights: List<Float>,
        rows: List<List<String>>,
        rightAligned: Set<Int> = emptySet(),
    ) {
        if (rows.isEmpty()) {
            note("Нет данных за выбранный период.")
            return
        }
        val widths = splitWidths(weights)

        fun drawHeader() {
            val c = g
            c.fillRect(left, cursor, right, cursor + TH_H, PdfColors.SURFACE)
            var x = left
            headers.forEachIndexed { i, htxt ->
                val w = widths[i]
                val rightSide = i in rightAligned
                c.text(
                    htxt,
                    if (rightSide) x + w - 4f else x + 4f,
                    cursor + TH_H - 4f,
                    SIZE_CELL,
                    PdfColors.TEXT_SEC,
                    bold = true,
                    align = if (rightSide) PdfAlign.RIGHT else PdfAlign.LEFT,
                    maxWidth = w - 8f,
                )
                x += w
            }
            c.line(left, cursor + TH_H, right, cursor + TH_H, PdfColors.SURFACE_DARK, 0.8f)
            cursor += TH_H
        }

        fun drawRow(cells: List<String>, shaded: Boolean) {
            val c = g
            val isAvg = cells.firstOrNull() == AVG_LABEL
            if (shaded) c.fillRect(left, cursor, right, cursor + ROW_H, PdfColors.SURFACE)
            var x = left
            cells.forEachIndexed { i, cell ->
                val w = widths.getOrElse(i) { 0f }
                val rightSide = i in rightAligned
                c.text(
                    cell,
                    if (rightSide) x + w - 4f else x + 4f,
                    cursor + ROW_H - 3.5f,
                    SIZE_CELL,
                    if (i == 0) PdfColors.TEXT else PdfColors.TEXT_SEC,
                    bold = isAvg,
                    align = if (rightSide) PdfAlign.RIGHT else PdfAlign.LEFT,
                    maxWidth = w - 8f,
                )
                x += w
            }
            c.line(left, cursor + ROW_H, right, cursor + ROW_H, PdfColors.GRID, 0.4f)
            cursor += ROW_H
        }

        ensure(TH_H + ROW_H)
        drawHeader()
        rows.forEachIndexed { i, cells ->
            if (cursor + ROW_H > bottom) {
                beginPage()
                drawHeader()
            }
            drawRow(cells, shaded = i % 2 == 1)
        }
        cursor += 6f
    }

    private fun splitWidths(weights: List<Float>): List<Float> {
        val total = weights.sumOf { it.toDouble() }.takeIf { it > 0.0 } ?: 1.0
        return weights.map { (it / total * width).toFloat() }
    }

    private fun stackedBar(bands: List<RangeBand>, height: Float) {
        ensure(height + 6f)
        val c = g
        val top = cursor + 2f
        var x = left
        bands.forEach { band ->
            val w = width * band.percent / 100f
            if (w <= 0f) return@forEach
            c.fillRect(x, top, x + w, top + height, PdfColors.band(band.kind))
            x += w
        }
        c.strokeRect(left, top, right, top + height, PdfColors.SURFACE_DARK, 0.5f)
        cursor = top + height + 2f
    }

    // ---------- report: summary ----------

    fun summary() {
        header(
            "Сводный отчёт",
            "Целевой диапазон ${fmtG(model.targetLo)}–${fmtG(model.targetHi)} ммоль/л" +
                " · сформирован ${fmtDateShort(DateKeys.key(LocalDate.now()))}",
        )

        if (!model.clinicallyValid) {
            callout(
                "Данных недостаточно для клинической интерпретации: " +
                    "${model.daysWithData} ${pluralDays(model.daysWithData)} с данными " +
                    "(${fmtPct(model.coveragePercent)} от периода). Для анализа нужно не менее " +
                    "${model.requiredDays} ${pluralDays(model.requiredDays)} с данными " +
                    "при заполнении не менее ${(ReportStats.MIN_COVERAGE * 100).toInt()}%.",
                PdfColors.WARN_BG,
                PdfColors.WARN_TEXT,
            )
        }

        glycemicControl()
        dataCompleteness()
        timeInRange()
        hypoHyper()
        postprandial()
        therapy()
        dayTable()
        disclaimer()
    }

    private fun glycemicControl() {
        section("Гликемический контроль")
        metrics(
            listOf(
                "Средняя глюкоза" to "${fmtG(model.meanG)} ммоль/л",
                "Расчётный HbA1c" to "${fmtG(model.hba1cPercent)}%",
                "HbA1c, ммоль/моль" to fmtG(model.hba1cMmolMol),
                "GMI" to "${fmtG(model.gmiPercent)}%",
                "SD / CV" to "${fmtG(model.sdG)} / ${fmtG(model.cvPercent)}%",
                "Всего показаний" to "${model.readings}",
            ),
        )
    }

    private fun dataCompleteness() {
        section("Полнота данных")
        metrics(
            listOf(
                "Дней в периоде" to "${model.periodDays}",
                "Дней с данными" to "${model.daysWithData}",
                "Заполнено" to fmtPct(model.coveragePercent),
                "Измерений в сутки" to fmtG(model.readingsPerDay),
                "Измерений CGM" to "${model.cgmReadings}",
                "Измерений глюкометром" to "${model.manualReadings}",
            ),
        )
    }

    private fun timeInRange() {
        section("Время в диапазоне")
        stackedBar(model.bands, 16f)

        val rows = model.bands.map { band ->
            listOf(band.kind.title, rangeLabel(band), fmtPct(band.percent), fmtDurationMinutes(band.perDayMinutes))
        }
        table(
            headers = listOf("Полоса", "Границы, ммоль/л", "% измерений", "В сутки"),
            weights = listOf(2.4f, 1.6f, 1.1f, 1.3f),
            rows = rows,
            rightAligned = setOf(2, 3),
        )
    }

    private fun hypoHyper() {
        section("Гипо- и гипергликемия")
        val mins = model.days.mapNotNull { it.minG }
        val maxs = model.days.mapNotNull { it.maxG }
        metrics(
            listOf(
                "Эпизодов гипогликемии" to "${model.hypoCount}",
                "Гипо уровня 2 (<3,0)" to "${model.hypoLevel2Count}",
                "Ночных гипо" to "${model.nightHypoCount}",
                "Гипергликемий (всего)" to "${model.hyperCount}",
                "Гипергликемий (ночных)" to "${model.nightHyperCount}",
                "Минимум за период" to fmtG(mins.minOrNull()),
                "Максимум за период" to fmtG(maxs.maxOrNull()),
            ),
        )
        val episodes = model.episodes.take(MAX_EPISODE_ROWS)
        if (episodes.isNotEmpty()) {
            table(
                headers = listOf("Дата", "Начало", "Конец", "Минимум", "Тип"),
                weights = listOf(1.6f, 1f, 1f, 1f, 1.5f),
                rows = episodes.map {
                    listOf(
                        "${fmtDateShort(it.date)} ${fmtDateWeekday(it.date)}",
                        it.startTime,
                        it.endTime,
                        fmtG(it.minG),
                        when {
                            it.level2 -> "уровень 2"
                            it.night -> "ночная"
                            else -> "обычная"
                        },
                    )
                },
                rightAligned = setOf(1, 2, 3),
            )
            if (model.episodes.size > MAX_EPISODE_ROWS) {
                note("Показаны первые $MAX_EPISODE_ROWS из ${model.episodes.size} эпизодов.")
            }
        }
    }

    private fun postprandial() {
        section("Гликемия натощак и после еды")
        val rows = mutableListOf<List<String>>()
        model.fasting?.let {
            rows += listOf(
                "Натощак (${fmtHour(ReportStats.FASTING_FROM)}–${fmtHour(ReportStats.FASTING_TO)})",
                fmtInt(it.readings),
                fmtG(it.meanG),
                fmtG(it.maxG),
            )
        }
        model.postprandial?.takeIf { it.readings > 0 }?.let {
            rows += listOf(
                "После еды (${ReportStats.PP_FROM.toInt()}–${ReportStats.PP_TO.toInt()} ч)",
                fmtInt(it.readings),
                fmtG(it.meanG),
                fmtG(it.maxG),
            )
        }
        table(
            headers = listOf("Показатель", "Измерений", "Средняя", "Максимум"),
            weights = listOf(2.6f, 1f, 1f, 1f),
            rows = rows,
            rightAligned = setOf(1, 2, 3),
        )
    }

    private fun therapy() {
        section("Инсулинотерапия и питание")
        metrics(
            listOf(
                "Суточная доза инсулина" to "${fmtG(model.tddPerDay)} ед.",
                "Базальный" to "${fmtG(model.basalPerDay)} ед.",
                "Болюсный" to "${fmtG(model.bolusPerDay)} ед.",
                "Углеводы в сутки" to "${fmtG(model.carbsPerDay)} г",
                "Ед. на 1 г углеводов" to fmtG(model.bolusCarbRatio),
                "Доля базального" to "${fmtG(basalShare())}%",
            ),
        )
    }

    private fun dayTable() {
        section("Показатели по дням")
        val days = model.days
        if (days.isEmpty()) {
            note("Нет данных за выбранный период.")
            return
        }
        val rows = mutableListOf<List<String>>()

        var week = mutableListOf<DayReport>()
        days.forEach { day ->
            rows += dayRow(day)
            week += day
            if ((parseDateKey(day.date)?.dayOfWeek?.value ?: 7) == 7) {
                rows += averageRow(week.toList())
                week = mutableListOf()
            }
        }
        if (week.isNotEmpty()) rows += averageRow(week.toList())

        table(
            headers = listOf("Дата", "N", "Ср.", "Мин", "Макс", "SD", "Гипо", "Базал", "Болюс", "УГ"),
            weights = listOf(1.6f, 0.55f, 0.8f, 0.7f, 0.7f, 0.7f, 0.6f, 0.85f, 0.85f, 0.75f),
            rows = rows,
            rightAligned = setOf(1, 2, 3, 4, 5, 6, 7, 8, 9),
        )
    }

    private fun averageRow(days: List<DayReport>): List<String> {
        if (days.isEmpty()) return List(DAY_COLUMNS) { "" }
        fun avgOf(selector: (DayReport) -> Float?): String {
            val values = days.mapNotNull(selector)
            return if (values.isEmpty()) "—" else fmtG(values.average().toFloat())
        }
        return listOf(
            AVG_LABEL,
            "${days.size}",
            avgOf { it.meanG },
            avgOf { it.minG },
            avgOf { it.maxG },
            avgOf { it.sdG },
            "${days.sumOf { it.hypoEpisodes }}",
            avgOf { it.basal.takeIf { v -> v > 0f } },
            avgOf { it.bolus.takeIf { v -> v > 0f } },
            avgOf { it.carbs.takeIf { v -> v > 0 }?.toFloat() },
        )
    }

    private fun dayRow(day: DayReport): List<String> = listOf(
        "${fmtDateDayMonth(day.date)} ${fmtDateWeekday(day.date)}",
        "${day.readings}",
        fmtG(day.meanG),
        fmtG(day.minG),
        fmtG(day.maxG),
        fmtG(day.sdG),
        if (day.hypoEpisodes > 0) "${day.hypoEpisodes}" else "—",
        if (day.basal > 0f) fmtG(day.basal) else "—",
        if (day.bolus > 0f) fmtG(day.bolus) else "—",
        if (day.carbs > 0) "${day.carbs}" else "—",
    )

    // ---------- helpers ----------

    private fun basalShare(): Float? {
        val total = model.basalTotal + model.bolusTotal
        return if (total > 0f) model.basalTotal / total * 100f else null
    }

    private fun rangeLabel(band: RangeBand): String = when (band.kind) {
        RangeBandKind.VERY_HIGH -> "> ${fmtG(band.lo)}"
        RangeBandKind.VERY_LOW -> "< ${fmtG(band.hi)}"
        RangeBandKind.LOW -> "${fmtG(band.lo)} – ${fmtG(band.hi)}"
        else -> "${fmtG(band.lo)} – ${fmtG(band.hi)}"
    }

    private fun disclaimer() {
        space(4f)
        note(
            "Отчёт сформирован приложением Glucolog по данным измерений. Значения HbA1c и GMI " +
                "расчётные и не заменяют лабораторный анализ. Решение о коррекции терапии " +
                "принимает лечащий врач.",
        )
    }
}

private const val DAY_COLUMNS = 10

private fun fmtHour(hourFloat: Float): String = floatToTime(hourFloat)
