package com.koftamainee.glucolog.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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

data class MarkerLevelSettings(
    val basal: Float = 14f,
    val meal: Float = 14f,
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
    private val targetGlucoseKey = floatPreferencesKey("target_glucose")
    private val markerLevelBasalKey = floatPreferencesKey("marker_level_basal")
    private val markerLevelMealKey = floatPreferencesKey("marker_level_meal")
    private val backupEnabledKey = booleanPreferencesKey("backup_enabled")
    private val backupIntervalHoursKey = intPreferencesKey("backup_interval_hours")
    private val backupKeepCountKey = intPreferencesKey("backup_keep_count")
    private val backupDaysKey = booleanPreferencesKey("backup_days")
    private val backupProductsKey = booleanPreferencesKey("backup_products")
    private val backupLastTimeKey = longPreferencesKey("backup_last_time")
    private val backupLastErrorKey = stringPreferencesKey("backup_last_error")
    private val backupGoogleEmailKey = stringPreferencesKey("backup_google_email")
    private val googleForcePickerKey = booleanPreferencesKey("google_force_picker")

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

    val targetGlucose: Flow<Float> =
        context.dataStore.data.map { prefs -> prefs[targetGlucoseKey] ?: 5f }

    suspend fun setTargetGlucose(value: Float) {
        context.dataStore.edit { prefs -> prefs[targetGlucoseKey] = value }
    }

    val markerLevels: Flow<MarkerLevelSettings> =
        context.dataStore.data.map { prefs ->
            MarkerLevelSettings(
                basal = prefs[markerLevelBasalKey] ?: 14f,
                meal = prefs[markerLevelMealKey] ?: 14f,
            )
        }

    suspend fun setMarkerLevelBasal(value: Float) {
        context.dataStore.edit { prefs -> prefs[markerLevelBasalKey] = value }
    }

    suspend fun setMarkerLevelMeal(value: Float) {
        context.dataStore.edit { prefs -> prefs[markerLevelMealKey] = value }
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

    val backupEnabled: Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[backupEnabledKey] ?: false }

    suspend fun setBackupEnabled(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[backupEnabledKey] = value }
    }

    val backupIntervalHours: Flow<Int> =
        context.dataStore.data.map { prefs -> prefs[backupIntervalHoursKey] ?: 24 }

    suspend fun setBackupIntervalHours(value: Int) {
        context.dataStore.edit { prefs -> prefs[backupIntervalHoursKey] = value }
    }

    val backupKeepCount: Flow<Int> =
        context.dataStore.data.map { prefs -> prefs[backupKeepCountKey] ?: 2 }

    suspend fun setBackupKeepCount(value: Int) {
        context.dataStore.edit { prefs -> prefs[backupKeepCountKey] = value }
    }

    val backupDays: Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[backupDaysKey] ?: true }

    suspend fun setBackupDays(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[backupDaysKey] = value }
    }

    val backupProducts: Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[backupProductsKey] ?: true }

    suspend fun setBackupProducts(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[backupProductsKey] = value }
    }

    val backupLastTime: Flow<Long> =
        context.dataStore.data.map { prefs -> prefs[backupLastTimeKey] ?: 0L }

    suspend fun setBackupLastTime(value: Long) {
        context.dataStore.edit { prefs -> prefs[backupLastTimeKey] = value }
    }

    val backupLastError: Flow<String?> =
        context.dataStore.data.map { prefs -> prefs[backupLastErrorKey] }

    suspend fun setBackupLastError(value: String?) {
        context.dataStore.edit { prefs ->
            if (value == null) prefs.remove(backupLastErrorKey)
            else prefs[backupLastErrorKey] = value
        }
    }

    val backupGoogleEmail: Flow<String?> =
        context.dataStore.data.map { prefs -> prefs[backupGoogleEmailKey] }

    suspend fun setBackupGoogleEmail(value: String?) {
        context.dataStore.edit { prefs ->
            if (value == null) prefs.remove(backupGoogleEmailKey)
            else prefs[backupGoogleEmailKey] = value
        }
    }

    val googleForcePicker: Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[googleForcePickerKey] ?: false }

    suspend fun setGoogleForcePicker(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[googleForcePickerKey] = value }
    }
}
