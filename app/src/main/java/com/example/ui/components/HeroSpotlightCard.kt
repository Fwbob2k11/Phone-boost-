package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DeckItem
import com.example.data.model.DeviceProfile
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.TelemetryLabelStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HeroSpotlightCard(
    item: DeckItem,
    cpuLoadPercent: Int = 0,
    cpuLoadFraction: Float = 0f,
    onLaunchClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cyan = MaterialTheme.colorScheme.primary
    val accent = resolveAccentColor(item.accentColorArgb, cyan)
    val launchInteraction = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(212.dp)
            .neonCyanPulseBorder(
                cpuLoadFraction = cpuLoadFraction,
                cornerRadius = 20.dp,
                baseStrokeWidth = 1.2.dp,
                glowColor = cyan
            ),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.img_deck_hero_bg),
                contentDescription = stringResource(R.string.cd_hero_background),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.62f)
                            )
                        )
                    )
                    .iconTintedGlow(accentColor = accent, isSpotlighted = true)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    FlowRow(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                text = stringResource(
                                    if (item.lastLaunchedAt > 0L) {
                                        R.string.hero_badge_recent
                                    } else {
                                        R.string.hero_badge_spotlight
                                    }
                                ),
                                style = TelemetryLabelStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        if (item.isPinned) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PushPin,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.badge_pinned),
                                        style = TelemetryLabelStyle,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        CpuLoadIntensityBadge(
                            cpuLoadPercent = cpuLoadPercent,
                            modifier = Modifier.testTag("hero_cpu_load_badge")
                        )
                    }

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("hero_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (item.isFavorite) {
                                Icons.Rounded.Star
                            } else {
                                Icons.Rounded.StarOutline
                            },
                            contentDescription = stringResource(R.string.cd_favorite_toggle),
                            tint = if (item.isFavorite) {
                                AmberWarm
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        AppIconBadge(
                            iconBitmap = item.iconBitmap,
                            sizeDp = 56,
                            borderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = buildString {
                                    append(if (item.isDetectedGame) "Game" else "Pinned App")
                                    if (item.launchCount > 0) {
                                        append(" · ${item.launchCount}× opened")
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Button(
                        onClick = onLaunchClick,
                        interactionSource = launchInteraction,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = cyan,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .springPressScale(launchInteraction)
                            .testTag("hero_launch_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.hero_launch_cta),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyHeroDeckCard(
    deviceProfile: DeviceProfile,
    cpuLoadPercent: Int = 0,
    cpuLoadFraction: Float = 0f,
    onPinAppsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cyan = MaterialTheme.colorScheme.primary
    val pinInteraction = remember { MutableInteractionSource() }
    val bannerRes = if (deviceProfile.isTablet) {
        R.drawable.img_device_tablet_hero
    } else {
        R.drawable.img_device_phone_hero
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .neonCyanPulseBorder(
                cpuLoadFraction = cpuLoadFraction,
                cornerRadius = 20.dp,
                baseStrokeWidth = 1.2.dp,
                glowColor = cyan
            )
            .testTag("empty_device_ready_card"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Device hero banner fading smoothly into matte graphite
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(164.dp)
            ) {
                Image(
                    painter = painterResource(id = bannerRes),
                    contentDescription = stringResource(
                        R.string.cd_device_hero_banner,
                        deviceProfile.displayName
                    ),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.25f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                )
                CpuLoadIntensityBadge(
                    cpuLoadPercent = cpuLoadPercent,
                    darkBackdrop = true,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                        .testTag("ready_cpu_load_badge")
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DeviceIdentityShowcaseRow(
                    deviceProfile = deviceProfile,
                    showReadyTitle = true
                )

                Text(
                    text = stringResource(R.string.hero_empty_subtitle, deviceProfile.displayName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                DeviceSpecChipsRow(deviceProfile = deviceProfile)

                Button(
                    onClick = onPinAppsClick,
                    interactionSource = pinInteraction,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cyan,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .springPressScale(pinInteraction)
                        .testTag("empty_pin_apps_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.hero_select_cta),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceIdentityShowcaseRow(
    deviceProfile: DeviceProfile,
    showReadyTitle: Boolean,
    modifier: Modifier = Modifier
) {
    val cyan = MaterialTheme.colorScheme.primary
    val iconDescription = stringResource(R.string.cd_device_silhouette, deviceProfile.displayName)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.65f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .size(68.dp)
                .semantics { contentDescription = iconDescription }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                DeviceHardwareIcon(
                    deviceProfile = deviceProfile,
                    size = 54.dp
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (showReadyTitle) {
                    stringResource(R.string.hero_empty_title, deviceProfile.displayName)
                } else {
                    deviceProfile.displayName
                },
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (deviceProfile.androidVersion.isNotBlank()) {
                    Text(
                        text = deviceProfile.androidVersion,
                        style = TelemetryLabelStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (deviceProfile.totalRamGb > 0.0) {
                    Text(
                        text = "·",
                        style = TelemetryLabelStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.device_profile_ram_gb, deviceProfile.totalRamGb),
                        style = TelemetryLabelStyle,
                        color = cyan
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeviceSpecChipsRow(
    deviceProfile: DeviceProfile,
    modifier: Modifier = Modifier
) {
    val neutralTint = MaterialTheme.colorScheme.onSurfaceVariant

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (deviceProfile.androidVersion.isNotBlank()) {
            DeviceSpecChip(
                text = deviceProfile.androidVersion,
                accentColor = MaterialTheme.colorScheme.primary
            )
        }
        if (deviceProfile.totalRamGb > 0.0) {
            DeviceSpecChip(
                text = stringResource(R.string.device_profile_ram_gb, deviceProfile.totalRamGb),
                accentColor = neutralTint
            )
        }
        if (deviceProfile.cpuAbi.isNotBlank() && deviceProfile.cpuCores > 0) {
            DeviceSpecChip(
                text = stringResource(
                    R.string.device_profile_cores_abi,
                    deviceProfile.cpuCores,
                    deviceProfile.cpuAbi
                ),
                icon = Icons.Rounded.Memory,
                accentColor = neutralTint
            )
        }
        DeviceSpecChip(
            text = stringResource(R.string.device_profile_refresh_hz, deviceProfile.refreshRateHz),
            icon = Icons.Rounded.Speed,
            accentColor = neutralTint
        )
    }
}

@Composable
private fun DeviceSpecChip(
    text: String,
    accentColor: Color,
    icon: ImageVector? = null
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = text,
                style = TelemetryLabelStyle,
                color = accentColor
            )
        }
    }
}

@Composable
fun AppIconBadge(
    iconBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    sizeDp: Int,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(sizeDp.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.7f))
    ) {
        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Apps,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size((sizeDp / 2).dp)
                )
            }
        }
    }
}

@Composable
fun CpuLoadIntensityBadge(
    cpuLoadPercent: Int,
    darkBackdrop: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cyan = MaterialTheme.colorScheme.primary
    val clamped = cpuLoadPercent.coerceIn(0, 100)
    val (tierLabel, badgeColor) = when {
        clamped < 30 -> stringResource(R.string.cpu_tier_calm) to cyan
        clamped < 60 -> stringResource(R.string.cpu_tier_active) to EmeraldPulse
        clamped < 85 -> stringResource(R.string.cpu_tier_high) to AmberWarm
        else -> stringResource(R.string.cpu_tier_peak) to CoralAlert
    }
    val containerColor = if (darkBackdrop) {
        MaterialTheme.colorScheme.background.copy(alpha = 0.86f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Surface(
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.48f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            LivePulseBeacon(
                color = badgeColor,
                modifier = Modifier.size(8.dp)
            )
            Text(
                text = stringResource(R.string.cpu_load_badge, clamped, tierLabel),
                style = TelemetryLabelStyle,
                color = badgeColor
            )
        }
    }
}
