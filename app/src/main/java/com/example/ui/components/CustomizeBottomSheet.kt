package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.example.data.model.DeckAccentTheme
import com.example.ui.theme.TelemetryLabelStyle
import com.example.ui.theme.accentSwatchColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeBottomSheet(
    accentTheme: DeckAccentTheme,
    showRamGauge: Boolean,
    showBatteryGauge: Boolean,
    showThermalGauge: Boolean,
    showStorageGauge: Boolean,
    thermalLimitCelsius: Int,
    currentTempCelsius: Float,
    useFahrenheit: Boolean,
    onAccentThemeSelected: (DeckAccentTheme) -> Unit,
    onToggleRamGauge: (Boolean) -> Unit,
    onToggleBatteryGauge: (Boolean) -> Unit,
    onToggleThermalGauge: (Boolean) -> Unit,
    onToggleStorageGauge: (Boolean) -> Unit,
    onThermalLimitChange: (Int) -> Unit,
    onToggleUnit: () -> Unit,
    onResetDefaults: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accent = MaterialTheme.colorScheme.primary

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
                .padding(bottom = 24.dp)
                .testTag("customize_bottom_sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = accent.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.customize_title),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = stringResource(R.string.customize_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(
                    onClick = onResetDefaults,
                    modifier = Modifier.testTag("customize_reset_button")
                ) {
                    Text(stringResource(R.string.customize_reset))
                }
            }

            // 1) Accent Color Theme
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.customize_section_theme),
                    style = TelemetryLabelStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val themes = DeckAccentTheme.entries
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (rowIndex in themes.indices step 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val first = themes[rowIndex]
                            AccentThemeOptionCard(
                                theme = first,
                                selected = accentTheme == first,
                                onClick = { onAccentThemeSelected(first) },
                                modifier = Modifier.weight(1f)
                            )
                            if (rowIndex + 1 < themes.size) {
                                val second = themes[rowIndex + 1]
                                AccentThemeOptionCard(
                                    theme = second,
                                    selected = accentTheme == second,
                                    onClick = { onAccentThemeSelected(second) },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // 2) System Gauges Visibility
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.customize_section_gauges),
                    style = TelemetryLabelStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        GaugeToggleRow(
                            icon = Icons.Rounded.Memory,
                            title = stringResource(R.string.telemetry_ram_label),
                            subtitle = stringResource(R.string.customize_gauge_ram_desc),
                            checked = showRamGauge,
                            onCheckedChange = onToggleRamGauge,
                            testTag = "gauge_toggle_ram"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                        GaugeToggleRow(
                            icon = Icons.Rounded.BatteryFull,
                            title = stringResource(R.string.telemetry_battery_label),
                            subtitle = stringResource(R.string.customize_gauge_battery_desc),
                            checked = showBatteryGauge,
                            onCheckedChange = onToggleBatteryGauge,
                            testTag = "gauge_toggle_battery"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                        GaugeToggleRow(
                            icon = Icons.Rounded.Thermostat,
                            title = stringResource(R.string.telemetry_thermal_label),
                            subtitle = stringResource(R.string.customize_gauge_thermal_desc),
                            checked = showThermalGauge,
                            onCheckedChange = onToggleThermalGauge,
                            testTag = "gauge_toggle_thermal"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                        GaugeToggleRow(
                            icon = Icons.Rounded.SdStorage,
                            title = stringResource(R.string.telemetry_storage_label),
                            subtitle = stringResource(R.string.customize_gauge_storage_desc),
                            checked = showStorageGauge,
                            onCheckedChange = onToggleStorageGauge,
                            testTag = "gauge_toggle_storage"
                        )
                    }
                }
            }

            // 3) Target Thermal Alert Temperature Slider (35°C – 45°C)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.customize_section_thermal),
                    style = TelemetryLabelStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ThermalPerformanceTunerCard(
                    thermalLimitCelsius = thermalLimitCelsius,
                    currentTempCelsius = currentTempCelsius,
                    useFahrenheit = useFahrenheit,
                    onThermalLimitChange = onThermalLimitChange,
                    onToggleUnit = onToggleUnit,
                    sliderTestTag = "customize_thermal_slider"
                )
            }
        }
    }
}

@Composable
private fun AccentThemeOptionCard(
    theme: DeckAccentTheme,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val swatch = accentSwatchColor(theme)
    val label = when (theme) {
        DeckAccentTheme.EMERALD_GREEN -> stringResource(R.string.customize_theme_emerald)
        DeckAccentTheme.CYBER_CYAN -> stringResource(R.string.customize_theme_cyan)
        DeckAccentTheme.NEON_PURPLE -> stringResource(R.string.customize_theme_purple)
        DeckAccentTheme.CRIMSON_RED -> stringResource(R.string.customize_theme_crimson)
    }

    Surface(
        color = if (selected) {
            swatch.copy(alpha = 0.14f)
        } else {
            MaterialTheme.colorScheme.background.copy(alpha = 0.72f)
        },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) swatch else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("accent_theme_${theme.storageKey}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(swatch),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color(0xFF061014),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GaugeToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    val accent = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = if (checked) accent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = accent
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
