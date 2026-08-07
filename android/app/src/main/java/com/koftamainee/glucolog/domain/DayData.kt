package com.koftamainee.glucolog.domain

import com.koftamainee.glucolog.data.db.DayEntity
import com.koftamainee.glucolog.data.db.GlucoseEntity
import com.koftamainee.glucolog.data.db.InsulinEntity
import com.koftamainee.glucolog.data.db.MealEntity
import com.koftamainee.glucolog.data.db.StoolEntity
import java.time.LocalDate

data class DayData(
    val date: LocalDate,
    val day: DayEntity?,
    val glucose: List<GlucoseEntity>,
    val insulin: List<InsulinEntity>,
    val prevInsulin: List<InsulinEntity> = emptyList(),
    val meals: List<MealEntity>,
    val stool: List<StoolEntity>,
)
