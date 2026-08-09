package com.koftamainee.glucolog.domain

import kotlin.math.roundToLong

data class FoodNutrients(
    val kcal: Float,
    val proteins: Float,
    val fats: Float,
    val carbs: Float,
)

data class FoodPortion(
    val nutrients: FoodNutrients,
    val mass: Float,
)

data class BolusResult(
    val breadUnits: Float,
    val bje: Float,
    val totalCarbs: Float,
    val insulinOnBreadUnits: Float,
    val insulinOnBje: Float,
    val correction: Float,
    val activeInsulin: Float,
    val total: Float,
)

object BolusCalculator {

    fun carbohydrateCoefficient(totalDailyDose: Float): Float =
        if (totalDailyDose <= 0f) 0f else 12f / (500f / totalDailyDose)

    fun insulinSensitivityFactor(totalDailyDose: Float): Float =
        if (totalDailyDose <= 0f) 0f else 100f / totalDailyDose

    fun breadUnits(carbsPer100: Float, mass: Float): Float =
        round2(carbsPer100 * mass / 100f / 12f)

    fun bje(kcalPer100: Float, carbsPer100: Float, mass: Float): Float =
        round2((kcalPer100 - carbsPer100 * 4f) / 100f * mass / 100f)

    fun totalCarbs(items: List<FoodPortion>): Float =
        items.fold(0f) { acc, item -> acc + item.nutrients.carbs * item.mass / 100f }

    fun calculate(
        items: List<FoodPortion>,
        ug: Float,
        fchi: Float,
        targetGlucose: Float,
        actualGlucose: Float,
        activeInsulin: Float,
    ): BolusResult {
        val totalBu = items.fold(0f) { acc, it -> acc + breadUnits(it.nutrients.carbs, it.mass) }
        val totalBje = items.fold(0f) { acc, it -> acc + bje(it.nutrients.kcal, it.nutrients.carbs, it.mass) }
        val insulinBu = round2(if (fchi == 0f) 0f else totalBu * ug / fchi)
        val insulinBje = round2(if (fchi == 0f) 0f else totalBje * ug / fchi)
        val correction = round2(if (fchi == 0f) 0f else (actualGlucose - targetGlucose) / fchi)
        return BolusResult(
            breadUnits = round2(totalBu),
            bje = round2(totalBje),
            totalCarbs = round2(totalCarbs(items)),
            insulinOnBreadUnits = insulinBu,
            insulinOnBje = insulinBje,
            correction = correction,
            activeInsulin = activeInsulin,
            total = round2(insulinBu + insulinBje + correction - activeInsulin),
        )
    }

    fun round2(value: Float): Float = (value * 100f).roundToLong() / 100f
}
