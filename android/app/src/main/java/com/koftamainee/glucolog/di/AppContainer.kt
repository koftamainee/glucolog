package com.koftamainee.glucolog.di

import android.content.Context
import com.koftamainee.glucolog.data.DayRepository
import com.koftamainee.glucolog.data.SettingsDataStore
import com.koftamainee.glucolog.data.db.AppDatabase

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase = AppDatabase.build(appContext)
    val dayRepository: DayRepository = DayRepository(database)
    val settingsDataStore: SettingsDataStore = SettingsDataStore(appContext)
}
