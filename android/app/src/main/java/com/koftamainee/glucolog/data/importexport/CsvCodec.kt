package com.koftamainee.glucolog.data.importexport

import com.koftamainee.glucolog.domain.Constants
import com.koftamainee.glucolog.domain.MealField
import com.koftamainee.glucolog.domain.PortableDay
import com.koftamainee.glucolog.domain.floatToTime
import com.koftamainee.glucolog.domain.timeToFloat
import java.util.LinkedHashMap
import java.util.Locale

object CsvCodec {

    fun export(days: List<PortableDay>): String {
        val rows = mutableListOf<List<String>>()
        rows.add(listOf("Дата", "Время", "Тип", "Значение", "Детали", "Источник"))
        days.sortedBy { it.date }.forEach { day ->
            day.glucose.sortedBy { it.h }.forEach { p ->
                rows.add(listOf(day.date, floatToTime(p.h), "Глюкоза", fmt1(p.g), "ммоль/л", p.source))
            }
            day.insulin.sortedBy { it.h }.forEach { p ->
                p.b?.takeIf { it > 0f }?.let {
                    rows.add(listOf(day.date, floatToTime(p.h), "Болюс", fmtIns(it), "ед."))
                }
                p.ba?.takeIf { it > 0f }?.let {
                    rows.add(listOf(day.date, floatToTime(p.h), "Базальный", fmtIns(it), "ед."))
                }
            }
            day.water?.let { rows.add(listOf(day.date, "", "Вода", "$it", "стаканов")) }
            day.sport?.let { rows.add(listOf(day.date, "", "Спорт", if (it) "Да" else "Нет", "")) }
            day.steps?.takeIf { it > 0 }?.let { rows.add(listOf(day.date, "", "Шаги", "$it", "")) }
            day.sleepStart?.let { rows.add(listOf(day.date, "", "Сон (лёг)", it, "")) }
            day.sleepEnd?.let { rows.add(listOf(day.date, "", "Сон (встал)", it, "")) }
            day.stress?.let { rows.add(listOf(day.date, "", "Стресс", it, "")) }
            if (day.stool.isNotEmpty()) {
                rows.add(listOf(day.date, "", "Стул", day.stool.joinToString(", "), ""))
            }
            day.notes?.let { rows.add(listOf(day.date, "", "Заметки", it.replace("\n", " "), "")) }
            day.conclusions?.let { rows.add(listOf(day.date, "", "Выводы", it.replace("\n", " "), "")) }
            Constants.MEALS.forEach { meal ->
                val m = day.meals.firstOrNull { it.key == meal.key } ?: return@forEach
                m.time?.let { rows.add(listOf(day.date, it, "${meal.key} время", "", "")) }
                m.hunger?.takeIf { it > 0 }?.let { rows.add(listOf(day.date, "", "${meal.key} голод", "$it", "")) }
                m.food?.let { rows.add(listOf(day.date, "", "${meal.key} еда", it.replace("\n", " "), "")) }
                m.phys?.let { rows.add(listOf(day.date, "", "${meal.key} физ", it.replace("\n", " "), "")) }
                m.emo?.let { rows.add(listOf(day.date, "", "${meal.key} эмоции", it.replace("\n", " "), "")) }
            }
        }
        return rows.joinToString("\n") { row ->
            row.joinToString(",") { escapeCell(it) }
        }
    }

    fun import(text: String): List<PortableDay> {
        val lines = text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) throw IllegalArgumentException("Файл пуст")
        val start = if (lines[0].contains("Дата")) 1 else 0
        val days = LinkedHashMap<String, DayBuilder>()
        for (i in start until lines.size) {
            val row = parseCsvLine(lines[i])
            if (row.size < 4) continue
            val dateKey = row[0].trim()
            val time = row[1].trim()
            val type = row[2].trim()
            val value = row[3].trim()
            if (dateKey.isEmpty() || type.isEmpty()) continue
            val day = days.getOrPut(dateKey) { DayBuilder() }
            val num = value.replace(',', '.')
            when {
                type == "Глюкоза" -> Unit
                type == "Болюс" -> timeToFloat(time)?.let {
                    day.addInsulin(it, num.toFloatOrNull()?.takeIf { v -> v > 0f }, null)
                }
                type == "Базальный" -> timeToFloat(time)?.let {
                    day.addInsulin(it, null, num.toFloatOrNull()?.takeIf { v -> v > 0f })
                }
                type == "Вода" -> day.water = num.toIntOrNull()
                type == "Спорт" -> day.sport = value == "Да"
                type == "Шаги" -> day.steps = num.toIntOrNull()
                type == "Сон (лёг)" -> day.sleepStart = value
                type == "Сон (встал)" -> day.sleepEnd = value
                type == "Стресс" -> day.stress = value
                type == "Стул" -> value.split(", ").filter { it.isNotEmpty() }.forEach { day.addStool(it) }
                type == "Заметки" -> day.notes = value
                type == "Выводы" -> day.conclusions = value
                type.endsWith(" время") -> day.setMealField(type.removeSuffix(" время"), MealField.TIME, time)
                type.endsWith(" голод") -> day.setMealField(type.removeSuffix(" голод"), MealField.HUNGER, value)
                type.endsWith(" еда") -> day.setMealField(type.removeSuffix(" еда"), MealField.FOOD, value)
                type.endsWith(" физ") -> day.setMealField(type.removeSuffix(" физ"), MealField.PHYS, value)
                type.endsWith(" эмоции") -> day.setMealField(type.removeSuffix(" эмоции"), MealField.EMO, value)
            }
        }
        if (days.isEmpty()) throw IllegalArgumentException("Файл не похож на экспорт Glucolog")
        return days.map { (date, builder) -> builder.toPortable(date) }
    }

    fun isNewFormat(text: String): Boolean {
        val first = text.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() } ?: return false
        return first.contains("Источник")
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    current.append(ch)
                }
            } else {
                when (ch) {
                    '"' -> inQuotes = true
                    ',' -> {
                        result.add(current.toString())
                        current = StringBuilder()
                    }
                    else -> current.append(ch)
                }
            }
            i++
        }
        result.add(current.toString())
        return result
    }

    private fun escapeCell(value: String): String {
        if (!value.contains(',') && !value.contains('"') && !value.contains('\n')) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }

    private fun fmt1(v: Float): String = String.format(Locale.US, "%.1f", v)

    private fun fmtIns(v: Float): String {
        val whole = v.toInt().toFloat()
        return if (v == whole) v.toInt().toString()
        else String.format(Locale.US, "%.1f", v)
    }
}
