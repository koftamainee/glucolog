package com.koftamainee.glucolog.data.importexport

import android.content.Context
import android.net.Uri

object FileOps {

    fun writeText(context: Context, uri: Uri, text: String) {
        write(context, uri) { out ->
            out.write(text.toByteArray(Charsets.UTF_8))
        }
    }

    fun write(context: Context, uri: Uri, block: (java.io.OutputStream) -> Unit) {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            block(out)
        } ?: throw IllegalArgumentException("Не удалось открыть файл для записи")
    }

    fun readText(context: Context, uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader(Charsets.UTF_8).readText()
        } ?: throw IllegalArgumentException("Не удалось прочитать файл")
    }
}
