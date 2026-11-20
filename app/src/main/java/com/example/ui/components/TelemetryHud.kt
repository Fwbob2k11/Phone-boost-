package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DeviceProfile
import com.example.data.model.HardwareTelemetry
import com.example.data.model.ThermalTier
import com.example.data.telemetry.ThermalPerformanceZone
import com.example.data.telemetry.ThermalTuningConfig
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.TelemetryLabelStyle
import com.example.ui.theme.TelemetryValueStyle
import kotlin.math.roundToInt

@Composable
fun TopConsoleHud(
    isRefreshing: Boolean,
    onRefreshClick: () -> Unit,
    onAddPinClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cyan = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.GridView,
                        contentDescription = null,
                        tint = cyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.hud_brand_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    LivePulseBeacon(color = EmeraldPulse)
                    Text(
                        text = stringResource(R.string.telemetry_live_badge),
                        style = TelemetryLabelStyle,
                        color = EmeraldPulse
                    )
                    Text(
                        text = "·",
                        style = TelemetryLabelStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.hud_subtitle),
                        style = TelemetryLabelStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = onRefreshClick,
                enabled = !isRefreshing,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("refresh_telemetry_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.cd_refresh_telemetry),
                    tint = if (isRefreshing) cyan else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FilledTonalButton(
                onClick = onAddPinClick,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(44.dp)
                    .testTag("add_pin_apps_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.cd_pin_apps),
                    tint = cyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.action_add_to_deck),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun TelemetryStrip(
    telemetry: HardwareTelemetry,
    isSessionGuardEnabled: Boolean,
    hasNotificationPermission: Boolean,
    thermalLimitCelsius: Int,
    useFahrenheit: Boolean,
    onOpenDiagnostics: () -> Unit,
    onSessionGuardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cyan = MaterialTheme.colorScheme.primary
    val slate = MaterialTheme.colorScheme.secondary
    val thermalColor = thermalColor(telemetry.thermalTier)
    val thermalProgress = thermalProgress(telemetry.thermalTier)
    val batteryColor = when {
        telemetry.isCharging -> EmeraldPulse
        telemetry.batteryPercent <= 20 -> CoralAlert
        else -> cyan
    }
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .springPressScale(interactionSource)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onOpenDiagnostics
            )
            .testTag("telemetry_strip"),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TelemetryArcTile(
                    icon = Icons.Rounded.Memory,
                    label = stringResource(R.string.telemetry_ram_label),
                    value = stringResource(R.string.telemetry_mb_unit, telemetry.freeRamMb),
                    progress = telemetry.ramFreeFraction,
                    accentColor = cyan,
                    secondaryColor = slate,
                    modifier = Modifier.weight(1f)
                )
                TelemetryArcTile(
                    icon = if (telemetry.isCharging) {
                        Icons.Rounded.BatteryChargingFull
                    } else {
                        Icons.Rounded.BatteryFull
                    },
                    label = stringResource(R.string.telemetry_battery_label),
                    value = stringResource(R.string.telemetry_percent_unit, telemetry.batteryPercent),
                    progress = (telemetry.batteryPercent / 100f).coerceIn(0f, 1f),
                    accentColor = batteryColor,
                    secondaryColor = slate,
                    modifier = Modifier.weight(1f)
                )
                TelemetryArcTile(
                    icon = Icons.Rounded.Thermostat,
                    label = stringResource(R.string.telemetry_thermal_label),
                    value = telemetry.thermalTier.label,
                    progress = thermalProgress,
                    accentColor = thermalColor,
                    secondaryColor = slate,
                    modifier = Modifier.weight(1f)
                )
                TelemetryArcTile(
                    icon = Icons.Rounded.SdStorage,
                    label = stringResource(R.string.telemetry_storage_label),
                    value = stringResource(R.string.telemetry_gb_unit, telemetry.freeStorageGb),
                    progress = 1f - telemetry.storageUsedFraction,
                    accentColor = EmeraldPulse,
                    secondaryColor = slate,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = stringResource(R.string.cd_open_diagnostics),
                    tint = cyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Compact live RGB LED headroom bar across the telemetry strip
            SegmentedLedBar(
                progress = telemetry.ramFreeFraction,
                segmentCount = 18,
                height = 4.dp,
                invertHeatColors = false
            )

            // Session Guard toggle pill for background game-session alerts
            val guardActive = isSessionGuardEnabled && hasNotificationPermission
            val guardColor = when {
                !hasNotificationPermission -> AmberWarm
                guardActive -> EmeraldPulse
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            val guardLabel = when {
                !hasNotificationPermission -> stringResource(R.string.session_guard_needs_perm)
                isSessionGuardEnabled -> stringResource(R.string.session_guard_armed)
                else -> stringResource(R.string.session_guard_off)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = guardColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, guardColor.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onSessionGuardClick)
                        .testTag("session_guard_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (guardActive) {
                            LivePulseBeacon(color = guardColor, modifier = Modifier.size(10.dp))
                        }
                        Text(
                            text = guardLabel,
                            style = TelemetryLabelStyle,
                            color = guardColor
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = zoneColor(ThermalTuningConfig.zoneForCelsius(thermalLimitCelsius)).copy(alpha = 0.14f),
                    border = BorderStroke(
                        1.dp,
                        zoneColor(ThermalTuningConfig.zoneForCelsius(thermalLimitCelsius)).copy(alpha = 0.55f)
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenDiagnostics)
                        .testTag("hud_thermal_limit_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Thermostat,
                            contentDescription = null,
                            tint = zoneColor(ThermalTuningConfig.zoneForCelsius(thermalLimitCelsius)),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = stringResource(
                                R.string.hud_thermal_target_pill,
                                ThermalTuningConfig.formatTemp(
                                    thermalLimitCelsius.toFloat(),
                                    useFahrenheit,
                                    decimals = 0
                                )
                            ),
                            style = TelemetryLabelStyle,
                            color = zoneColor(ThermalTuningConfig.zoneForCelsius(thermalLimitCelsius))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryArcTile(
    icon: ImageVector,
    label: String,
    value: String,
    progress: Float,
    accentColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        RadialTelemetryGauge(
            progress = progress,
            accentColor = accentColor,
            secondaryColor = secondaryColor,
            size = 36.dp,
            strokeWidth = 3.5.dp
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(15.dp)
            )
        }
        Text(
            text = value,
            style = TelemetryValueStyle.copy(fontSize = MaterialTheme.typography.labelLarge.fontSize),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            style = TelemetryLabelStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsBottomSheet(
    deviceProfile: DeviceProfile,
    telemetry: HardwareTelemetry,
    thermalLimitCelsius: Int,
    useFahrenheit: Boolean,
    isRefreshing: Boolean,
    onThermalLimitChange: (Int) -> Unit,
    onToggleUnit: () -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cyan = MaterialTheme.colorScheme.primary
    val slate = MaterialTheme.colorScheme.secondary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LivePulseBeacon(color = EmeraldPulse)
                        Text(
                            text = stringResource(R.string.diagnostics_title),
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                    Text(
                        text = stringResource(R.string.diagnostics_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onRefresh,
                    enabled = !isRefreshing,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("diagnostics_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.diagnostics_refresh))
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.72f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DeviceIdentityShowcaseRow(
                        deviceProfile = deviceProfile,
                        showReadyTitle = false
                    )
                    DeviceSpecChipsRow(deviceProfile = deviceProfile)
                }
            }

            ThermalPerformanceTunerCard(
                thermalLimitCelsius = thermalLimitCelsius,
                currentTempCelsius = telemetry.batteryTempCelsius,
                useFahrenheit = useFahrenheit,
                onThermalLimitChange = onThermalLimitChange,
                onToggleUnit = onToggleUnit
            )

            val ramFreePct = (telemetry.ramFreeFraction * 100f).toInt()
            DiagnosticMeterCard(
                icon = Icons.Rounded.Memory,
                title = stringResource(R.string.diagnostics_ram_title),
                subtitle = stringResource(
                    R.string.diagnostics_ram_detail,
                    telemetry.freeRamMb,
                    telemetry.totalRamMb,
                    ramFreePct
                ),
                badgeText = "${ramFreePct}%",
                progress = telemetry.ramUsedFraction,
                accentColor = cyan,
                secondaryColor = slate,
                invertHeatColors = true
            )

            val currentTempText = ThermalTuningConfig.formatTemp(
                telemetry.batteryTempCelsius,
                useFahrenheit,
                decimals = 1
            )
            val batterySubtitle = if (telemetry.isCharging) {
                "Charging · ${telemetry.batteryPercent}% · $currentTempText"
            } else {
                "On Battery · ${telemetry.batteryPercent}% · $currentTempText"
            }
            DiagnosticMeterCard(
                icon = if (telemetry.isCharging) {
                    Icons.Rounded.BatteryChargingFull
                } else {
                    Icons.Rounded.BatteryFull
                },
                title = stringResource(R.string.diagnostics_battery_title),
                subtitle = batterySubtitle,
                badgeText = "${telemetry.batteryPercent}%",
                progress = (telemetry.batteryPercent / 100f).coerceIn(0f, 1f),
                accentColor = if (telemetry.isCharging) EmeraldPulse else cyan,
                secondaryColor = slate,
                invertHeatColors = false
            )

            val thermalColor = thermalColor(telemetry.thermalTier)
            val thermalProgress = thermalProgress(telemetry.thermalTier)
            DiagnosticMeterCard(
                icon = Icons.Rounded.Thermostat,
                title = stringResource(R.string.diagnostics_thermal_title),
                subtitle = "${telemetry.thermalTier.label} · $currentTempText",
                badgeText = telemetry.thermalTier.label,
                progress = thermalProgress,
                accentColor = thermalColor,
                secondaryColor = slate,
                invertHeatColors = true
            )

            val storageFreePct = ((1f - telemetry.storageUsedFraction) * 100f).toInt()
            DiagnosticMeterCard(
                icon = Icons.Rounded.SdStorage,
                title = stringResource(R.string.diagnostics_storage_title),
                subtitle = stringResource(
                    R.string.diagnostics_storage_detail,
                    telemetry.freeStorageGb,
                    telemetry.totalStorageGb
                ),
                badgeText = "${storageFreePct}%",
                progress = telemetry.storageUsedFraction,
                accentColor = EmeraldPulse,
                secondaryColor = slate,
                invertHeatColors = true
            )
        }
    }
}

@Composable
private fun DiagnosticMeterCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String,
    progress: Float,
    accentColor: Color,
    secondaryColor: Color,
    invertHeatColors: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.72f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RadialTelemetryGauge(
                    progress = progress,
                    accentColor = accentColor,
                    secondaryColor = secondaryColor,
                    size = 48.dp,
                    strokeWidth = 4.5.dp
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        style = TelemetryValueStyle.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = MaterialTheme.typography.bodySmall.fontSize
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = accentColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = badgeText,
                        style = TelemetryLabelStyle,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            SegmentedLedBar(
                progress = progress,
                segmentCount = 14,
                height = 7.dp,
                invertHeatColors = invertHeatColors
            )
        }
    }
}

@Composable
private fun ThermalPerformanceTunerCard(
    thermalLimitCelsius: Int,
    currentTempCelsius: Float,
    useFahrenheit: Boolean,
    onThermalLimitChange: (Int) -> Unit,
    onToggleUnit: () -> Unit
) {
    val zone = ThermalTuningConfig.zoneForCelsius(thermalLimitCelsius)
    val accent = zoneColor(zone)
    val targetLabel = ThermalTuningConfig.formatTemp(
        thermalLimitCelsius.toFloat(),
        useFahrenheit,
        decimals = 0
    )
    val minLabel = ThermalTuningConfig.formatTemp(
        ThermalTuningConfig.MIN_TEMP_C.toFloat(),
        useFahrenheit,
        decimals = 0
    )
    val midLabel = ThermalTuningConfig.formatTemp(
        ThermalTuningConfig.DEFAULT_TEMP_C.toFloat(),
        useFahrenheit,
        decimals = 0
    )
    val maxLabel = ThermalTuningConfig.formatTemp(
        ThermalTuningConfig.MAX_TEMP_C.toFloat(),
        useFahrenheit,
        decimals = 0
    )

    val uiSec = "${zone.foregroundSampleMs / 1000f}s".removeSuffix(".0s").let {
        if (it.endsWith("s")) it else "${it}s"
    }
    val bgSec = "${zone.backgroundSampleMs / 1000}s"

    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.78f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.2.dp, accent.copy(alpha = 0.65f)),
        modifier = Modifier.testTag("thermal_tuner_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.tuner_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.tuner_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // °C / °F switchable pill
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onToggleUnit)
                            .testTag("temp_unit_toggle")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.tuner_unit_c),
                                style = TelemetryLabelStyle,
                                color = if (!useFahrenheit) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Text(
                                text = "|",
                                style = TelemetryLabelStyle,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = stringResource(R.string.tuner_unit_f),
                                style = TelemetryLabelStyle,
                                color = if (useFahrenheit) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }

                    Surface(
                        color = accent.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.7f))
                    ) {
                        Text(
                            text = targetLabel,
                            style = TelemetryValueStyle,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Slider(
                value = thermalLimitCelsius.toFloat(),
                onValueChange = { onThermalLimitChange(it.roundToInt()) },
                valueRange = ThermalTuningConfig.MIN_TEMP_C.toFloat()..ThermalTuningConfig.MAX_TEMP_C.toFloat(),
                steps = ThermalTuningConfig.MAX_TEMP_C - ThermalTuningConfig.MIN_TEMP_C - 1,
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    activeTickColor = MaterialTheme.colorScheme.background,
                    inactiveTickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("thermal_performance_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = minLabel, style = TelemetryLabelStyle, color = MaterialTheme.colorScheme.primary)
                Text(text = midLabel, style = TelemetryLabelStyle, color = EmeraldPulse)
                Text(text = maxLabel, style = TelemetryLabelStyle, color = CoralAlert)
            }

            if (currentTempCelsius > 0.1f) {
                val deltaC = thermalLimitCelsius - currentTempCelsius
                val nowFormatted = ThermalTuningConfig.formatTemp(currentTempCelsius, useFahrenheit, 1)
                val headroomText = if (deltaC > 0f) {
                    stringResource(
                        R.string.tuner_headroom_ok,
                        nowFormatted,
                        ThermalTuningConfig.formatDelta(deltaC, useFahrenheit)
                    )
                } else {
                    stringResource(R.string.tuner_headroom_hot, nowFormatted)
                }
                Text(
                    text = headroomText,
                    style = TelemetryLabelStyle,
                    color = if (deltaC > 0f) EmeraldPulse else CoralAlert
                )
            }

            // Live guidance explaining what this temperature setting does to help the phone
            Surface(
                color = accent.copy(alpha = 0.10f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("thermal_benefit_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = accent.copy(alpha = 0.20f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = zone.badge,
                                style = TelemetryLabelStyle,
                                color = accent,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.tuner_sampling_badge, uiSec, bgSec),
                            style = TelemetryLabelStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = zone.phoneBenefitTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = zone.phoneBenefitBody,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.tuner_os_note),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun zoneColor(zone: ThermalPerformanceZone): Color {
    return when (zone) {
        ThermalPerformanceZone.COOL_ECO -> MaterialTheme.colorScheme.primary
        ThermalPerformanceZone.BALANCED -> EmeraldPulse
        ThermalPerformanceZone.PERFORMANCE -> AmberWarm
        ThermalPerformanceZone.PEAK_WATCH -> CoralAlert
    }
}

@Composable
private fun thermalColor(tier: ThermalTier): Color {
    return when (tier) {
        ThermalTier.COOL -> MaterialTheme.colorScheme.primary
        ThermalTier.NOMINAL -> EmeraldPulse
        ThermalTier.WARM -> AmberWarm
        ThermalTier.HOT -> CoralAlert
    }
}

private fun thermalProgress(tier: ThermalTier): Float {
    return when (tier) {
        ThermalTier.COOL -> 0.25f
        ThermalTier.NOMINAL -> 0.48f
        ThermalTier.WARM -> 0.74f
        ThermalTier.HOT -> 0.96f
    }
}
