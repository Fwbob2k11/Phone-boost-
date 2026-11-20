package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DroidDeckDatabase
import com.example.data.local.LibraryEntry
import com.example.data.model.AndroidChassisStyle
import com.example.data.model.HardwareTelemetry
import com.example.data.model.ThermalTier
import com.example.data.telemetry.DeviceProfileProvider
import com.example.data.telemetry.SessionAlertEvaluator
import com.example.data.telemetry.SessionAlertState
import com.example.data.telemetry.SessionAlertType
import com.example.data.telemetry.TelemetryMonitor
import com.example.data.telemetry.ThermalPerformanceZone
import com.example.data.telemetry.ThermalTuningConfig
import com.example.ui.components.pulseCyclesPerSecond
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DroidDeck Hub", appName)
    }

    @Test
    fun `room dao persists pinned and favorite entries`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, DroidDeckDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = db.libraryDao()
            val entry = LibraryEntry(
                packageName = "com.demo.game",
                isPinned = true,
                isFavorite = true,
                launchCount = 3
            )
            dao.upsert(entry)

            val entries = dao.observeAllEntries().first()
            assertEquals(1, entries.size)
            assertEquals("com.demo.game", entries.first().packageName)
            assertTrue(entries.first().isPinned)
            assertTrue(entries.first().isFavorite)
            assertEquals(3, entries.first().launchCount)
        } finally {
            db.close()
        }
    }

    @Test
    fun `telemetry monitor samples valid system snapshot`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val monitor = TelemetryMonitor(context)
        val snapshot = monitor.sample()

        assertTrue(snapshot.freeRamMb >= 0L)
        assertTrue(snapshot.batteryPercent in 0..100)
        assertTrue(snapshot.cpuLoadPercent in 0..100)
        assertTrue(snapshot.lastUpdatedAt > 0L)
    }

    @Test
    fun `session alert evaluator triggers thermal battery and ram alerts with cooldown`() {
        val critical = HardwareTelemetry(
            freeRamMb = 320L,
            totalRamMb = 4096L,
            batteryPercent = 18,
            isCharging = false,
            batteryTempCelsius = 41.5f,
            thermalTier = ThermalTier.WARM
        )

        val firstEval = SessionAlertEvaluator.evaluate(
            telemetry = critical,
            currentState = SessionAlertState(),
            nowMs = 1_000_000L
        )
        assertEquals(
            listOf(SessionAlertType.THERMAL, SessionAlertType.LOW_BATTERY, SessionAlertType.LOW_RAM),
            firstEval.triggeredAlerts
        )

        // Within cooldown window -> no duplicate alerts
        val repeatEval = SessionAlertEvaluator.evaluate(
            telemetry = critical,
            currentState = firstEval.nextState,
            nowMs = 1_010_000L
        )
        assertTrue(repeatEval.triggeredAlerts.isEmpty())

        // After recovery -> alert flags reset cleanly
        val recovered = HardwareTelemetry(
            freeRamMb = 1600L,
            totalRamMb = 4096L,
            batteryPercent = 45,
            isCharging = true,
            batteryTempCelsius = 31.0f,
            thermalTier = ThermalTier.COOL
        )
        val recoveredEval = SessionAlertEvaluator.evaluate(
            telemetry = recovered,
            currentState = repeatEval.nextState,
            nowMs = 1_020_000L
        )
        assertTrue(recoveredEval.triggeredAlerts.isEmpty())
        assertFalse(recoveredEval.nextState.thermalAlerted)
        assertFalse(recoveredEval.nextState.batteryAlerted)
        assertFalse(recoveredEval.nextState.ramAlerted)
    }

    @Test
    fun `device profile provider formats model names without duplicate brand`() {
        assertEquals("Pixel 9", DeviceProfileProvider.formatDeviceDisplayName("Google", "Pixel 9"))
        assertEquals("Samsung SM-S928U", DeviceProfileProvider.formatDeviceDisplayName("samsung", "SM-S928U"))
        assertEquals("Samsung Galaxy S24", DeviceProfileProvider.formatDeviceDisplayName("Samsung", "Samsung Galaxy S24"))
        assertEquals("Samsung Galaxy S24", DeviceProfileProvider.formatDeviceDisplayName("samsung", "Galaxy S24"))
        assertEquals("Motorola Edge", DeviceProfileProvider.formatDeviceDisplayName("motorola", "Motorola Edge"))

        val context = ApplicationProvider.getApplicationContext<Context>()
        val profile = DeviceProfileProvider(context).read()
        assertTrue(profile.displayName.isNotBlank())
        assertTrue(profile.cpuCores >= 1)
        assertTrue(profile.refreshRateHz >= 30)
        assertTrue(profile.screenAspectRatio in 1.1f..2.45f)
        assertTrue(profile.cornerRoundness in 0.03f..0.22f)

        assertEquals(
            AndroidChassisStyle.PIXEL_VISOR,
            DeviceProfileProvider.detectChassisStyle("Google", "Pixel 9 Pro", isTablet = false, isFoldable = false)
        )
        assertEquals(
            AndroidChassisStyle.GALAXY_RINGS,
            DeviceProfileProvider.detectChassisStyle("samsung", "SM-S928U", isTablet = false, isFoldable = false)
        )
        assertEquals(
            AndroidChassisStyle.CIRCULAR_MODULE,
            DeviceProfileProvider.detectChassisStyle("OnePlus", "CPH2583", isTablet = false, isFoldable = false)
        )
        assertEquals(
            AndroidChassisStyle.NOTHING_MINIMAL,
            DeviceProfileProvider.detectChassisStyle("Nothing", "Phone (2)", isTablet = false, isFoldable = false)
        )
        assertEquals(
            AndroidChassisStyle.FOLDABLE,
            DeviceProfileProvider.detectChassisStyle("samsung", "Galaxy Z Fold6", isTablet = false, isFoldable = true)
        )
        assertEquals(
            AndroidChassisStyle.TABLET,
            DeviceProfileProvider.detectChassisStyle("samsung", "Galaxy Tab S9", isTablet = true, isFoldable = false)
        )
    }

    @Test
    fun `thermal tuning config maps zones and respects custom alert threshold`() {
        assertEquals(ThermalPerformanceZone.COOL_ECO, ThermalTuningConfig.zoneForCelsius(35))
        assertEquals(ThermalPerformanceZone.BALANCED, ThermalTuningConfig.zoneForCelsius(39))
        assertEquals(ThermalPerformanceZone.PERFORMANCE, ThermalTuningConfig.zoneForCelsius(42))
        assertEquals(ThermalPerformanceZone.PEAK_WATCH, ThermalTuningConfig.zoneForCelsius(44))

        assertEquals("39°C", ThermalTuningConfig.formatTemp(39f, useFahrenheit = false, decimals = 0))
        assertEquals("102°F", ThermalTuningConfig.formatTemp(39f, useFahrenheit = true, decimals = 0))

        // At 37°C: triggers when user limit is 36°C (Cool & Eco), stays quiet when limit is 42°C (Performance)
        val warmSample = HardwareTelemetry(
            freeRamMb = 1800L,
            totalRamMb = 4096L,
            batteryPercent = 70,
            isCharging = false,
            batteryTempCelsius = 37.2f,
            thermalTier = ThermalTier.NOMINAL
        )
        val ecoEval = SessionAlertEvaluator.evaluate(
            telemetry = warmSample,
            currentState = SessionAlertState(),
            thermalLimitCelsius = 36f
        )
        assertEquals(listOf(SessionAlertType.THERMAL), ecoEval.triggeredAlerts)

        val perfEval = SessionAlertEvaluator.evaluate(
            telemetry = warmSample,
            currentState = SessionAlertState(),
            thermalLimitCelsius = 42f
        )
        assertTrue(perfEval.triggeredAlerts.isEmpty())
    }

    @Test
    fun `hybrid cpu load and border pulse speed scale monotonically with load`() {
        val idleLoad = TelemetryMonitor.computeHybridCpuLoadPercent(
            cpufreqFraction = 0.10f,
            processCpuFraction = 0.04f,
            ramUsedFraction = 0.30f
        )
        val heavyLoad = TelemetryMonitor.computeHybridCpuLoadPercent(
            cpufreqFraction = 0.90f,
            processCpuFraction = 0.70f,
            ramUsedFraction = 0.80f
        )
        assertTrue(idleLoad in 0..30)
        assertTrue(heavyLoad in 70..100)
        assertTrue(heavyLoad > idleLoad)

        // Fallback when cpufreq sysfs is unreadable on device
        val fallbackLoad = TelemetryMonitor.computeHybridCpuLoadPercent(
            cpufreqFraction = null,
            processCpuFraction = 0.60f,
            ramUsedFraction = 0.50f
        )
        assertEquals(56, fallbackLoad)

        val idleHz = pulseCyclesPerSecond(0.0f)
        val midHz = pulseCyclesPerSecond(0.5f)
        val peakHz = pulseCyclesPerSecond(1.0f)
        assertTrue(idleHz < midHz)
        assertTrue(midHz < peakHz)
    }
}
