package com.koftamainee.glucolog.di

import android.content.Context
import com.koftamainee.glucolog.data.DayRepository
import com.koftamainee.glucolog.data.ProductRepository
import com.koftamainee.glucolog.data.SettingsDataStore
import com.koftamainee.glucolog.data.db.AppDatabase
import com.koftamainee.glucolog.data.xdrip.XdripStatusProvider
import com.koftamainee.glucolog.data.xdrip.XdripWebClient

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext

    val database: AppDatabase = AppDatabase.build(appContext)
    val dayRepository: DayRepository = DayRepository(database)
    val settingsDataStore: SettingsDataStore = SettingsDataStore(appContext)
    val xdripWebClient: XdripWebClient = XdripWebClient()
    val xdripStatusProvider: XdripStatusProvider =
        XdripStatusProvider(dayRepository, settingsDataStore)
    val productRepository: ProductRepository = ProductRepository(database)
}
