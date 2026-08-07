package com.koftamainee.glucolog.ui.day.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.koftamainee.glucolog.data.db.MealEntity
import com.koftamainee.glucolog.domain.Constants
import com.koftamainee.glucolog.domain.MealField
import com.koftamainee.glucolog.ui.components.DebouncedOutlinedTextField
import com.koftamainee.glucolog.ui.components.SectionCard
import com.koftamainee.glucolog.ui.components.TimeField

@Composable
fun MealsSection(
    meals: List<MealEntity>,
    onSetField: (mealKey: String, field: MealField, value: String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Constants.MEALS.forEach { meal ->
            val data = meals.firstOrNull { it.key == meal.key }
            MealCard(
                title = meal.name,
                meal = data,
                onSetField = { field, value -> onSetField(meal.key, field, value) },
            )
        }
    }
}

@Composable
private fun MealCard(
    title: String,
    meal: MealEntity?,
    onSetField: (MealField, String?) -> Unit,
) {
    SectionCard(title = title) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HungerDots(
                hunger = meal?.hunger ?: 0,
                onSelect = { v -> onSetField(MealField.HUNGER, v.toString()) },
            )
            TimeField(
                value = meal?.time ?: "",
                onValue = { onSetField(MealField.TIME, it) },
                label = "Время",
                modifier = Modifier.weight(1f),
            )
        }

        DebouncedOutlinedTextField(
            value = meal?.food ?: "",
            onCommit = { onSetField(MealField.FOOD, it) },
            placeholder = "Приём пищи",
            minLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DebouncedOutlinedTextField(
                value = meal?.phys ?: "",
                onCommit = { onSetField(MealField.PHYS, it) },
                placeholder = "Физ. ощущения",
                minLines = 2,
                modifier = Modifier.weight(1f),
            )
            DebouncedOutlinedTextField(
                value = meal?.emo ?: "",
                onCommit = { onSetField(MealField.EMO, it) },
                placeholder = "Эмоции",
                minLines = 2,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HungerDots(
    hunger: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Constants.HUNGER_LEVELS.forEach { level ->
            val filled = level <= hunger
            Surface(
                modifier = Modifier.size(28.dp),
                shape = CircleShape,
                color = if (filled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    if (filled) Color.Transparent else MaterialTheme.colorScheme.outline,
                ),
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onSelect(level) },
                )
            }
        }
    }
}
