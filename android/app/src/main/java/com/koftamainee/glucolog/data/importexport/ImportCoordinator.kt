package com.koftamainee.glucolog.data.importexport

import com.koftamainee.glucolog.domain.PortableDay

data class ImportedFile(
    val days: List<PortableDay>,
    val isNewFormat: Boolean,
)

object ImportCoordinator {

    fun parse(text: String): ImportedFile {
        val trimmed = text.removePrefix("\uFEFF").trim()
        if (trimmed.isEmpty()) throw IllegalArgumentException("Файл пуст")
        return if (trimmed.startsWith("{")) {
            ImportedFile(JsonCodec.import(trimmed), JsonCodec.isNewFormat(trimmed))
        } else {
            val days = CsvCodec.import(trimmed)
            ImportedFile(days, CsvCodec.isNewFormat(trimmed))
        }
    }
}
