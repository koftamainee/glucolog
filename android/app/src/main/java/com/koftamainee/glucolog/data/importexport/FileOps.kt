package com.koftamainee.glucolog.data.importexport

import android.content.Context
import android.net.Uri

object FileOps {

    fun writeText(context: Context, uri: Uri, text: String) {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(text.toByteArray(Charsets.UTF_8))
        } ?: throw IllegalArgumentException("Не удалось открыть файл для записи")
    }

    fun readText(context: Context, uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader(Charsets.UTF_8).readText()
        } ?: throw IllegalArgumentException("Не удалось прочитать файл")
    }
}
