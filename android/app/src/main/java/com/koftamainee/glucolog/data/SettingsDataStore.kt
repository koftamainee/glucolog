package com.koftamainee.glucolog.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeMode(val storageValue: String) {
    SYSTEM("system"),
    DARK("dark"),
    LIGHT("light");

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: SYSTEM
    }
}

class SettingsDataStore(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme_mode")
    private val xdripConnectedKey = booleanPreferencesKey("xdrip_connected")
    private val xiaomiServiceEnabledKey = booleanPreferencesKey("xiaomi_service_enabled")

    val themeMode: Flow<ThemeMode> =
        context.dataStore.data.map { prefs ->
            ThemeMode.fromStorage(prefs[themeKey])
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[themeKey] = mode.storageValue
        }
    }

    val xdripConnected: Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[xdripConnectedKey] ?: false }

    suspend fun setXdripConnected(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[xdripConnectedKey] = value
        }
    }

    val xiaomiServiceEnabled: Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[xiaomiServiceEnabledKey] ?: false }

    suspend fun isXiaomiServiceEnabled(): Boolean = xiaomiServiceEnabled.first()

    suspend fun setXiaomiServiceEnabled(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[xiaomiServiceEnabledKey] = value
        }
    }
}
