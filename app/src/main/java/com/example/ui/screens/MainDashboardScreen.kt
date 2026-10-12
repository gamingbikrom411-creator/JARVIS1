package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProtocolStatus
import com.example.ui.JarvisViewModel
import com.example.ui.ScreenDestination
import com.example.ui.components.ArcReactorVisualizer
import com.example.ui.components.AudioWaveformBar
import com.example.ui.components.GlowButton
import com.example.ui.components.HudCard
import com.example.ui.components.TelemetryChip

@Composable
fun MainDashboardScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val theme by viewModel.activeTheme.collectAsState()
    val amplitude by viewModel.voiceEngine.amplitude.collectAsState()
    val isSpeaking by viewModel.voiceEngine.isSpeaking.collectAsState()
    val isListening by viewModel.voiceEngine.isListening.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val protocols by viewModel.protocols.collectAsState()
    val lastTranscript by viewModel.lastTranscript.collectAsState()

    val userName = userProfile?.userName ?: "Sir"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. Top HUD Header Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(theme.primaryColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "J.A.R.V.I.S.",
                        color = theme.primaryColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                }
                Text(
                    text = "STARK IND. // OS v9.4",
                    color = theme.textMutedColor,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Quick Nav Icons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { viewModel.navigateTo(ScreenDestination.PROTOCOLS) },
                    modifier = Modifier.testTag("nav_protocols_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Protocols",
                        tint = theme.primaryColor
                    )
                }
                IconButton(
                    onClick = { viewModel.navigateTo(ScreenDestination.SCANNER) },
                    modifier = Modifier.testTag("nav_scanner_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Scanner",
                        tint = theme.primaryColor
                    )
                }
                IconButton(
                    onClick = { viewModel.navigateTo(ScreenDestination.MEMORY) },
                    modifier = Modifier.testTag("nav_memory_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Memory",
                        tint = theme.primaryColor
                    )
                }
                IconButton(
                    onClick = { viewModel.navigateTo(ScreenDestination.THEMES) },
                    modifier = Modifier.testTag("nav_themes_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Themes",
                        tint = theme.primaryColor
                    )
                }
            }
        }

        // --- 2. Telemetry Ribbon ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TelemetryChip(label = "ARC CORE", value = "99.8%", theme = theme)
            TelemetryChip(label = "USER", value = userName, theme = theme)
            TelemetryChip(label = "THEME", value = theme.displayName.take(8), theme = theme)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 3. Centerpiece: 3D Holographic Arc Reactor Visualizer ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            ArcReactorVisualizer(
                theme = theme,
                amplitude = amplitude,
                isSpeaking = isSpeaking,
                isListening = isListening,
                size = 250.dp,
                onClick = { viewModel.triggerQuickVoiceInput() }
            )
        }

        // Status Readout
        val statusText = when {
            isListening -> "LISTENING TO $userName..."
            isSpeaking -> "JARVIS VOCALIZING..."
            else -> "TAP REACTOR OR START LIVE CALL"
        }

        Text(
            text = statusText,
            color = if (isListening || isSpeaking) theme.primaryColor else theme.textMutedColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )

        // Audio Frequency Waveform Bars
        AudioWaveformBar(
            theme = theme,
            amplitude = amplitude,
            isActive = isSpeaking || isListening,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // --- 4. Primary Live Voice Controls ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Live Voice Call Button
            GlowButton(
                text = "Live Voice Call",
                theme = theme,
                icon = Icons.Default.Call,
                onClick = {
                    viewModel.navigateTo(ScreenDestination.LIVE_CALL)
                    viewModel.startLiveCall()
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("start_live_call_btn")
            )

            // Quick Speech Mic Button
            GlowButton(
                text = if (isListening) "Listening..." else "Tap to Speak",
                theme = theme,
                icon = Icons.Default.Mic,
                onClick = { viewModel.triggerQuickVoiceInput() },
                modifier = Modifier
                    .weight(1f)
                    .testTag("quick_mic_btn")
            )
        }

        // Live Transcript Peek Card
        if (lastTranscript.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            HudCard(theme = theme, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = theme.primaryColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = lastTranscript,
                        color = theme.textColor,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // --- 5. Stark Protocols Quick Deck ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "STARK PROTOCOLS",
                color = theme.primaryColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.2.sp
            )
            Text(
                text = "VIEW ALL >",
                color = theme.textMutedColor,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.clickable { viewModel.navigateTo(ScreenDestination.PROTOCOLS) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3 Featured Protocols
        protocols.take(3).forEach { protocol ->
            val isActive = protocol.status == ProtocolStatus.ACTIVE
            val isInit = protocol.status == ProtocolStatus.INITIALIZING

            HudCard(
                theme = theme,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("quick_protocol_${protocol.id}")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = protocol.title,
                                color = theme.textColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val statusBadge = when {
                                isActive -> "ACTIVE"
                                isInit -> "LAUNCHING..."
                                else -> "READY"
                            }
                            val badgeColor = when {
                                isActive -> Color(0xFF00E676)
                                isInit -> Color(0xFFFF9100)
                                else -> theme.primaryColor.copy(alpha = 0.7f)
                            }
                            Text(
                                text = "[$statusBadge]",
                                color = badgeColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = protocol.description,
                            color = theme.textMutedColor,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    GlowButton(
                        text = if (isActive) "RESET" else "DEPLOY",
                        theme = theme,
                        isDestructive = isActive,
                        onClick = {
                            if (isActive) viewModel.resetProtocol(protocol.id)
                            else viewModel.activateProtocol(protocol.id)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 6. Tactical Scanner Banner ---
        HudCard(
            theme = theme,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.navigateTo(ScreenDestination.SCANNER) }
                .testTag("dashboard_scanner_card")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CutCornerShape(8.dp))
                        .background(theme.primaryColor.copy(alpha = 0.15f))
                        .border(1.dp, theme.primaryColor, CutCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = theme.primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "OPTICAL CAMERA SCANNER",
                        color = theme.textColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Real-time tactical HUD analysis & threat assessment via Gemini Vision",
                        color = theme.textMutedColor,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
