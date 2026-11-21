package com.example.data.model

import androidx.compose.ui.graphics.ImageBitmap

data class InstalledAppInfo(
    val packageName: String,
    val name: String,
    val isDetectedGame: Boolean,
    val iconBitmap: ImageBitmap?,
    val accentColorArgb: Long? = null
)

data class DeckItem(
    val packageName: String,
    val name: String,
    val isDetectedGame: Boolean,
    val isPinned: Boolean,
    val isFavorite: Boolean,
    val isHidden: Boolean,
    val lastLaunchedAt: Long,
    val launchCount: Int,
    val iconBitmap: ImageBitmap?,
    val accentColorArgb: Long? = null
)

enum class ThermalTier(val label: String) {
    COOL("Cool"),
    NOMINAL("Nominal"),
    WARM("Warm"),
    HOT("Hot")
}

enum class AndroidChassisStyle {
    PIXEL_VISOR,
    GALAXY_RINGS,
    CIRCULAR_MODULE,
    NOTHING_MINIMAL,
    FOLDABLE,
    TABLET,
    STANDARD_ANDROID
}

enum class DisplayCutoutStyle {
    NONE,
    PUNCH_HOLE_CENTER,
    PUNCH_HOLE_LEFT,
    PUNCH_HOLE_RIGHT,
    PILL_CENTER,
    NOTCH_CENTER
}

data class DeviceProfile(
    val displayName: String = "Android Device",
    val manufacturer: String = "",
    val model: String = "",
    val androidVersion: String = "",
    val cpuAbi: String = "",
    val cpuCores: Int = 0,
    val refreshRateHz: Int = 60,
    val isTablet: Boolean = false,
    val isFoldable: Boolean = false,
    val totalRamGb: Double = 0.0,
    val chassisStyle: AndroidChassisStyle = AndroidChassisStyle.STANDARD_ANDROID,
    val cutoutStyle: DisplayCutoutStyle = DisplayCutoutStyle.PUNCH_HOLE_CENTER,
    val screenAspectRatio: Float = 2.15f,
    val cornerRoundness: Float = 0.14f
)

data class HardwareTelemetry(
    val freeRamMb: Long = 0L,
    val totalRamMb: Long = 0L,
    val batteryPercent: Int = 0,
    val isCharging: Boolean = false,
    val batteryTempCelsius: Float = 0f,
    val thermalTier: ThermalTier = ThermalTier.NOMINAL,
    val cpuLoadPercent: Int = 0,
    val freeStorageGb: Double = 0.0,
    val totalStorageGb: Double = 0.0,
    val lastUpdatedAt: Long = 0L
) {
    val usedRamMb: Long
        get() = (totalRamMb - freeRamMb).coerceAtLeast(0L)

    val ramFreeFraction: Float
        get() = if (totalRamMb > 0L) (freeRamMb.toFloat() / totalRamMb.toFloat()).coerceIn(0f, 1f) else 0f

    val ramUsedFraction: Float
        get() = if (totalRamMb > 0L) (usedRamMb.toFloat() / totalRamMb.toFloat()).coerceIn(0f, 1f) else 0f

    val cpuLoadFraction: Float
        get() = (cpuLoadPercent / 100f).coerceIn(0f, 1f)

    val storageUsedFraction: Float
        get() = if (totalStorageGb > 0.0) {
            ((totalStorageGb - freeStorageGb) / totalStorageGb).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }
}

enum class DeckFilter {
    ALL,
    FAVORITES,
    GAMES,
    PINNED
}

enum class DeckAccentTheme(val storageKey: String) {
    EMERALD_GREEN("emerald_green"),
    CYBER_CYAN("cyber_cyan"),
    NEON_PURPLE("neon_purple"),
    CRIMSON_RED("crimson_red");

    companion object {
        fun fromStorageKey(key: String?): DeckAccentTheme {
            return entries.firstOrNull { it.storageKey == key } ?: CYBER_CYAN
        }
    }
}
