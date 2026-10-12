package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProtocolStatus
import com.example.data.model.StarkProtocol
import com.example.ui.JarvisViewModel
import com.example.ui.ScreenDestination
import com.example.ui.components.GlowButton
import com.example.ui.components.HudCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StarkProtocolsScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val theme by viewModel.activeTheme.collectAsState()
    val protocols by viewModel.protocols.collectAsState()

    BackHandler {
        viewModel.navigateTo(ScreenDestination.DASHBOARD)
    }

    val activeCount = protocols.count { it.status == ProtocolStatus.ACTIVE }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // --- Header Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(ScreenDestination.DASHBOARD) },
                modifier = Modifier.testTag("protocols_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = theme.primaryColor
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "STARK DEFENSE PROTOCOLS",
                    color = theme.primaryColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "LEVEL 7 TACTICAL CLEARANCE // $activeCount ACTIVE",
                    color = theme.textMutedColor,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Voice Command Hint Bar
        HudCard(
            theme = theme,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = theme.primaryColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "VOICE TRIGGER ACTIVATION",
                        color = theme.textColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Say: 'Jarvis, initiate House Party Protocol' or 'Veronica'",
                        color = theme.textMutedColor,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // --- Protocols List ---
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(protocols, key = { it.id }) { protocol ->
                ProtocolCardItem(protocol = protocol, viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProtocolCardItem(
    protocol: StarkProtocol,
    viewModel: JarvisViewModel
) {
    val theme by viewModel.activeTheme.collectAsState()
    val isActive = protocol.status == ProtocolStatus.ACTIVE
    val isInit = protocol.status == ProtocolStatus.INITIALIZING

    val infiniteTransition = rememberInfiniteTransition(label = "proto_pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    HudCard(
        theme = theme,
        borderAlpha = if (isActive) 0.9f else 0.4f,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("protocol_card_${protocol.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when (protocol.severityLevel) {
                                    "OMEGA", "CRITICAL" -> Color(0xFFFF1744).copy(alpha = 0.2f)
                                    "HEAVY" -> Color(0xFFFF9100).copy(alpha = 0.2f)
                                    else -> theme.primaryColor.copy(alpha = 0.2f)
                                }
                            )
                            .border(
                                0.8.dp,
                                when (protocol.severityLevel) {
                                    "OMEGA", "CRITICAL" -> Color(0xFFFF1744)
                                    "HEAVY" -> Color(0xFFFF9100)
                                    else -> theme.primaryColor
                                },
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = protocol.codeName,
                            color = when (protocol.severityLevel) {
                                "OMEGA", "CRITICAL" -> Color(0xFFFF1744)
                                "HEAVY" -> Color(0xFFFF9100)
                                else -> theme.primaryColor
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = protocol.title,
                        color = theme.textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Status Badge
                val statusColor = when {
                    isActive -> Color(0xFF00E676)
                    isInit -> Color(0xFFFF9100)
                    else -> theme.textMutedColor
                }
                Text(
                    text = when {
                        isActive -> "ENGAGED"
                        isInit -> "DEPLOYING..."
                        else -> "STANDBY"
                    },
                    color = if (isInit) statusColor.copy(alpha = alphaAnim) else statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Description
            Text(
                text = protocol.description,
                color = theme.textMutedColor,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
            )

            // Progress bar when initializing
            AnimatedVisibility(visible = isInit) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        text = "ESTABLISHING SATELLITE TELEMETRY...",
                        color = Color(0xFFFF9100),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .padding(top = 2.dp),
                        color = Color(0xFFFF9100),
                        trackColor = theme.surfaceColor
                    )
                }
            }

            // Assigned Suit Units / Modules Chips
            if (protocol.suitUnits.isNotEmpty()) {
                Text(
                    text = "DEPLOYED UNITS:",
                    color = theme.textMutedColor,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    protocol.suitUnits.forEach { unit ->
                        Box(
                            modifier = Modifier
                                .clip(CutCornerShape(4.dp))
                                .background(theme.surfaceVariantColor.copy(alpha = 0.6f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = unit,
                                color = theme.textColor,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Action Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (isActive) {
                    GlowButton(
                        text = "DISENGAGE / RESET",
                        theme = theme,
                        isDestructive = true,
                        onClick = { viewModel.resetProtocol(protocol.id) },
                        testTag = "reset_${protocol.id}"
                    )
                } else {
                    GlowButton(
                        text = if (isInit) "CANCEL" else "DEPLOY PROTOCOL",
                        theme = theme,
                        onClick = {
                            if (isInit) viewModel.resetProtocol(protocol.id)
                            else viewModel.activateProtocol(protocol.id)
                        },
                        testTag = "deploy_${protocol.id}"
                    )
                }
            }
        }
    }
}
