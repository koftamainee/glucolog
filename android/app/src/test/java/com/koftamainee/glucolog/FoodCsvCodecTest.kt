package com.koftamainee.glucolog

import com.koftamainee.glucolog.data.db.ProductEntity
import com.koftamainee.glucolog.data.importexport.FoodCsvCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodCsvCodecTest {

    @Test
    fun `export then parse round-trips products`() {
        val product = ProductEntity(
            id = 1,
            name = "Коврижка",
            kcal = 368f,
            proteins = 5.5f,
            fats = 6f,
            carbs = 73f,
            portionMass = 142,
            note = "с мёдом",
            source = "manual",
        )
        val text = FoodCsvCodec.export(products = listOf(product))
        val parsed = FoodCsvCodec.parse(text)
        assertEquals(1, parsed.products.size)
        assertEquals("Коврижка", parsed.products[0].name)
        assertEquals(368f, parsed.products[0].kcal, 0.001f)
        assertEquals(5.5f, parsed.products[0].proteins, 0.001f)
        assertEquals(6f, parsed.products[0].fats, 0.001f)
        assertEquals(73f, parsed.products[0].carbs, 0.001f)
        assertEquals(142, parsed.products[0].portionMass)
        assertEquals("с мёдом", parsed.products[0].note)
        assertEquals("manual", parsed.products[0].source)
    }

    @Test
    fun `recipe rows are ignored on import`() {
        val csv = listOf(
            "Тип;Название;Ккал;Белки;Жиры;Углеводы;МассаПорции;Примечание;Источник",
            "ПРОДУКТ;Мука;350;10;1;75;100;;manual",
            "РЕЦЕПТ;Омлет;200;11;8;4;150;;manual",
            "ИНГРЕДИЕНТ;Омлет;Мука;100",
        ).joinToString("\n")
        val parsed = FoodCsvCodec.parse(csv)
        assertEquals(1, parsed.products.size)
        assertEquals("Мука", parsed.products[0].name)
    }

    @Test
    fun `empty or wrong file throws`() {
        var threw = false
        try {
            FoodCsvCodec.parse("это не экспорт")
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun `numbers with comma are parsed`() {
        val csv = "Тип;Название;Ккал;Белки;Жиры;Углеводы;МассаПорции;Примечание;Источник\n" +
            "ПРОДУКТ;Хлеб;265,5;8;3,2;48;50;;manual"
        val parsed = FoodCsvCodec.parse(csv)
        assertNotNull(parsed.products.singleOrNull())
        assertEquals(265.5f, parsed.products[0].kcal, 0.001f)
        assertEquals(3.2f, parsed.products[0].fats, 0.001f)
    }

    @Test
    fun `semicolon and quotes are escaped`() {
        val product = ProductEntity(
            name = "Сыр; плавленый",
            kcal = 250f,
            proteins = 10f,
            fats = 15f,
            carbs = 20f,
            portionMass = 0,
            note = "вкус \"сливочный\"",
            source = "manual",
        )
        val parsed = FoodCsvCodec.parse(FoodCsvCodec.export(listOf(product)))
        assertEquals("Сыр; плавленый", parsed.products[0].name)
        assertEquals("вкус \"сливочный\"", parsed.products[0].note)
    }
}
