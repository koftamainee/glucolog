package com.koftamainee.glucolog.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
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

data class MarkerLineSettings(
    val manual: Boolean = true,
    val meal: Boolean = true,
    val bolusStart: Boolean = true,
    val bolusEnd: Boolean = true,
    val basal: Boolean = true,
)

data class TargetRangeSettings(
    val lo: Float = 4f,
    val hi: Float = 8f,
)

class SettingsDataStore(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme_mode")
    private val xdripConnectedKey = booleanPreferencesKey("xdrip_connected")
    private val markerManualKey = booleanPreferencesKey("marker_line_manual")
    private val markerMealKey = booleanPreferencesKey("marker_line_meal")
    private val markerBolusStartKey = booleanPreferencesKey("marker_line_bolus_start")
    private val markerBolusEndKey = booleanPreferencesKey("marker_line_bolus_end")
    private val markerBasalKey = booleanPreferencesKey("marker_line_basal")
    private val targetLoKey = floatPreferencesKey("target_range_lo")
    private val targetHiKey = floatPreferencesKey("target_range_hi")

    val markerLines: Flow<MarkerLineSettings> =
        context.dataStore.data.map { prefs ->
            MarkerLineSettings(
                manual = prefs[markerManualKey] ?: true,
                meal = prefs[markerMealKey] ?: true,
                bolusStart = prefs[markerBolusStartKey] ?: true,
                bolusEnd = prefs[markerBolusEndKey] ?: true,
                basal = prefs[markerBasalKey] ?: true,
            )
        }

    suspend fun setMarkerLineManual(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[markerManualKey] = value }
    }

    suspend fun setMarkerLineMeal(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[markerMealKey] = value }
    }

    suspend fun setMarkerLineBolusStart(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[markerBolusStartKey] = value }
    }

    suspend fun setMarkerLineBolusEnd(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[markerBolusEndKey] = value }
    }

    suspend fun setMarkerLineBasal(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[markerBasalKey] = value }
    }

    val targetRange: Flow<TargetRangeSettings> =
        context.dataStore.data.map { prefs ->
            TargetRangeSettings(
                lo = prefs[targetLoKey] ?: 4f,
                hi = prefs[targetHiKey] ?: 8f,
            )
        }

    suspend fun setTargetLo(value: Float) {
        context.dataStore.edit { prefs -> prefs[targetLoKey] = value }
    }

    suspend fun setTargetHi(value: Float) {
        context.dataStore.edit { prefs -> prefs[targetHiKey] = value }
    }

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
}
