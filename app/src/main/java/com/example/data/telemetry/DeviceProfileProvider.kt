package com.example.data.telemetry

import android.app.ActivityManager
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.RoundedCorner
import android.view.WindowManager
import com.example.data.model.AndroidChassisStyle
import com.example.data.model.DeviceProfile
import com.example.data.model.DisplayCutoutStyle
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class DeviceProfileProvider(context: Context) {

    private val appContext = context.applicationContext

    fun read(): DeviceProfile {
        val manufacturer = Build.MANUFACTURER.orEmpty().trim()
        val model = Build.MODEL.orEmpty().trim()
        val displayName = formatDeviceDisplayName(
            manufacturer = manufacturer,
            model = model
        )
        val androidVersion = "Android ${Build.VERSION.RELEASE ?: Build.VERSION.SDK_INT.toString()}"
        val cpuAbi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty().ifBlank { "arm64" }
        val cpuCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val refreshRateHz = readRefreshRateHz()
        val smallestWidthDp = appContext.resources.configuration.smallestScreenWidthDp
        val (screenWidthPx, screenHeightPx) = readScreenDimensionsPx()
        val shortPx = min(screenWidthPx, screenHeightPx).coerceAtLeast(1)
        val longPx = max(screenWidthPx, screenHeightPx).coerceAtLeast(1)
        val aspectRatio = (longPx.toFloat() / shortPx.toFloat()).coerceIn(1.1f, 2.45f)

        val lowerModel = model.lowercase()
        val isFoldable = lowerModel.contains("fold") ||
            (smallestWidthDp in 520..760 && aspectRatio < 1.45f)
        val isTablet = !isFoldable && smallestWidthDp >= 600
        val totalRamGb = readTotalRamGb()

        val chassisStyle = detectChassisStyle(
            manufacturer = manufacturer,
            model = model,
            isTablet = isTablet,
            isFoldable = isFoldable
        )
        val cutoutStyle = readCutoutStyle(shortPx)
        val cornerRoundness = readCornerRoundness(
            model = model,
            shortPx = shortPx,
            isTablet = isTablet
        )

        return DeviceProfile(
            displayName = displayName,
            manufacturer = manufacturer,
            model = model,
            androidVersion = androidVersion,
            cpuAbi = cpuAbi,
            cpuCores = cpuCores,
            refreshRateHz = refreshRateHz,
            isTablet = isTablet,
            isFoldable = isFoldable,
            totalRamGb = totalRamGb,
            chassisStyle = chassisStyle,
            cutoutStyle = cutoutStyle,
            screenAspectRatio = aspectRatio,
            cornerRoundness = cornerRoundness
        )
    }

    private fun readRefreshRateHz(): Int {
        return try {
            val dm = appContext.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            val display = dm?.getDisplay(Display.DEFAULT_DISPLAY)
            val rate = display?.refreshRate ?: 60f
            rate.roundToInt().coerceIn(30, 240)
        } catch (_: Exception) {
            60
        }
    }

    @Suppress("DEPRECATION")
    private fun readScreenDimensionsPx(): Pair<Int, Int> {
        return try {
            val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && wm != null) {
                val bounds = wm.maximumWindowMetrics.bounds
                bounds.width().coerceAtLeast(1) to bounds.height().coerceAtLeast(1)
            } else {
                val metrics = DisplayMetrics()
                val dm = appContext.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
                dm?.getDisplay(Display.DEFAULT_DISPLAY)?.getRealMetrics(metrics)
                val w = metrics.widthPixels.coerceAtLeast(appContext.resources.displayMetrics.widthPixels)
                val h = metrics.heightPixels.coerceAtLeast(appContext.resources.displayMetrics.heightPixels)
                w.coerceAtLeast(1) to h.coerceAtLeast(1)
            }
        } catch (_: Exception) {
            val dm = appContext.resources.displayMetrics
            dm.widthPixels.coerceAtLeast(1080) to dm.heightPixels.coerceAtLeast(2400)
        }
    }

    private fun readCutoutStyle(displayWidthPx: Int): DisplayCutoutStyle {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return DisplayCutoutStyle.PUNCH_HOLE_CENTER
        }
        return try {
            val dm = appContext.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            val display = dm?.getDisplay(Display.DEFAULT_DISPLAY)
            val cutout = display?.cutout ?: return DisplayCutoutStyle.NONE
            val topRect = cutout.boundingRectTop
            if (topRect.isEmpty) {
                return DisplayCutoutStyle.NONE
            }
            val safeWidth = displayWidthPx.coerceAtLeast(1).toFloat()
            val centerRatio = topRect.centerX().toFloat() / safeWidth
            val widthRatio = topRect.width().toFloat() / safeWidth
            val shapeRatio = topRect.width().toFloat() / topRect.height().coerceAtLeast(1).toFloat()

            when {
                centerRatio < 0.36f -> DisplayCutoutStyle.PUNCH_HOLE_LEFT
                centerRatio > 0.64f -> DisplayCutoutStyle.PUNCH_HOLE_RIGHT
                widthRatio > 0.22f && shapeRatio > 2.6f -> DisplayCutoutStyle.NOTCH_CENTER
                shapeRatio > 1.75f -> DisplayCutoutStyle.PILL_CENTER
                else -> DisplayCutoutStyle.PUNCH_HOLE_CENTER
            }
        } catch (_: Exception) {
            DisplayCutoutStyle.PUNCH_HOLE_CENTER
        }
    }

    private fun readCornerRoundness(
        model: String,
        shortPx: Int,
        isTablet: Boolean
    ): Float {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val dm = appContext.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
                val display = dm?.getDisplay(Display.DEFAULT_DISPLAY)
                val topLeft = display?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)
                if (topLeft != null && topLeft.radius > 0) {
                    return (topLeft.radius.toFloat() / shortPx.coerceAtLeast(1).toFloat())
                        .coerceIn(0.03f, 0.22f)
                }
            } catch (_: Exception) {
                // Fall back to model heuristics below
            }
        }
        val lowerModel = model.lowercase()
        return when {
            isTablet -> 0.08f
            lowerModel.contains("ultra") || lowerModel.contains("xperia") -> 0.05f
            else -> 0.14f
        }
    }

    private fun readTotalRamGb(): Double {
        return try {
            val am = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                ?: return 0.0
            val info = ActivityManager.MemoryInfo()
            am.getMemoryInfo(info)
            info.totalMem / (1024.0 * 1024.0 * 1024.0)
        } catch (_: Exception) {
            0.0
        }
    }

    companion object {
        fun detectChassisStyle(
            manufacturer: String,
            model: String,
            isTablet: Boolean,
            isFoldable: Boolean
        ): AndroidChassisStyle {
            if (isFoldable) return AndroidChassisStyle.FOLDABLE
            if (isTablet) return AndroidChassisStyle.TABLET

            val brand = manufacturer.trim().lowercase()
            val cleanModel = model.trim().lowercase()

            return when {
                brand.contains("google") || cleanModel.contains("pixel") ->
                    AndroidChassisStyle.PIXEL_VISOR
                brand.contains("samsung") || cleanModel.startsWith("sm-") || cleanModel.contains("galaxy") ->
                    AndroidChassisStyle.GALAXY_RINGS
                brand.contains("oneplus") || brand.contains("xiaomi") || brand.contains("redmi") ||
                    brand.contains("poco") || brand.contains("oppo") || brand.contains("vivo") ||
                    brand.contains("realme") ->
                    AndroidChassisStyle.CIRCULAR_MODULE
                brand.contains("nothing") || cleanModel.contains("phone (") ->
                    AndroidChassisStyle.NOTHING_MINIMAL
                else -> AndroidChassisStyle.STANDARD_ANDROID
            }
        }

        fun formatDeviceDisplayName(manufacturer: String, model: String): String {
            val cleanBrand = manufacturer.trim()
            val cleanModel = model.trim()

            if (cleanModel.isBlank()) {
                return if (cleanBrand.isBlank()) {
                    "Android Device"
                } else {
                    cleanBrand.replaceFirstChar { it.uppercase() }
                }
            }

            if (cleanBrand.isBlank()) {
                return cleanModel
            }

            if (cleanModel.startsWith(cleanBrand, ignoreCase = true)) {
                return cleanModel.replaceFirstChar { it.uppercase() }
            }

            if (cleanBrand.equals("Google", ignoreCase = true) &&
                cleanModel.startsWith("Pixel", ignoreCase = true)
            ) {
                return cleanModel
            }

            val formattedBrand = cleanBrand.replaceFirstChar { it.uppercase() }
            return "$formattedBrand $cleanModel"
        }
    }
}
