package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.AndroidChassisStyle
import com.example.data.model.DeviceProfile
import com.example.data.model.DisplayCutoutStyle
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.TitaniumSlate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun CyberArcadeBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val bgColor = MaterialTheme.colorScheme.background
    val elevatedTint = MaterialTheme.colorScheme.surfaceVariant

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = bgColor)
            // Subtle graphite top ambient vignette (no grid lines or light sweeps)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        elevatedTint.copy(alpha = 0.55f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, 0f),
                    radius = size.maxDimension * 0.72f
                ),
                center = Offset(size.width * 0.5f, 0f),
                radius = size.maxDimension * 0.72f
            )
        }
        content()
    }
}

@Composable
fun RadialTelemetryGauge(
    progress: Float,
    accentColor: Color,
    secondaryColor: Color = TitaniumSlate,
    size: Dp = 44.dp,
    strokeWidth: Dp = 3.5.dp,
    modifier: Modifier = Modifier,
    centerContent: @Composable () -> Unit = {}
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0.04f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "radial_gauge_progress"
    )
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(inset, inset)

            // Clean 270-degree track
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Crisp single-tone precision indicator arc
            drawArc(
                color = accentColor,
                startAngle = 135f,
                sweepAngle = 270f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        centerContent()
    }
}

@Composable
fun SegmentedLedBar(
    progress: Float,
    segmentCount: Int = 12,
    height: Dp = 5.dp,
    invertHeatColors: Boolean = false,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "led_bar_progress"
    )
    val cyan = MaterialTheme.colorScheme.primary
    val offColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val gap = 3.dp.toPx()
        val totalGap = gap * (segmentCount - 1)
        val segWidth = ((size.width - totalGap) / segmentCount).coerceAtLeast(2f)
        val litCount = (animatedProgress * segmentCount).roundToInt().coerceIn(0, segmentCount)

        for (index in 0 until segmentCount) {
            val x = index * (segWidth + gap)
            val fraction = index.toFloat() / (segmentCount - 1).coerceAtLeast(1)
            val segColor = if (invertHeatColors) {
                when {
                    fraction < 0.55f -> cyan
                    fraction < 0.80f -> AmberWarm
                    else -> CoralAlert
                }
            } else {
                when {
                    fraction < 0.70f -> cyan
                    else -> EmeraldPulse
                }
            }

            val isLit = index < litCount
            drawRoundRect(
                color = if (isLit) segColor else offColor,
                topLeft = Offset(x, 0f),
                size = Size(segWidth, size.height),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}

@Composable
fun LivePulseBeacon(
    color: Color = EmeraldPulse,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "live_beacon")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon_alpha"
    )

    Box(
        modifier = modifier.size(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = color.copy(alpha = pulseAlpha),
                radius = size.minDimension / 3.2f
            )
        }
    }
}

@Composable
fun DeviceHardwareIcon(
    deviceProfile: DeviceProfile,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    val frameColor = MaterialTheme.colorScheme.outlineVariant
    val bodyColor = MaterialTheme.colorScheme.surfaceVariant
    val screenColor = MaterialTheme.colorScheme.background
    val cyan = MaterialTheme.colorScheme.primary
    val mutedDetail = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            val (bodyW, bodyH) = when (deviceProfile.chassisStyle) {
                AndroidChassisStyle.TABLET -> w * 0.86f to h * 0.62f
                AndroidChassisStyle.FOLDABLE -> w * 0.78f to h * 0.82f
                else -> {
                    val safeAspect = deviceProfile.screenAspectRatio.coerceIn(1.85f, 2.35f)
                    val targetH = h * 0.88f
                    val targetW = (targetH / safeAspect).coerceIn(w * 0.40f, w * 0.56f)
                    targetW to targetH
                }
            }

            val left = (w - bodyW) / 2f
            val top = (h - bodyH) / 2f
            val cornerPx = (bodyW * deviceProfile.cornerRoundness.coerceIn(0.04f, 0.22f))
                .coerceAtLeast(3.dp.toPx())
            val bezel = (bodyW * 0.065f).coerceIn(1.8.dp.toPx(), 3.dp.toPx())

            // Physical side power + volume hardware buttons (right edge)
            val btnX = left + bodyW
            val btnW = 1.6.dp.toPx()
            drawRoundRect(
                color = frameColor.copy(alpha = 0.85f),
                topLeft = Offset(btnX, top + bodyH * 0.24f),
                size = Size(btnW, bodyH * 0.14f),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
            drawRoundRect(
                color = frameColor.copy(alpha = 0.85f),
                topLeft = Offset(btnX, top + bodyH * 0.43f),
                size = Size(btnW, bodyH * 0.09f),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )

            // Anodized graphite outer frame
            drawRoundRect(
                color = bodyColor,
                topLeft = Offset(left, top),
                size = Size(bodyW, bodyH),
                cornerRadius = CornerRadius(cornerPx, cornerPx)
            )
            drawRoundRect(
                color = frameColor,
                topLeft = Offset(left, top),
                size = Size(bodyW, bodyH),
                cornerRadius = CornerRadius(cornerPx, cornerPx),
                style = Stroke(width = 1.3.dp.toPx())
            )

            // OLED screen plane
            val screenLeft = left + bezel
            val screenTop = top + bezel
            val screenW = (bodyW - bezel * 2f).coerceAtLeast(4f)
            val screenH = (bodyH - bezel * 2f).coerceAtLeast(4f)
            val innerCorner = (cornerPx - bezel).coerceAtLeast(2.dp.toPx())

            drawRoundRect(
                color = screenColor,
                topLeft = Offset(screenLeft, screenTop),
                size = Size(screenW, screenH),
                cornerRadius = CornerRadius(innerCorner, innerCorner)
            )

            // Subtle Android OEM chassis hardware schematic engraved inside screen
            val schematicColor = mutedDetail.copy(alpha = 0.24f)
            when (deviceProfile.chassisStyle) {
                AndroidChassisStyle.PIXEL_VISOR -> {
                    val visorH = screenH * 0.11f
                    val visorY = screenTop + screenH * 0.22f
                    drawRoundRect(
                        color = schematicColor,
                        topLeft = Offset(screenLeft + screenW * 0.08f, visorY),
                        size = Size(screenW * 0.84f, visorH),
                        cornerRadius = CornerRadius(visorH / 2f, visorH / 2f)
                    )
                    drawCircle(
                        color = cyan.copy(alpha = 0.35f),
                        radius = visorH * 0.26f,
                        center = Offset(screenLeft + screenW * 0.30f, visorY + visorH / 2f)
                    )
                    drawCircle(
                        color = schematicColor,
                        radius = visorH * 0.22f,
                        center = Offset(screenLeft + screenW * 0.50f, visorY + visorH / 2f)
                    )
                }
                AndroidChassisStyle.GALAXY_RINGS -> {
                    val ringX = screenLeft + screenW * 0.28f
                    val ringR = (screenW * 0.08f).coerceAtLeast(1.8.dp.toPx())
                    for (idx in 0..2) {
                        drawCircle(
                            color = if (idx == 0) cyan.copy(alpha = 0.35f) else schematicColor,
                            radius = ringR,
                            center = Offset(ringX, screenTop + screenH * (0.20f + idx * 0.11f)),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                }
                AndroidChassisStyle.CIRCULAR_MODULE -> {
                    val modR = screenW * 0.24f
                    val modCenter = Offset(screenLeft + screenW * 0.5f, screenTop + screenH * 0.30f)
                    drawCircle(
                        color = schematicColor,
                        radius = modR,
                        center = modCenter,
                        style = Stroke(width = 1.1.dp.toPx())
                    )
                    drawCircle(
                        color = cyan.copy(alpha = 0.35f),
                        radius = modR * 0.26f,
                        center = Offset(modCenter.x - modR * 0.34f, modCenter.y - modR * 0.22f)
                    )
                }
                AndroidChassisStyle.NOTHING_MINIMAL -> {
                    drawArc(
                        color = schematicColor,
                        startAngle = 200f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(screenLeft + screenW * 0.25f, screenTop + screenH * 0.22f),
                        size = Size(screenW * 0.5f, screenW * 0.5f),
                        style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawCircle(
                        color = cyan.copy(alpha = 0.45f),
                        radius = 1.6.dp.toPx(),
                        center = Offset(screenLeft + screenW * 0.72f, screenTop + screenH * 0.26f)
                    )
                }
                AndroidChassisStyle.FOLDABLE -> {
                    val foldX = screenLeft + screenW / 2f
                    drawLine(
                        color = frameColor.copy(alpha = 0.7f),
                        start = Offset(foldX, screenTop + 2.dp.toPx()),
                        end = Offset(foldX, screenTop + screenH - 2.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                AndroidChassisStyle.TABLET -> {
                    drawRoundRect(
                        color = cyan.copy(alpha = 0.14f),
                        topLeft = Offset(screenLeft + screenW * 0.12f, screenTop + screenH * 0.24f),
                        size = Size(screenW * 0.76f, screenH * 0.42f),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
                AndroidChassisStyle.STANDARD_ANDROID -> {
                    drawRoundRect(
                        color = schematicColor,
                        topLeft = Offset(screenLeft + screenW * 0.18f, screenTop + screenH * 0.17f),
                        size = Size(screenW * 0.18f, screenH * 0.18f),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }

            // Real front camera cutout matching the user's actual Android display
            val cutoutY = screenTop + (screenH * 0.075f).coerceAtLeast(3.dp.toPx())
            val holeRadius = (screenW * 0.055f).coerceIn(1.3.dp.toPx(), 2.2.dp.toPx())
            when (deviceProfile.cutoutStyle) {
                DisplayCutoutStyle.NONE -> Unit
                DisplayCutoutStyle.PUNCH_HOLE_CENTER -> {
                    drawCircle(
                        color = frameColor,
                        radius = holeRadius * 1.25f,
                        center = Offset(screenLeft + screenW * 0.5f, cutoutY)
                    )
                    drawCircle(
                        color = cyan.copy(alpha = 0.55f),
                        radius = holeRadius * 0.55f,
                        center = Offset(screenLeft + screenW * 0.5f, cutoutY)
                    )
                }
                DisplayCutoutStyle.PUNCH_HOLE_LEFT -> {
                    drawCircle(
                        color = frameColor,
                        radius = holeRadius * 1.25f,
                        center = Offset(screenLeft + screenW * 0.20f, cutoutY)
                    )
                    drawCircle(
                        color = cyan.copy(alpha = 0.55f),
                        radius = holeRadius * 0.55f,
                        center = Offset(screenLeft + screenW * 0.20f, cutoutY)
                    )
                }
                DisplayCutoutStyle.PUNCH_HOLE_RIGHT -> {
                    drawCircle(
                        color = frameColor,
                        radius = holeRadius * 1.25f,
                        center = Offset(screenLeft + screenW * 0.80f, cutoutY)
                    )
                    drawCircle(
                        color = cyan.copy(alpha = 0.55f),
                        radius = holeRadius * 0.55f,
                        center = Offset(screenLeft + screenW * 0.80f, cutoutY)
                    )
                }
                DisplayCutoutStyle.PILL_CENTER -> {
                    val pillW = screenW * 0.26f
                    val pillH = holeRadius * 2f
                    drawRoundRect(
                        color = frameColor,
                        topLeft = Offset(screenLeft + (screenW - pillW) / 2f, cutoutY - pillH / 2f),
                        size = Size(pillW, pillH),
                        cornerRadius = CornerRadius(pillH / 2f, pillH / 2f)
                    )
                }
                DisplayCutoutStyle.NOTCH_CENTER -> {
                    val notchW = screenW * 0.34f
                    val notchH = holeRadius * 2.3f
                    drawRoundRect(
                        color = bodyColor,
                        topLeft = Offset(screenLeft + (screenW - notchW) / 2f, screenTop),
                        size = Size(notchW, notchH),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }
            }

            // Android bottom gesture navigation pill
            val navW = screenW * 0.34f
            drawRoundRect(
                color = mutedDetail.copy(alpha = 0.45f),
                topLeft = Offset(
                    screenLeft + (screenW - navW) / 2f,
                    screenTop + screenH - 4.dp.toPx()
                ),
                size = Size(navW, 1.4.dp.toPx()),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
        }
    }
}

fun Modifier.springPressScale(interactionSource: InteractionSource): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_press_scale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

fun Modifier.iconTintedGlow(
    accentColor: Color,
    isSpotlighted: Boolean
): Modifier = this.drawBehind {
    val glowAlpha = if (isSpotlighted) 0.12f else 0.06f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                accentColor.copy(alpha = glowAlpha),
                Color.Transparent
            ),
            center = Offset(size.width * 0.20f, size.height * 0.20f),
            radius = size.maxDimension * 0.68f
        ),
        center = Offset(size.width * 0.20f, size.height * 0.20f),
        radius = size.maxDimension * 0.68f
    )
}

fun resolveAccentColor(accentColorArgb: Long?, fallback: Color): Color {
    return if (accentColorArgb != null) Color(accentColorArgb.toInt()) else fallback
}

fun pulseCyclesPerSecond(cpuLoadFraction: Float): Float {
    val clamped = cpuLoadFraction.coerceIn(0f, 1f)
    // Scales smoothly from ~0.38 Hz (2.6s calm breath) to ~1.55 Hz (0.65s rapid pulse)
    return 0.38f + (1.55f - 0.38f) * clamped
}

fun Modifier.neonCyanPulseBorder(
    cpuLoadFraction: Float,
    cornerRadius: Dp = 20.dp,
    baseStrokeWidth: Dp = 1.2.dp,
    glowColor: Color = ElectricCyan
): Modifier = composed {
    val baseOutlineColor = MaterialTheme.colorScheme.outline
    val smoothedLoad by animateFloatAsState(
        targetValue = cpuLoadFraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 450, easing = LinearEasing),
        label = "cpu_load_smooth"
    )
    val latestLoad by rememberUpdatedState(smoothedLoad)
    var phase by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var lastFrameNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastFrameNanos != 0L) {
                    val dtSeconds = ((frameNanos - lastFrameNanos) / 1_000_000_000f)
                        .coerceIn(0f, 0.05f)
                    val speed = pulseCyclesPerSecond(latestLoad)
                    phase = (phase + dtSeconds * speed) % 1f
                }
                lastFrameNanos = frameNanos
            }
        }
    }

    this.drawWithContent {
        val theta = phase * 2f * PI.toFloat()
        val breath = 0.5f + 0.5f * sin(theta)
        val radiusPx = cornerRadius.toPx()
        val strokePx = baseStrokeWidth.toPx()

        // Restrained hairline outer cyan aura that intensifies gently with CPU load
        val haloAlpha = (0.06f + 0.12f * smoothedLoad) * (0.45f + 0.55f * breath)
        val haloWidth = strokePx * (2.2f + 1.1f * breath * (0.35f + 0.65f * smoothedLoad))

        drawRoundRect(
            color = glowColor.copy(alpha = haloAlpha),
            topLeft = Offset(-haloWidth * 0.12f, -haloWidth * 0.12f),
            size = Size(size.width + haloWidth * 0.24f, size.height + haloWidth * 0.24f),
            cornerRadius = CornerRadius(radiusPx + haloWidth * 0.12f, radiusPx + haloWidth * 0.12f),
            style = Stroke(width = haloWidth)
        )

        drawContent()

        val borderInset = strokePx / 2f
        val borderSize = Size(
            (size.width - strokePx).coerceAtLeast(1f),
            (size.height - strokePx).coerceAtLeast(1f)
        )
        val borderRadius = (radiusPx - borderInset).coerceAtLeast(1f)

        val cx = size.width / 2f
        val cy = size.height / 2f
        val sweepRadius = size.maxDimension * 0.58f
        val beamStart = Offset(cx + cos(theta) * sweepRadius, cy + sin(theta) * sweepRadius)
        val beamEnd = Offset(cx - cos(theta) * sweepRadius, cy - sin(theta) * sweepRadius)

        val baseAlpha = (0.28f + 0.34f * smoothedLoad) * (0.55f + 0.45f * breath)
        val crestAlpha = (0.58f + 0.32f * smoothedLoad) * (0.65f + 0.35f * breath)

        // Subtle graphite base hairline + orbiting precision cyan highlight
        drawRoundRect(
            color = baseOutlineColor,
            topLeft = Offset(borderInset, borderInset),
            size = borderSize,
            cornerRadius = CornerRadius(borderRadius, borderRadius),
            style = Stroke(width = strokePx)
        )

        drawRoundRect(
            brush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.00f to glowColor.copy(alpha = crestAlpha),
                    0.18f to glowColor.copy(alpha = baseAlpha * 0.8f),
                    0.50f to glowColor.copy(alpha = baseAlpha * 0.22f),
                    0.85f to Color.Transparent,
                    1.00f to glowColor.copy(alpha = baseAlpha * 0.35f)
                ),
                start = beamStart,
                end = beamEnd
            ),
            topLeft = Offset(borderInset, borderInset),
            size = borderSize,
            cornerRadius = CornerRadius(borderRadius, borderRadius),
            style = Stroke(width = strokePx * (1f + 0.15f * smoothedLoad * breath))
        )
    }
}
