package com.koftamainee.glucolog.data.backup

import com.koftamainee.glucolog.data.db.ProductEntity
import com.koftamainee.glucolog.data.importexport.JsonCodec
import com.koftamainee.glucolog.domain.PortableDay
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

object BackupPayload {

    const val VERSION = 1

    fun build(days: List<PortableDay>?, products: List<ProductEntity>?): String {
        val root = JSONObject()
        root.put("app", "glucolog")
        root.put("version", VERSION)
        root.put("createdAt", Instant.now().toString())
        if (days != null) {
            root.put("days", JSONObject(JsonCodec.export(days)))
        }
        if (products != null) {
            root.put("products", JSONArray().apply {
                products.forEach { put(productToJson(it)) }
            })
        }
        return root.toString()
    }

    private fun productToJson(p: ProductEntity): JSONObject {
        val o = JSONObject()
        o.put("name", p.name)
        o.put("kcal", p.kcal.toDouble())
        o.put("proteins", p.proteins.toDouble())
        o.put("fats", p.fats.toDouble())
        o.put("carbs", p.carbs.toDouble())
        o.put("portionMass", p.portionMass)
        p.note?.let { o.put("note", it) }
        o.put("source", p.source)
        o.put("hidden", p.hidden)
        p.lastUsed?.let { o.put("lastUsed", it) }
        return o
    }

    data class ParsedBackup(
        val days: List<PortableDay>,
        val products: List<ProductEntity>,
    )

    fun parse(json: String): ParsedBackup {
        val root = try {
            JSONObject(json)
        } catch (e: Exception) {
            throw IllegalArgumentException("Неверный JSON бэкапа")
        }
        val days = root.optJSONObject("days")
            ?.let { JsonCodec.import(it.toString()) }
            ?: emptyList()
        val products = mutableListOf<ProductEntity>()
        root.optJSONArray("products")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val name = o.optString("name").takeIf { it.isNotEmpty() } ?: continue
                products.add(
                    ProductEntity(
                        name = name,
                        kcal = o.optDouble("kcal").toFloat(),
                        proteins = o.optDouble("proteins").toFloat(),
                        fats = o.optDouble("fats").toFloat(),
                        carbs = o.optDouble("carbs").toFloat(),
                        portionMass = o.optInt("portionMass"),
                        note = o.optString("note").takeIf { it.isNotEmpty() },
                        source = o.optString("source").takeIf { it.isNotEmpty() } ?: "manual",
                        hidden = o.optBoolean("hidden", false),
                        lastUsed = o.optLong("lastUsed").takeIf { it > 0L },
                    )
                )
            }
        }
        return ParsedBackup(days = days, products = products)
    }
}
