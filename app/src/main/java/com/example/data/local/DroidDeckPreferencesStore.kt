package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.DeckAccentTheme
import com.example.data.telemetry.ThermalTuningConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.droidDeckDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "droiddeck_settings",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, "droiddeck_prefs"))
    }
)

data class DroidDeckPreferences(
    val accentTheme: DeckAccentTheme = DeckAccentTheme.CYBER_CYAN,
    val showRamGauge: Boolean = true,
    val showBatteryGauge: Boolean = true,
    val showThermalGauge: Boolean = true,
    val showStorageGauge: Boolean = true,
    val thermalLimitCelsius: Int = ThermalTuningConfig.DEFAULT_TEMP_C,
    val useFahrenheit: Boolean = false,
    val isSessionGuardEnabled: Boolean = true
)

class DroidDeckPreferencesStore internal constructor(
    private val dataStore: DataStore<Preferences>
) {
    constructor(context: Context) : this(context.applicationContext.droidDeckDataStore)

    val preferencesFlow: Flow<DroidDeckPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            DroidDeckPreferences(
                accentTheme = DeckAccentTheme.fromStorageKey(prefs[KEY_ACCENT_THEME]),
                showRamGauge = prefs[KEY_SHOW_RAM_GAUGE] ?: true,
                showBatteryGauge = prefs[KEY_SHOW_BATTERY_GAUGE] ?: true,
                showThermalGauge = prefs[KEY_SHOW_THERMAL_GAUGE] ?: true,
                showStorageGauge = prefs[KEY_SHOW_STORAGE_GAUGE] ?: true,
                thermalLimitCelsius = ThermalTuningConfig.clampCelsius(
                    prefs[KEY_THERMAL_LIMIT_C] ?: ThermalTuningConfig.DEFAULT_TEMP_C
                ),
                useFahrenheit = prefs[KEY_USE_FAHRENHEIT] ?: false,
                isSessionGuardEnabled = prefs[KEY_SESSION_GUARD_ENABLED] ?: true
            )
        }

    suspend fun setAccentTheme(theme: DeckAccentTheme) {
        dataStore.edit { prefs ->
            prefs[KEY_ACCENT_THEME] = theme.storageKey
        }
    }

    suspend fun setShowRamGauge(visible: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_SHOW_RAM_GAUGE] = visible
        }
    }

    suspend fun setShowBatteryGauge(visible: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_SHOW_BATTERY_GAUGE] = visible
        }
    }

    suspend fun setShowThermalGauge(visible: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_SHOW_THERMAL_GAUGE] = visible
        }
    }

    suspend fun setShowStorageGauge(visible: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_SHOW_STORAGE_GAUGE] = visible
        }
    }

    suspend fun setThermalLimitCelsius(tempC: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_THERMAL_LIMIT_C] = ThermalTuningConfig.clampCelsius(tempC)
        }
    }

    suspend fun setUseFahrenheit(useFahrenheit: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_USE_FAHRENHEIT] = useFahrenheit
        }
    }

    suspend fun setSessionGuardEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_SESSION_GUARD_ENABLED] = enabled
        }
    }

    suspend fun resetCustomization() {
        dataStore.edit { prefs ->
            prefs[KEY_ACCENT_THEME] = DeckAccentTheme.CYBER_CYAN.storageKey
            prefs[KEY_SHOW_RAM_GAUGE] = true
            prefs[KEY_SHOW_BATTERY_GAUGE] = true
            prefs[KEY_SHOW_THERMAL_GAUGE] = true
            prefs[KEY_SHOW_STORAGE_GAUGE] = true
            prefs[KEY_THERMAL_LIMIT_C] = ThermalTuningConfig.DEFAULT_TEMP_C
        }
    }

    companion object {
        private val KEY_ACCENT_THEME = stringPreferencesKey("accent_theme")
        private val KEY_SHOW_RAM_GAUGE = booleanPreferencesKey("show_ram_gauge")
        private val KEY_SHOW_BATTERY_GAUGE = booleanPreferencesKey("show_battery_gauge")
        private val KEY_SHOW_THERMAL_GAUGE = booleanPreferencesKey("show_thermal_gauge")
        private val KEY_SHOW_STORAGE_GAUGE = booleanPreferencesKey("show_storage_gauge")
        private val KEY_THERMAL_LIMIT_C = intPreferencesKey("thermal_limit_celsius")
        private val KEY_USE_FAHRENHEIT = booleanPreferencesKey("use_fahrenheit")
        private val KEY_SESSION_GUARD_ENABLED = booleanPreferencesKey("session_guard_enabled")
    }
}
