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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.koftamainee.glucolog.data.db.MealEntity
import com.koftamainee.glucolog.domain.Constants
import com.koftamainee.glucolog.domain.MealField
import com.koftamainee.glucolog.ui.components.DebouncedOutlinedTextField
import com.koftamainee.glucolog.ui.components.PrimaryAddButton
import com.koftamainee.glucolog.ui.components.SectionCard
import com.koftamainee.glucolog.ui.components.TimeField

@Composable
fun MealsSection(
    meals: List<MealEntity>,
    onSetField: (String, MealField, String?) -> Unit,
    onAdd: () -> Unit,
    onDelete: (Long) -> Unit,
) {
    SectionCard(title = "Приёмы пищи") {
        if (meals.isEmpty()) {
            Text(
                text = "Пока нет приёмов пищи",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
        meals.forEachIndexed { index, meal ->
            MealCard(
                title = "Приём пищи ${index + 1}",
                meal = meal,
                onSetField = { field, value -> onSetField(meal.key, field, value) },
                onDelete = { onDelete(meal.id) },
            )
        }
        PrimaryAddButton(
            text = "Добавить",
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MealCard(
    title: String,
    meal: MealEntity?,
    onSetField: (MealField, String?) -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить приём пищи",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DebouncedOutlinedTextField(
                    value = meal?.food ?: "",
                    onCommit = { onSetField(MealField.FOOD, it) },
                    placeholder = "Приём пищи",
                    minLines = 2,
                    modifier = Modifier.weight(1f),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Углеводы",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    DebouncedOutlinedTextField(
                        value = meal?.carbs?.toString() ?: "",
                        onCommit = { onSetField(MealField.CARBS, it) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(64.dp),
                    )
                }
            }
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
