package com.koftamainee.glucolog.data.importexport

import com.koftamainee.glucolog.domain.Constants
import com.koftamainee.glucolog.domain.MealEntry
import com.koftamainee.glucolog.domain.PortableDay
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.LinkedHashMap

object JsonCodec {

    fun export(days: List<PortableDay>): String {
        val root = JSONObject(LinkedHashMap<String, Any>())
        days.sortedBy { it.date }.forEach { day ->
            root.put(day.date, dayToJson(day))
        }
        return root.toString()
    }

    fun import(text: String): List<PortableDay> {
        val root = try {
            JSONObject(text)
        } catch (e: JSONException) {
            throw IllegalArgumentException("Неверный JSON")
        }
        val result = mutableListOf<PortableDay>()
        val keys = root.keys()
        while (keys.hasNext()) {
            val dateKey = keys.next()
            if (!DATE_REGEX.matches(dateKey)) continue
            val dayObj = root.optJSONObject(dateKey) ?: continue
            val builder = DayBuilder()

            dayObj.optJSONArray("glucose")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val p = arr.optJSONObject(i) ?: continue
                    val h = p.optDouble("h").takeIf { !it.isNaN() }?.toFloat() ?: continue
                    val g = p.optDouble("g").takeIf { !it.isNaN() }?.toFloat() ?: continue
                    val source = p.optString("source").takeIf { it.isNotEmpty() } ?: "manual"
                    builder.addGlucose(h, g, source)
                }
            }

            dayObj.optJSONArray("insulin")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val p = arr.optJSONObject(i) ?: continue
                    val h = p.optDouble("h").takeIf { !it.isNaN() }?.toFloat() ?: continue
                    val b = p.optDouble("b").takeIf { !it.isNaN() && it > 0.0 }?.toFloat()
                    val ba = p.optDouble("ba").takeIf { !it.isNaN() && it > 0.0 }?.toFloat()
                    builder.addInsulin(h, b, ba)
                }
            }

            Constants.MEALS.forEach { meal ->
                val m = dayObj.optJSONObject(meal.key) ?: return@forEach
                builder.putMeal(
                    MealEntry(
                        key = meal.key,
                        time = m.optString("time").takeIf { it.isNotEmpty() },
                        hunger = m.optInt("hunger", -1).takeIf { it in 1..5 },
                        food = m.optString("food").takeIf { it.isNotEmpty() },
                        carbs = m.optInt("carbs", -1).takeIf { it in 0..999 },
                    )
                )
            }

            builder.water = dayObj.optInt("water", -1).takeIf { it in 0..8 }
            builder.sport = if (dayObj.has("sport")) dayObj.optBoolean("sport") else null
            builder.steps = dayObj.optInt("steps", -1).takeIf { it >= 0 }
            builder.sleepStart = dayObj.optString("sleepStart").takeIf { it.isNotEmpty() }
            builder.sleepEnd = dayObj.optString("sleepEnd").takeIf { it.isNotEmpty() }
            builder.stress = dayObj.optString("stress").takeIf { it.isNotEmpty() }
            dayObj.optJSONArray("stool")?.let { arr ->
                for (i in 0 until arr.length()) {
                    builder.addStool(arr.optString(i))
                }
            }
            builder.notes = dayObj.optString("notes").takeIf { it.isNotEmpty() }
            builder.conclusions = dayObj.optString("conclusions").takeIf { it.isNotEmpty() }

            result.add(builder.toPortable(dateKey))
        }
        if (result.isEmpty()) throw IllegalArgumentException("Файл не похож на экспорт Glucolog")
        return result
    }

    fun isNewFormat(text: String): Boolean = try {
        val root = JSONObject(text)
        val keys = root.keys()
        var found = false
        while (keys.hasNext() && !found) {
            val dayObj = root.optJSONObject(keys.next()) ?: continue
            val arr = dayObj.optJSONArray("glucose") ?: continue
            for (i in 0 until arr.length()) {
                if (arr.optJSONObject(i)?.has("source") == true) {
                    found = true
                    break
                }
            }
        }
        found
    } catch (e: Exception) {
        false
    }

    private fun dayToJson(day: PortableDay): JSONObject {
        val o = JSONObject()
        if (day.glucose.isNotEmpty()) {
            o.put("glucose", JSONArray().apply {
                day.glucose.sortedBy { it.h }.forEach { p ->
                    put(JSONObject()
                        .put("h", p.h.toDouble())
                        .put("g", p.g.toDouble())
                        .put("source", p.source))
                }
            })
        }
        if (day.insulin.isNotEmpty()) {
            o.put("insulin", JSONArray().apply {
                day.insulin.sortedBy { it.h }.forEach { p ->
                    val jo = JSONObject().put("h", p.h.toDouble())
                    p.b?.takeIf { it > 0f }?.let { jo.put("b", it.toDouble()) }
                    p.ba?.takeIf { it > 0f }?.let { jo.put("ba", it.toDouble()) }
                    put(jo)
                }
            })
        }
        Constants.MEALS.forEach { meal ->
            val m = day.meals.firstOrNull { it.key == meal.key } ?: return@forEach
            val mo = JSONObject()
            m.time?.let { mo.put("time", it) }
            m.hunger?.let { mo.put("hunger", it) }
            m.food?.let { mo.put("food", it) }
            m.carbs?.let { mo.put("carbs", it) }
            o.put(meal.key, mo)
        }
        day.water?.let { o.put("water", it) }
        day.sport?.let { o.put("sport", it) }
        day.steps?.let { o.put("steps", it) }
        day.sleepStart?.let { o.put("sleepStart", it) }
        day.sleepEnd?.let { o.put("sleepEnd", it) }
        day.stress?.let { o.put("stress", it) }
        if (day.stool.isNotEmpty()) {
            o.put("stool", JSONArray().apply { day.stool.forEach { put(it) } })
        }
        day.notes?.let { o.put("notes", it) }
        day.conclusions?.let { o.put("conclusions", it) }
        return o
    }

    private val DATE_REGEX = Regex("\\d{4}-\\d{2}-\\d{2}")
}
