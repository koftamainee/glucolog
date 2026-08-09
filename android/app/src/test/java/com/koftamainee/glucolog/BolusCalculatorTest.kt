package com.koftamainee.glucolog

import com.koftamainee.glucolog.domain.BolusCalculator
import com.koftamainee.glucolog.domain.FoodNutrients
import com.koftamainee.glucolog.domain.FoodPortion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BolusCalculatorTest {

    private val kovrizhka = FoodNutrients(kcal = 368f, proteins = 5.5f, fats = 6f, carbs = 73f)
    private val pastila = FoodNutrients(kcal = 352.4f, proteins = 0.9f, fats = 0f, carbs = 87.2f)

    @Test
    fun breadUnits_kovrizhka142() {
        assertEquals(8.64f, BolusCalculator.breadUnits(kovrizhka.carbs, 142f), 0.001f)
    }

    @Test
    fun bje_scaledByMass_kovrizhka142() {
        assertEquals(1.08f, BolusCalculator.bje(kovrizhka.kcal, kovrizhka.carbs, 142f), 0.001f)
    }

    @Test
    fun breadUnits_pastila32() {
        assertEquals(2.33f, BolusCalculator.breadUnits(pastila.carbs, 32f), 0.001f)
    }

    @Test
    fun bje_pastila32() {
        assertEquals(0.01f, BolusCalculator.bje(pastila.kcal, pastila.carbs, 32f), 0.001f)
    }

    @Test
    fun derivedFactors_defaultDose20() {
        assertEquals(0.48f, BolusCalculator.carbohydrateCoefficient(20f), 0.0001f)
        assertEquals(5f, BolusCalculator.insulinSensitivityFactor(20f), 0.0001f)
    }

    @Test
    fun sumsAcrossItems() {
        val items = listOf(
            FoodPortion(kovrizhka, 142f),
            FoodPortion(pastila, 32f),
        )
        val result = BolusCalculator.calculate(
            items = items,
            ug = 0.48f,
            fchi = 5f,
            targetGlucose = 5f,
            actualGlucose = 5f,
            activeInsulin = 0f,
        )
        assertEquals(10.97f, result.breadUnits, 0.001f)
        assertEquals(1.09f, result.bje, 0.001f)
        assertEquals(131.56f, result.totalCarbs, 0.01f)
        assertEquals(1.05f, result.insulinOnBreadUnits, 0.001f)
        assertEquals(0.1f, result.insulinOnBje, 0.001f)
        assertEquals(1.15f, result.total, 0.001f)
    }

    @Test
    fun correctionIsZeroWhenActualEqualsTarget() {
        val result = BolusCalculator.calculate(
            items = listOf(FoodPortion(kovrizhka, 142f)),
            ug = 0.48f,
            fchi = 5f,
            targetGlucose = 5f,
            actualGlucose = 5f,
            activeInsulin = 0f,
        )
        assertEquals(0f, result.correction, 0.001f)
    }

    @Test
    fun correctionPositiveAboveTarget() {
        val result = BolusCalculator.calculate(
            items = emptyList(),
            ug = 0.48f,
            fchi = 5f,
            targetGlucose = 5f,
            actualGlucose = 10f,
            activeInsulin = 0f,
        )
        assertEquals(1f, result.correction, 0.001f)
        assertEquals(1f, result.total, 0.001f)
    }

    @Test
    fun activeInsulinSubtracted() {
        val result = BolusCalculator.calculate(
            items = listOf(FoodPortion(kovrizhka, 142f)),
            ug = 0.48f,
            fchi = 5f,
            targetGlucose = 5f,
            actualGlucose = 5f,
            activeInsulin = 0.4f,
        )
        assertEquals(0.53f, result.total, 0.001f)
    }

    @Test
    fun zeroDoseIsSafe() {
        val result = BolusCalculator.calculate(
            items = listOf(FoodPortion(kovrizhka, 142f)),
            ug = 0f,
            fchi = 0f,
            targetGlucose = 5f,
            actualGlucose = 5f,
            activeInsulin = 0f,
        )
        assertEquals(0f, result.insulinOnBreadUnits, 0.001f)
        assertEquals(0f, result.insulinOnBje, 0.001f)
        assertEquals(0f, result.correction, 0.001f)
    }

    @Test
    fun emptyItems() {
        val result = BolusCalculator.calculate(
            items = emptyList(),
            ug = 0.48f,
            fchi = 5f,
            targetGlucose = 5f,
            actualGlucose = 5f,
            activeInsulin = 0f,
        )
        assertEquals(0f, result.breadUnits, 0.001f)
        assertEquals(0f, result.bje, 0.001f)
        assertEquals(0f, result.total, 0.001f)
    }

    @Test
    fun round2Works() {
        assertEquals(1.08f, BolusCalculator.round2(1.0792f), 0.001f)
        assertEquals(0.83f, BolusCalculator.round2(0.82928f), 0.001f)
        assertTrue(BolusCalculator.round2(-0.07f) < 0f)
    }
}
