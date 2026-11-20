package com.example.data.telemetry

import android.content.Context
import java.util.Locale
import kotlin.math.roundToInt

enum class ThermalPerformanceZone(
    val title: String,
    val badge: String,
    val phoneBenefitTitle: String,
    val phoneBenefitBody: String,
    val foregroundSampleMs: Long,
    val backgroundSampleMs: Long
) {
    COOL_ECO(
        title = "Cool & Eco Guard",
        badge = "COOL & ECO",
        phoneBenefitTitle = "Protects battery lifespan & keeps your phone cool",
        phoneBenefitBody = "Alerts early at lower heat to slow long-term battery wear, keep the chassis comfortable in hand, and prevent early OS thermal throttling. Slows background sampling to reduce wakeups.",
        foregroundSampleMs = 6_000L,
        backgroundSampleMs = 15_000L
    ),
    BALANCED(
        title = "Balanced Stability",
        badge = "BALANCED",
        phoneBenefitTitle = "Sustains smooth FPS without excessive heat",
        phoneBenefitBody = "The ideal everyday gaming balance—gives games healthy thermal headroom for consistent frame pacing while warning you before Android begins aggressive clock throttling.",
        foregroundSampleMs = 4_000L,
        backgroundSampleMs = 10_000L
    ),
    PERFORMANCE(
        title = "Performance Runway",
        badge = "PERFORMANCE",
        phoneBenefitTitle = "Extends thermal runway for demanding 3D titles",
        phoneBenefitBody = "Lets your phone run warmer before alerting so heavy games and emulators hold boost clocks longer, while increasing sensor polling speed to catch sudden heat spikes.",
        foregroundSampleMs = 2_500L,
        backgroundSampleMs = 6_000L
    ),
    PEAK_WATCH(
        title = "Peak Burst Watch",
        badge = "PEAK WATCH",
        phoneBenefitTitle = "Maximum headroom with rapid thermal vigilance",
        phoneBenefitBody = "Permits near-peak operating temperatures for short competitive bursts and polls rapidly so you are warned right before critical throttling or dim-screen limits trigger.",
        foregroundSampleMs = 2_000L,
        backgroundSampleMs = 5_000L
    )
}

object ThermalTuningConfig {
    const val MIN_TEMP_C = 34
    const val MAX_TEMP_C = 45
    const val DEFAULT_TEMP_C = 39

    fun clampCelsius(tempC: Int): Int = tempC.coerceIn(MIN_TEMP_C, MAX_TEMP_C)

    fun zoneForCelsius(tempC: Int): ThermalPerformanceZone {
        return when (clampCelsius(tempC)) {
            in 34..36 -> ThermalPerformanceZone.COOL_ECO
            in 37..40 -> ThermalPerformanceZone.BALANCED
            in 41..42 -> ThermalPerformanceZone.PERFORMANCE
            else -> ThermalPerformanceZone.PEAK_WATCH
        }
    }

    fun celsiusToFahrenheit(tempC: Float): Float = (tempC * 9f / 5f) + 32f

    fun formatTemp(tempC: Float, useFahrenheit: Boolean, decimals: Int = 1): String {
        return if (useFahrenheit) {
            val f = celsiusToFahrenheit(tempC)
            if (decimals == 0) {
                "${f.roundToInt()}°F"
            } else {
                String.format(Locale.US, "%.${decimals}f°F", f)
            }
        } else {
            if (decimals == 0) {
                "${tempC.roundToInt()}°C"
            } else {
                String.format(Locale.US, "%.${decimals}f°C", tempC)
            }
        }
    }

    fun formatDelta(deltaC: Float, useFahrenheit: Boolean): String {
        val value = if (useFahrenheit) deltaC * 9f / 5f else deltaC
        val unit = if (useFahrenheit) "°F" else "°C"
        val sign = if (value >= 0f) "+" else ""
        return String.format(Locale.US, "%s%.1f%s", sign, value, unit)
    }
}

class ThermalTuningStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getThermalLimitCelsius(): Int {
        val raw = prefs.getInt(KEY_THERMAL_LIMIT_C, ThermalTuningConfig.DEFAULT_TEMP_C)
        return ThermalTuningConfig.clampCelsius(raw)
    }

    fun setThermalLimitCelsius(tempC: Int) {
        prefs.edit()
            .putInt(KEY_THERMAL_LIMIT_C, ThermalTuningConfig.clampCelsius(tempC))
            .apply()
    }

    fun isUseFahrenheit(): Boolean = prefs.getBoolean(KEY_USE_FAHRENHEIT, false)

    fun setUseFahrenheit(useFahrenheit: Boolean) {
        prefs.edit().putBoolean(KEY_USE_FAHRENHEIT, useFahrenheit).apply()
    }

    companion object {
        private const val PREFS_NAME = "droiddeck_prefs"
        private const val KEY_THERMAL_LIMIT_C = "thermal_limit_celsius"
        private const val KEY_USE_FAHRENHEIT = "use_fahrenheit"
    }
}
