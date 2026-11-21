package com.example.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.DroidDeckPreferencesStore
import com.example.data.model.HardwareTelemetry
import com.example.data.telemetry.SessionAlertEvaluator
import com.example.data.telemetry.SessionAlertState
import com.example.data.telemetry.SessionAlertType
import com.example.data.telemetry.TelemetryMonitor
import com.example.data.telemetry.ThermalTuningConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameSessionMonitorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null
    private lateinit var telemetryMonitor: TelemetryMonitor
    private lateinit var preferencesStore: DroidDeckPreferencesStore
    private var alertState = SessionAlertState()
    private var activeGameName: String = "Game Session"

    override fun onCreate() {
        super.onCreate()
        telemetryMonitor = TelemetryMonitor(applicationContext)
        preferencesStore = DroidDeckPreferencesStore(applicationContext)
        ensureNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_MONITOR) {
            stopMonitoringAndSelf()
            return START_NOT_STICKY
        }

        val gameName = intent?.getStringExtra(EXTRA_GAME_NAME)?.takeIf { it.isNotBlank() }
            ?: activeGameName
        activeGameName = gameName
        alertState = SessionAlertState()

        val initialNotification = buildOngoingNotification(
            gameName = activeGameName,
            telemetry = HardwareTelemetry()
        )
        startForegroundCompat(initialNotification)
        startMonitoringLoop()

        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        stopMonitoringAndSelf()
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundCompat(notification: Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID_ONGOING,
            notification,
            type
        )
    }

    private fun startMonitoringLoop() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            while (isActive) {
                val prefs = preferencesStore.preferencesFlow.first()
                val limitCelsius = prefs.thermalLimitCelsius
                val useFahrenheit = prefs.useFahrenheit
                val zone = ThermalTuningConfig.zoneForCelsius(limitCelsius)

                val snapshot = telemetryMonitor.sample()
                updateOngoingNotification(snapshot)

                val evaluation = SessionAlertEvaluator.evaluate(
                    telemetry = snapshot,
                    currentState = alertState,
                    thermalLimitCelsius = limitCelsius.toFloat()
                )
                alertState = evaluation.nextState
                for (alertType in evaluation.triggeredAlerts) {
                    postThresholdAlert(alertType, snapshot, limitCelsius, useFahrenheit)
                }

                delay(zone.backgroundSampleMs)
            }
        }
    }

    private fun updateOngoingNotification(telemetry: HardwareTelemetry) {
        if (!canPostNotifications()) return
        val notification = buildOngoingNotification(activeGameName, telemetry)
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID_ONGOING, notification)
    }

    private fun buildOngoingNotification(
        gameName: String,
        telemetry: HardwareTelemetry
    ): Notification {
        val returnIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val returnPendingIntent = PendingIntent.getActivity(
            this,
            0,
            returnIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, GameSessionMonitorService::class.java).apply {
            action = ACTION_STOP_MONITOR
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bodyText = getString(
            R.string.notif_monitor_body,
            telemetry.freeRamMb,
            telemetry.batteryPercent,
            telemetry.thermalTier.label
        )

        return NotificationCompat.Builder(this, CHANNEL_ONGOING)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(getString(R.string.notif_monitor_title, gameName))
            .setContentText(bodyText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(returnPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.notif_action_stop),
                stopPendingIntent
            )
            .build()
    }

    private fun postThresholdAlert(
        type: SessionAlertType,
        telemetry: HardwareTelemetry,
        limitCelsius: Int,
        useFahrenheit: Boolean
    ) {
        if (!canPostNotifications()) return

        val (notificationId, title, body) = when (type) {
            SessionAlertType.THERMAL -> {
                val currentTemp = ThermalTuningConfig.formatTemp(telemetry.batteryTempCelsius, useFahrenheit, 1)
                val targetTemp = ThermalTuningConfig.formatTemp(limitCelsius.toFloat(), useFahrenheit, 0)
                Triple(
                    NOTIFICATION_ID_ALERT_THERMAL,
                    getString(R.string.alert_thermal_title, telemetry.thermalTier.label),
                    getString(R.string.alert_thermal_target_body, activeGameName, currentTemp, targetTemp)
                )
            }
            SessionAlertType.LOW_BATTERY -> Triple(
                NOTIFICATION_ID_ALERT_BATTERY,
                getString(R.string.alert_battery_title, telemetry.batteryPercent),
                getString(R.string.alert_battery_body, telemetry.batteryPercent, activeGameName)
            )
            SessionAlertType.LOW_RAM -> {
                val freePct = (telemetry.ramFreeFraction * 100f).toInt()
                Triple(
                    NOTIFICATION_ID_ALERT_RAM,
                    getString(R.string.alert_ram_title, telemetry.freeRamMb),
                    getString(R.string.alert_ram_body, telemetry.freeRamMb, freePct, activeGameName)
                )
            }
        }

        val openDeckIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openDeckPendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            openDeckIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alertNotification = NotificationCompat.Builder(this, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openDeckPendingIntent)
            .build()

        NotificationManagerCompat.from(this).notify(notificationId, alertNotification)
    }

    private fun ensureNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return

        val monitorChannel = NotificationChannel(
            CHANNEL_ONGOING,
            getString(R.string.notif_channel_monitor_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notif_channel_monitor_desc)
            setShowBadge(false)
        }

        val alertsChannel = NotificationChannel(
            CHANNEL_ALERTS,
            getString(R.string.notif_channel_alerts_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.notif_channel_alerts_desc)
        }

        manager.createNotificationChannel(monitorChannel)
        manager.createNotificationChannel(alertsChannel)
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun stopMonitoringAndSelf() {
        monitorJob?.cancel()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        private const val CHANNEL_ONGOING = "droiddeck_session_monitor"
        private const val CHANNEL_ALERTS = "droiddeck_session_alerts"
        private const val NOTIFICATION_ID_ONGOING = 4001
        private const val NOTIFICATION_ID_ALERT_THERMAL = 4002
        private const val NOTIFICATION_ID_ALERT_BATTERY = 4003
        private const val NOTIFICATION_ID_ALERT_RAM = 4004

        const val ACTION_STOP_MONITOR = "com.example.action.STOP_GAME_SESSION_MONITOR"
        const val EXTRA_GAME_NAME = "extra_game_name"

        fun startSession(context: Context, gameName: String) {
            val intent = Intent(context, GameSessionMonitorService::class.java).apply {
                putExtra(EXTRA_GAME_NAME, gameName)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopSession(context: Context) {
            val intent = Intent(context, GameSessionMonitorService::class.java)
            context.stopService(intent)
        }
    }
}
