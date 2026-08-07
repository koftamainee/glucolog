package com.koftamainee.glucolog.data.xdrip

import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

data class XdripReading(
    val date: LocalDate,
    val h: Float,
    val g: Float,
)

class XdripWebClient {

    suspend fun fetchSgv(count: Int = 1000): List<XdripReading> = withContext(Dispatchers.IO) {
        val url = URL("http://127.0.0.1:$PORT/sgv.json?count=$count")
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw XdripHttpException(connection.responseCode)
            }
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            val zone = ZoneId.systemDefault()
            val readings = buildList {
                val array = JSONArray(text)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val sgv = obj.optInt("sgv", Int.MIN_VALUE)
                    val dateMs = obj.optLong("date", -1L)
                    if (sgv == Int.MIN_VALUE || dateMs < 0L) continue
                    val local = Instant.ofEpochMilli(dateMs).atZone(zone)
                    add(
                        XdripReading(
                            date = local.toLocalDate(),
                            h = local.hour + local.minute / 60f,
                            g = (sgv / MGDL_PER_MMOL).toFloat(),
                        )
                    )
                }
            }
            readings.sortedWith(compareBy({ it.date }, { it.h }))
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val PORT = 17580
        const val MGDL_PER_MMOL = 18.016
        private const val CONNECT_TIMEOUT_MS = 3000
        private const val READ_TIMEOUT_MS = 5000
    }
}

class XdripHttpException(val code: Int) : Exception("HTTP $code")
