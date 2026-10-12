package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.HudThemeId
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactorVisualizer(
    theme: HudThemeId,
    amplitude: Float,
    isSpeaking: Boolean,
    isListening: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 260.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arc_reactor_anim")

    // Clockwise outer ring rotation
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_rotation"
    )

    // Counter-clockwise inner ring rotation
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_rotation"
    )

    // Gentle idle breathing glow
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_pulse"
    )

    val effectiveAmplitude = (amplitude * 1.5f).coerceIn(0f, 1f)
    val dynamicPulse = if (isSpeaking || isListening) {
        1f + effectiveAmplitude * 0.45f
    } else {
        idlePulse
    }

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.minDimension / 2f * 0.88f

            val primary = theme.primaryColor
            val secondary = theme.secondaryColor
            val accent = theme.accentColor
            val glow = theme.glowColor

            // 1. Outermost glow aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glow.copy(alpha = if (isSpeaking || isListening) 0.5f else 0.25f),
                        glow.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.15f * dynamicPulse
                ),
                radius = baseRadius * 1.15f * dynamicPulse,
                center = center
            )

            // 2. Radial Audio Waveform spikes (40 spikes around circumference)
            val spikeCount = 36
            for (i in 0 until spikeCount) {
                val angleDeg = (i * (360f / spikeCount)) + outerRotation * 0.2f
                val angleRad = (angleDeg * PI / 180f).toFloat()

                val waveHeight = if (isSpeaking || isListening) {
                    val harmonic = sin(i * 0.8f + outerRotation * 0.1f) * 0.5f + 0.5f
                    (harmonic * effectiveAmplitude * 28f).coerceAtLeast(4f)
                } else {
                    4f + (i % 3) * 2f
                }

                val startR = baseRadius * 0.94f
                val endR = startR + waveHeight
                val startX = center.x + cos(angleRad) * startR
                val startY = center.y + sin(angleRad) * startR
                val endX = center.x + cos(angleRad) * endR
                val endY = center.y + sin(angleRad) * endR

                drawLine(
                    color = primary.copy(alpha = if (isSpeaking || isListening) 0.85f else 0.4f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
            }

            // 3. Outermost mechanical tick ring
            rotate(outerRotation, pivot = center) {
                drawCircle(
                    color = primary.copy(alpha = 0.5f),
                    radius = baseRadius * 0.92f,
                    center = center,
                    style = Stroke(width = 1.5f)
                )

                // 4 Quadrant Major Brackets
                for (deg in listOf(0f, 90f, 180f, 270f)) {
                    drawArc(
                        color = accent,
                        startAngle = deg - 12f,
                        sweepAngle = 24f,
                        useCenter = false,
                        topLeft = Offset(center.x - baseRadius * 0.92f, center.y - baseRadius * 0.92f),
                        size = androidx.compose.ui.geometry.Size(baseRadius * 1.84f, baseRadius * 1.84f),
                        style = Stroke(width = 4f, cap = StrokeCap.Square)
                    )
                }

                // 24 Minor Tick lines
                for (j in 0 until 24) {
                    val tickAngle = (j * 15f * PI / 180f).toFloat()
                    val tInner = baseRadius * 0.86f
                    val tOuter = baseRadius * 0.91f
                    drawLine(
                        color = primary.copy(alpha = if (j % 6 == 0) 0.9f else 0.35f),
                        start = Offset(center.x + cos(tickAngle) * tInner, center.y + sin(tickAngle) * tInner),
                        end = Offset(center.x + cos(tickAngle) * tOuter, center.y + sin(tickAngle) * tOuter),
                        strokeWidth = if (j % 6 == 0) 3f else 1.5f
                    )
                }
            }

            // 4. Middle Arc Reactor Coils Ring (10 segments)
            rotate(innerRotation, pivot = center) {
                val coilCount = 10
                val coilRadius = baseRadius * 0.68f

                drawCircle(
                    color = secondary.copy(alpha = 0.6f),
                    radius = coilRadius,
                    center = center,
                    style = Stroke(width = 2f)
                )

                for (k in 0 until coilCount) {
                    val startAngle = k * (360f / coilCount)
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(primary, secondary, primary),
                            center = center
                        ),
                        startAngle = startAngle + 4f,
                        sweepAngle = (360f / coilCount) - 8f,
                        useCenter = false,
                        topLeft = Offset(center.x - coilRadius, center.y - coilRadius),
                        size = androidx.compose.ui.geometry.Size(coilRadius * 2f, coilRadius * 2f),
                        style = Stroke(width = 10f, cap = StrokeCap.Butt)
                    )
                }
            }

            // 5. Inner Core Ring
            val innerRingRadius = baseRadius * 0.45f * dynamicPulse
            drawCircle(
                color = primary,
                radius = innerRingRadius,
                center = center,
                style = Stroke(width = 3.5f)
            )

            // 6. Central Arc Triangular Core Geometry
            rotate(-outerRotation * 0.5f, pivot = center) {
                val triRadius = innerRingRadius * 0.72f
                val path = Path()
                for (p in 0 until 3) {
                    val pAngle = (p * 120f - 90f) * PI / 180f
                    val px = center.x + (cos(pAngle) * triRadius).toFloat()
                    val py = center.y + (sin(pAngle) * triRadius).toFloat()
                    if (p == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()

                drawPath(
                    path = path,
                    color = accent.copy(alpha = 0.85f),
                    style = Stroke(width = 3f)
                )
            }

            // 7. Core Plasma Center (Hyper-bright pulsing orb)
            val plasmaRadius = baseRadius * 0.22f * dynamicPulse
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        primary.copy(alpha = 0.95f),
                        secondary.copy(alpha = 0.6f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = plasmaRadius * 1.3f
                ),
                radius = plasmaRadius,
                center = center
            )
        }
    }
}
