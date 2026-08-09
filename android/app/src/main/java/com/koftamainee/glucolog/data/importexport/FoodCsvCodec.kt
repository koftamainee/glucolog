package com.koftamainee.glucolog.data.importexport

import com.koftamainee.glucolog.data.db.ProductEntity
import java.util.Locale

data class FoodImport(
    val products: List<ProductEntity>,
)

object FoodCsvCodec {

    private const val HEADER = "Тип;Название;Ккал;Белки;Жиры;Углеводы;МассаПорции;Примечание;Источник"

    fun export(products: List<ProductEntity>): String {
        val rows = mutableListOf<List<String>>()
        rows.add(HEADER.split(';'))
        products.sortedBy { it.name }.forEach { p ->
            rows.add(
                listOf(
                    "ПРОДУКТ", p.name, fmt(p.kcal), fmt(p.proteins), fmt(p.fats), fmt(p.carbs),
                    p.portionMass.toString(), p.note ?: "", p.source,
                )
            )
        }
        return rows.joinToString("\n") { row ->
            row.joinToString(";") { escapeCell(it) }
        }
    }

    fun parse(text: String): FoodImport {
        val lines = text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) throw IllegalArgumentException("Файл пуст")
        val start = if (lines[0].contains("Тип")) 1 else 0
        val products = mutableListOf<ProductEntity>()
        for (i in start until lines.size) {
            val row = parseCsvLine(lines[i])
            if (row.size < 4) continue
            if (row[0].trim() != "ПРОДУКТ") continue
            val name = row.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
                ?: continue
            val num = { index: Int -> (row.getOrNull(index) ?: "").replace(',', '.').toFloatOrNull() ?: 0f }
            products.add(
                ProductEntity(
                    name = name,
                    kcal = num(2),
                    proteins = num(3),
                    fats = num(4),
                    carbs = num(5),
                    portionMass = (row.getOrNull(6) ?: "").toIntOrNull() ?: 0,
                    note = row.getOrNull(7)?.trim()?.takeIf { it.isNotEmpty() },
                    source = row.getOrNull(8)?.trim()?.takeIf { it.isNotEmpty() } ?: "manual",
                )
            )
        }
        if (products.isEmpty()) {
            throw IllegalArgumentException("Файл не похож на экспорт еды Glucolog")
        }
        return FoodImport(products = products)
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
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
                    ';' -> {
                        result.add(current.toString())
                        current.clear()
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
        if (!value.contains(';') && !value.contains('"') && !value.contains('\n')) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }

    private fun fmt(v: Float): String = String.format(Locale.US, "%.2f", v)
}
