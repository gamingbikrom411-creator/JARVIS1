package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.JarvisViewModel
import com.example.ui.ScreenDestination
import com.example.ui.components.ArcReactorVisualizer
import com.example.ui.components.AudioWaveformBar
import com.example.ui.components.HudCard

@Composable
fun LiveVoiceCallScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val theme by viewModel.activeTheme.collectAsState()
    val amplitude by viewModel.voiceEngine.amplitude.collectAsState()
    val isSpeaking by viewModel.voiceEngine.isSpeaking.collectAsState()
    val isListening by viewModel.voiceEngine.isListening.collectAsState()
    val durationSeconds by viewModel.callDurationSeconds.collectAsState()
    val callStatus by viewModel.liveCallStatus.collectAsState()
    val lastTranscript by viewModel.lastTranscript.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    val currentLang = userProfile?.preferredLanguage ?: "Hinglish"
    val userName = userProfile?.userName ?: "Sir"

    BackHandler {
        viewModel.endLiveCall()
        viewModel.navigateTo(ScreenDestination.DASHBOARD)
    }

    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    val formattedDuration = "%02d:%02d".format(minutes, seconds)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- 1. Header Information ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = theme.primaryColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "STARK SATELLITE COMMS // SECURE",
                    color = theme.primaryColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp
                )
            }

            Text(
                text = "LIVE VOICE CALL WITH J.A.R.V.I.S.",
                color = theme.textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Duration and Pulse Dot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isMuted) Color.Yellow else Color(0xFF00E676))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formattedDuration,
                    color = theme.textMutedColor,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            // Language Selector Chips
            Row(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                listOf("English", "Hindi", "Hinglish", "Bangla").forEach { lang ->
                    val isSelected = currentLang.equals(lang, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) theme.primaryColor.copy(alpha = 0.25f)
                                else theme.surfaceColor
                            )
                            .border(
                                width = if (isSelected) 1.2.dp else 0.6.dp,
                                color = if (isSelected) theme.primaryColor else theme.textMutedColor.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.setPreferredLanguage(lang) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = lang,
                            color = if (isSelected) theme.primaryColor else theme.textMutedColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // --- 2. Centerpiece: Arc Reactor & Audio Waveform ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            ArcReactorVisualizer(
                theme = theme,
                amplitude = amplitude,
                isSpeaking = isSpeaking,
                isListening = isListening,
                size = 220.dp,
                onClick = {
                    if (!isListening && !isSpeaking && !isMuted) {
                        viewModel.triggerQuickVoiceInput()
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Call Status readout
            Text(
                text = callStatus.uppercase(),
                color = if (isSpeaking || isListening) theme.primaryColor else theme.textMutedColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            AudioWaveformBar(
                theme = theme,
                amplitude = amplitude,
                isActive = isSpeaking || isListening,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Live Transcript Card
            HudCard(
                theme = theme,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = if (lastTranscript.isNotBlank()) lastTranscript else "Speak naturally to J.A.R.V.I.S. in your preferred language...",
                        color = if (lastTranscript.isNotBlank()) theme.textColor else theme.textMutedColor.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // --- 3. Bottom Call Controls ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Quick suggested prompts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.Center
            ) {
                listOf(
                    "আজকে অনেক ভালো লাগছে! 🔥",
                    "ধুর, কাজ করল না 😡",
                    "কিছুই বুঝতে পারছি না",
                    "House Party Protocol",
                    "How do I seem to be feeling?"
                ).forEach { prompt ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(theme.surfaceVariantColor.copy(alpha = 0.5f))
                            .clickable { viewModel.processUserInput(prompt) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = prompt,
                            color = theme.textMutedColor,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Call Actions Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute Button
                IconButton(
                    onClick = { viewModel.toggleMute() },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(if (isMuted) Color(0xFFFF5252).copy(alpha = 0.2f) else theme.surfaceColor)
                        .border(
                            1.dp,
                            if (isMuted) Color(0xFFFF5252) else theme.primaryColor.copy(alpha = 0.5f),
                            CircleShape
                        )
                        .testTag("live_call_mute_btn")
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mute",
                        tint = if (isMuted) Color(0xFFFF5252) else theme.primaryColor
                    )
                }

                // End Call Button
                IconButton(
                    onClick = {
                        viewModel.endLiveCall()
                        viewModel.navigateTo(ScreenDestination.DASHBOARD)
                    },
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD50000))
                        .testTag("end_call_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Push to Talk / Manual Listen trigger
                IconButton(
                    onClick = { viewModel.triggerQuickVoiceInput() },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(if (isListening) theme.primaryColor.copy(alpha = 0.3f) else theme.surfaceColor)
                        .border(1.dp, theme.primaryColor.copy(alpha = 0.5f), CircleShape)
                        .testTag("live_call_listen_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Hearing,
                        contentDescription = "Listen",
                        tint = theme.primaryColor
                    )
                }
            }

            Text(
                text = "ENCRYPTED STARK AUDIO STREAM",
                color = theme.textMutedColor.copy(alpha = 0.5f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
