package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HudThemeId
import kotlin.math.sin

@Composable
fun HudCard(
    theme: HudThemeId,
    modifier: Modifier = Modifier,
    cutCorner: Dp = 12.dp,
    borderAlpha: Float = 0.5f,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = CutCornerShape(
        topStart = cutCorner,
        topEnd = 0.dp,
        bottomEnd = cutCorner,
        bottomStart = 0.dp
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(theme.surfaceColor.copy(alpha = 0.85f))
            .border(
                BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(
                            theme.primaryColor.copy(alpha = borderAlpha),
                            theme.secondaryColor.copy(alpha = borderAlpha * 0.4f),
                            theme.primaryColor.copy(alpha = borderAlpha)
                        )
                    )
                ),
                shape
            )
            .padding(14.dp)
    ) {
        content()
    }
}

@Composable
fun GlowButton(
    text: String,
    theme: HudThemeId,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isDestructive: Boolean = false,
    testTag: String = "glow_button"
) {
    val buttonColor = if (isDestructive) Color(0xFFFF1744) else theme.primaryColor
    val shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp)

    Box(
        modifier = modifier
            .testTag(testTag)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        buttonColor.copy(alpha = 0.22f),
                        theme.secondaryColor.copy(alpha = 0.12f)
                    )
                )
            )
            .border(BorderStroke(1.2.dp, buttonColor.copy(alpha = 0.8f)), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = buttonColor,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(end = 6.dp)
                )
            }
            Text(
                text = text.uppercase(),
                color = buttonColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.2.sp
            )
        }
    }
}

@Composable
fun TelemetryChip(
    label: String,
    value: String,
    theme: HudThemeId,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(theme.surfaceVariantColor.copy(alpha = 0.6f))
            .border(0.8.dp, theme.primaryColor.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(theme.primaryColor)
        )
        Text(
            text = " $label: ",
            color = theme.textMutedColor,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = theme.textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun AudioWaveformBar(
    theme: HudThemeId,
    amplitude: Float,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 24
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_bars")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
    ) {
        val totalWidth = size.width
        val barWidth = (totalWidth / barCount) * 0.6f
        val gap = (totalWidth / barCount) * 0.4f
        val maxHeight = size.height

        for (i in 0 until barCount) {
            val progress = i.toFloat() / barCount
            val wave = sin((progress * 4f + phase).toDouble()).toFloat() * 0.5f + 0.5f
            val heightMultiplier = if (isActive) {
                (0.2f + wave * 0.4f + amplitude * 0.6f).coerceIn(0.1f, 1f)
            } else {
                0.12f
            }

            val currentBarHeight = maxHeight * heightMultiplier
            val left = i * (barWidth + gap)
            val top = (maxHeight - currentBarHeight) / 2f

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        theme.primaryColor,
                        theme.secondaryColor.copy(alpha = 0.6f)
                    )
                ),
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(barWidth, currentBarHeight)
            )
        }
    }
}
