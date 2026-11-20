package com.example.data.telemetry

import com.example.data.model.HardwareTelemetry
import com.example.data.model.ThermalTier

enum class SessionAlertType {
    THERMAL,
    LOW_BATTERY,
    LOW_RAM
}

data class SessionAlertState(
    val thermalAlerted: Boolean = false,
    val batteryAlerted: Boolean = false,
    val ramAlerted: Boolean = false,
    val lastThermalAlertAt: Long = 0L,
    val lastBatteryAlertAt: Long = 0L,
    val lastRamAlertAt: Long = 0L
)

data class SessionAlertEvaluation(
    val triggeredAlerts: List<SessionAlertType>,
    val nextState: SessionAlertState
)

object SessionAlertEvaluator {

    const val LOW_BATTERY_THRESHOLD_PCT = 20
    const val LOW_BATTERY_RECOVERY_PCT = 25
    const val LOW_RAM_THRESHOLD_FRACTION = 0.12f
    const val LOW_RAM_THRESHOLD_MB = 450L
    const val LOW_RAM_RECOVERY_FRACTION = 0.16f
    const val LOW_RAM_RECOVERY_MB = 550L
    const val ALERT_COOLDOWN_MS = 300_000L
    const val THERMAL_HYSTERESIS_C = 1.5f

    fun evaluate(
        telemetry: HardwareTelemetry,
        currentState: SessionAlertState,
        thermalLimitCelsius: Float = ThermalTuningConfig.DEFAULT_TEMP_C.toFloat(),
        nowMs: Long = System.currentTimeMillis()
    ): SessionAlertEvaluation {
        val triggered = mutableListOf<SessionAlertType>()

        val hasTempSensor = telemetry.batteryTempCelsius > 0.1f
        val isThermalHigh = if (hasTempSensor) {
            telemetry.batteryTempCelsius >= thermalLimitCelsius ||
                telemetry.thermalTier == ThermalTier.HOT
        } else {
            telemetry.thermalTier == ThermalTier.WARM || telemetry.thermalTier == ThermalTier.HOT
        }

        val isThermalRecovered = if (hasTempSensor) {
            telemetry.batteryTempCelsius <= (thermalLimitCelsius - THERMAL_HYSTERESIS_C) &&
                telemetry.thermalTier != ThermalTier.HOT
        } else {
            telemetry.thermalTier == ThermalTier.COOL || telemetry.thermalTier == ThermalTier.NOMINAL
        }

        var nextThermalAlerted = currentState.thermalAlerted
        var nextThermalTime = currentState.lastThermalAlertAt
        if (isThermalHigh) {
            val cooldownElapsed = nowMs - currentState.lastThermalAlertAt >= ALERT_COOLDOWN_MS
            if (!currentState.thermalAlerted || cooldownElapsed) {
                triggered.add(SessionAlertType.THERMAL)
                nextThermalAlerted = true
                nextThermalTime = nowMs
            }
        } else if (isThermalRecovered) {
            nextThermalAlerted = false
        }

        val isBatteryLow = !telemetry.isCharging && telemetry.batteryPercent in 1..LOW_BATTERY_THRESHOLD_PCT
        val isBatteryRecovered = telemetry.isCharging || telemetry.batteryPercent >= LOW_BATTERY_RECOVERY_PCT

        var nextBatteryAlerted = currentState.batteryAlerted
        var nextBatteryTime = currentState.lastBatteryAlertAt
        if (isBatteryLow) {
            val cooldownElapsed = nowMs - currentState.lastBatteryAlertAt >= ALERT_COOLDOWN_MS
            if (!currentState.batteryAlerted || cooldownElapsed) {
                triggered.add(SessionAlertType.LOW_BATTERY)
                nextBatteryAlerted = true
                nextBatteryTime = nowMs
            }
        } else if (isBatteryRecovered) {
            nextBatteryAlerted = false
        }

        val isRamLow = telemetry.totalRamMb > 0L && (
            telemetry.ramFreeFraction < LOW_RAM_THRESHOLD_FRACTION ||
                telemetry.freeRamMb < LOW_RAM_THRESHOLD_MB
            )
        val isRamRecovered = telemetry.totalRamMb > 0L &&
            telemetry.ramFreeFraction >= LOW_RAM_RECOVERY_FRACTION &&
            telemetry.freeRamMb >= LOW_RAM_RECOVERY_MB

        var nextRamAlerted = currentState.ramAlerted
        var nextRamTime = currentState.lastRamAlertAt
        if (isRamLow) {
            val cooldownElapsed = nowMs - currentState.lastRamAlertAt >= ALERT_COOLDOWN_MS
            if (!currentState.ramAlerted || cooldownElapsed) {
                triggered.add(SessionAlertType.LOW_RAM)
                nextRamAlerted = true
                nextRamTime = nowMs
            }
        } else if (isRamRecovered) {
            nextRamAlerted = false
        }

        return SessionAlertEvaluation(
            triggeredAlerts = triggered,
            nextState = SessionAlertState(
                thermalAlerted = nextThermalAlerted,
                batteryAlerted = nextBatteryAlerted,
                ramAlerted = nextRamAlerted,
                lastThermalAlertAt = nextThermalTime,
                lastBatteryAlertAt = nextBatteryTime,
                lastRamAlertAt = nextRamTime
            )
        )
    }
}
