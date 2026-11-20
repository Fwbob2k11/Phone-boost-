package com.example.data.telemetry

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.Process
import android.os.StatFs
import android.os.SystemClock
import com.example.data.model.HardwareTelemetry
import com.example.data.model.ThermalTier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

class TelemetryMonitor(context: Context) {

    private val appContext = context.applicationContext

    @Volatile
    private var lastWallTimeMs: Long = 0L

    @Volatile
    private var lastProcessCpuTimeMs: Long = 0L

    suspend fun sample(): HardwareTelemetry = withContext(Dispatchers.IO) {
        val (freeRamMb, totalRamMb) = readMemoryMb()
        val ramUsedFraction = if (totalRamMb > 0L) {
            ((totalRamMb - freeRamMb).coerceAtLeast(0L).toFloat() / totalRamMb.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        val (batteryPercent, isCharging, batteryTempCelsius) = readBatteryStats()
        val thermalTier = readThermalTier(batteryTempCelsius)
        val cpuLoadPercent = readHybridCpuLoadPercent(ramUsedFraction)
        val (freeStorageGb, totalStorageGb) = readStorageGb()

        HardwareTelemetry(
            freeRamMb = freeRamMb,
            totalRamMb = totalRamMb,
            batteryPercent = batteryPercent,
            isCharging = isCharging,
            batteryTempCelsius = batteryTempCelsius,
            thermalTier = thermalTier,
            cpuLoadPercent = cpuLoadPercent,
            freeStorageGb = freeStorageGb,
            totalStorageGb = totalStorageGb,
            lastUpdatedAt = System.currentTimeMillis()
        )
    }

    private fun readHybridCpuLoadPercent(ramUsedFraction: Float): Int {
        val cpufreqFraction = readCpuFreqScalingFraction()
        val processCpuFraction = readProcessCpuFraction()
        return computeHybridCpuLoadPercent(
            cpufreqFraction = cpufreqFraction,
            processCpuFraction = processCpuFraction,
            ramUsedFraction = ramUsedFraction
        )
    }

    private fun readCpuFreqScalingFraction(): Float? {
        val coreCount = Runtime.getRuntime().availableProcessors().coerceIn(1, 16)
        var sum = 0f
        var readableCores = 0

        for (core in 0 until coreCount) {
            val basePath = "/sys/devices/system/cpu/cpu$core/cpufreq"
            val cur = readLongFromSysfs("$basePath/scaling_cur_freq") ?: continue
            val max = readLongFromSysfs("$basePath/cpuinfo_max_freq")
                ?: readLongFromSysfs("$basePath/scaling_max_freq")
                ?: continue
            val min = readLongFromSysfs("$basePath/cpuinfo_min_freq") ?: 0L
            if (max > min && cur >= 0L) {
                val normalized = ((cur - min).toFloat() / (max - min).toFloat()).coerceIn(0f, 1f)
                sum += normalized
                readableCores++
            }
        }

        return if (readableCores > 0) (sum / readableCores).coerceIn(0f, 1f) else null
    }

    private fun readLongFromSysfs(path: String): Long? {
        return try {
            val file = File(path)
            if (!file.canRead()) return null
            file.readText().trim().toLongOrNull()
        } catch (_: Exception) {
            null
        }
    }

    private fun readProcessCpuFraction(): Float? {
        val nowWall = SystemClock.elapsedRealtime()
        val nowCpu = Process.getElapsedCpuTime()
        val prevWall = lastWallTimeMs
        val prevCpu = lastProcessCpuTimeMs

        lastWallTimeMs = nowWall
        lastProcessCpuTimeMs = nowCpu

        if (prevWall <= 0L || nowWall <= prevWall || nowCpu < prevCpu) {
            return null
        }

        val wallDelta = (nowWall - prevWall).coerceAtLeast(1L)
        val cpuDelta = (nowCpu - prevCpu).coerceAtLeast(0L)
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        return (cpuDelta.toFloat() / (wallDelta.toFloat() * cores.toFloat())).coerceIn(0f, 1f)
    }

    private fun readMemoryMb(): Pair<Long, Long> {
        val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return 0L to 0L
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val freeMb = memInfo.availMem / (1024L * 1024L)
        val totalMb = memInfo.totalMem / (1024L * 1024L)
        return freeMb to totalMb
    }

    private fun readBatteryStats(): Triple<Int, Boolean, Float> {
        val batteryIntent = appContext.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) {
            ((level * 100f) / scale.toFloat()).toInt().coerceIn(0, 100)
        } else {
            val bm = appContext.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.coerceIn(0, 100) ?: 0
        }

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempCelsius = (tempTenths / 10f).coerceAtLeast(0f)

        return Triple(percent, isCharging, tempCelsius)
    }

    private fun readThermalTier(batteryTempCelsius: Float): ThermalTier {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager != null) {
                return when (powerManager.currentThermalStatus) {
                    PowerManager.THERMAL_STATUS_NONE -> {
                        if (batteryTempCelsius in 0.1f..32.0f) ThermalTier.COOL else ThermalTier.NOMINAL
                    }
                    PowerManager.THERMAL_STATUS_LIGHT -> ThermalTier.NOMINAL
                    PowerManager.THERMAL_STATUS_MODERATE -> ThermalTier.WARM
                    PowerManager.THERMAL_STATUS_SEVERE,
                    PowerManager.THERMAL_STATUS_CRITICAL,
                    PowerManager.THERMAL_STATUS_EMERGENCY,
                    PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalTier.HOT
                    else -> ThermalTier.NOMINAL
                }
            }
        }
        return when {
            batteryTempCelsius in 0.1f..32.0f -> ThermalTier.COOL
            batteryTempCelsius < 38.5f -> ThermalTier.NOMINAL
            batteryTempCelsius < 43.0f -> ThermalTier.WARM
            else -> ThermalTier.HOT
        }
    }

    private fun readStorageGb(): Pair<Double, Double> {
        return try {
            val statFs = StatFs(Environment.getDataDirectory().path)
            val bytesPerGb = 1024.0 * 1024.0 * 1024.0
            val freeGb = statFs.availableBytes / bytesPerGb
            val totalGb = statFs.totalBytes / bytesPerGb
            freeGb to totalGb
        } catch (_: Exception) {
            0.0 to 0.0
        }
    }

    companion object {
        fun computeHybridCpuLoadPercent(
            cpufreqFraction: Float?,
            processCpuFraction: Float?,
            ramUsedFraction: Float
        ): Int {
            val ram = ramUsedFraction.coerceIn(0f, 1f)
            val proc = processCpuFraction?.coerceIn(0f, 1f)
            val freq = cpufreqFraction?.coerceIn(0f, 1f)

            val hybridFraction = when {
                freq != null && proc != null -> freq * 0.55f + proc * 0.25f + ram * 0.20f
                freq != null -> freq * 0.70f + ram * 0.30f
                proc != null -> proc * 0.55f + ram * 0.45f
                else -> ram * 0.70f
            }
            return (hybridFraction * 100f).roundToInt().coerceIn(0, 100)
        }
    }
}
