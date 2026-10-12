package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.JarvisViewModel
import com.example.ui.ScreenDestination
import com.example.ui.components.GlowButton
import com.example.ui.components.HudCard
import com.example.ui.components.TelemetryChip

@Composable
fun CameraScannerScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val theme by viewModel.activeTheme.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val latestAnalysis by viewModel.latestAnalysis.collectAsState()
    val capturedBitmap by viewModel.capturedBitmap.collectAsState()
    val context = LocalContext.current

    BackHandler {
        viewModel.navigateTo(ScreenDestination.DASHBOARD)
    }

    // Camera launcher (zero extra manifest hassle, returns bitmap directly)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.setCapturedImage(bitmap)
        }
    }

    // Photo picker launcher (complying with Google Play zero storage permission)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    viewModel.setCapturedImage(bitmap)
                }
            } catch (_: Exception) {}
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "laser_sweep")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "laser_y"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(ScreenDestination.DASHBOARD) },
                modifier = Modifier.testTag("scanner_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = theme.primaryColor
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "OPTICAL HUD SCANNER",
                    color = theme.primaryColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "TACTICAL THREAT ANALYSIS // GEMINI VISION",
                    color = theme.textMutedColor,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // --- Optical Viewfinder Box ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.2f)
                .clip(CutCornerShape(12.dp))
                .background(Color(0xFF03070E))
                .border(1.5.dp, theme.primaryColor.copy(alpha = 0.6f), CutCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Viewfinder image or placeholder
            if (capturedBitmap != null) {
                Image(
                    bitmap = capturedBitmap!!.asImageBitmap(),
                    contentDescription = "Target",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = theme.primaryColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "AWAITING TARGET IMAGE",
                        color = theme.textMutedColor,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap Live Scan or Sample Target below",
                        color = theme.textMutedColor.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                }
            }

            // --- Sci-Fi HUD Overlay Canvas ---
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val bracketLen = 28f
                val strokeW = 3f
                val pColor = theme.primaryColor

                // 4 Corner Brackets
                // Top-Left
                drawLine(pColor, Offset(16f, 16f), Offset(16f + bracketLen, 16f), strokeW)
                drawLine(pColor, Offset(16f, 16f), Offset(16f, 16f + bracketLen), strokeW)

                // Top-Right
                drawLine(pColor, Offset(w - 16f, 16f), Offset(w - 16f - bracketLen, 16f), strokeW)
                drawLine(pColor, Offset(w - 16f, 16f), Offset(w - 16f, 16f + bracketLen), strokeW)

                // Bottom-Left
                drawLine(pColor, Offset(16f, h - 16f), Offset(16f + bracketLen, h - 16f), strokeW)
                drawLine(pColor, Offset(16f, h - 16f), Offset(16f, h - 16f - bracketLen), strokeW)

                // Bottom-Right
                drawLine(pColor, Offset(w - 16f, h - 16f), Offset(w - 16f - bracketLen, h - 16f), strokeW)
                drawLine(pColor, Offset(w - 16f, h - 16f), Offset(w - 16f, h - 16f - bracketLen), strokeW)

                // Central Crosshair Reticle
                val cx = w / 2f
                val cy = h / 2f
                val crossLen = 22f
                drawLine(pColor.copy(alpha = 0.7f), Offset(cx - crossLen, cy), Offset(cx + crossLen, cy), 1.5f)
                drawLine(pColor.copy(alpha = 0.7f), Offset(cx, cy - crossLen), Offset(cx, cy + crossLen), 1.5f)
                drawCircle(pColor.copy(alpha = 0.5f), radius = 32f, center = Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))

                // Scanning Laser Line
                val currentLaserY = h * laserY
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, theme.primaryColor, Color.White, theme.primaryColor, Color.Transparent)
                    ),
                    start = Offset(0f, currentLaserY),
                    end = Offset(w, currentLaserY),
                    strokeWidth = 3f
                )
            }

            // Scanning Status Overlay
            if (isScanning) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = theme.primaryColor,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "COMPUTING TACTICAL SCAN...",
                            color = theme.primaryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Viewfinder Telemetry
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TelemetryChip(label = "OPTIC", value = "4K SENSOR", theme = theme)
            TelemetryChip(label = "GRID", value = "LOCK: ENGAGED", theme = theme)
            TelemetryChip(label = "ZOOM", value = "1.0X TACTICAL", theme = theme)
        }

        // Action Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlowButton(
                text = "Live Scan",
                theme = theme,
                icon = Icons.Default.CameraAlt,
                onClick = { cameraLauncher.launch(null) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("camera_snap_btn")
            )

            GlowButton(
                text = "Gallery",
                theme = theme,
                icon = Icons.Default.PhotoLibrary,
                onClick = {
                    photoPickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("gallery_pick_btn")
            )
        }

        // Quick Simulated Tactical Targets (handy for quick tests)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            listOf(
                "Mark L Armor" to 0xFFE53935.toInt(),
                "Quantum Core" to 0xFF00E5FF.toInt(),
                "Hostile Drone" to 0xFFFF9100.toInt()
            ).forEach { (label, tint) ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(theme.surfaceVariantColor.copy(alpha = 0.5f))
                        .border(0.6.dp, theme.primaryColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .clickable {
                            val sample = createSampleTargetBitmap(label, tint)
                            viewModel.setCapturedImage(sample)
                        }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "+ $label",
                        color = theme.textMutedColor,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- Tactical Analysis Report Card ---
        if (latestAnalysis != null) {
            val report = latestAnalysis!!
            val threatColor = when (report.threatLevel) {
                "CRITICAL", "HIGH" -> Color(0xFFFF1744)
                "MODERATE" -> Color(0xFFFF9100)
                else -> Color(0xFF00E676)
            }

            HudCard(
                theme = theme,
                borderAlpha = 0.9f,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tactical_analysis_report")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "TACTICAL ASSESSMENT REPORT",
                            color = theme.primaryColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(threatColor.copy(alpha = 0.2f))
                                .border(1.dp, threatColor, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "THREAT: ${report.threatLevel}",
                                color = threatColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "TARGET: ${report.targetClassification}",
                        color = theme.textColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "COMPOSITION: ${report.structuralComposition}",
                        color = theme.textMutedColor,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    HudCard(
                        theme = theme,
                        modifier = Modifier.fillMaxWidth(),
                        borderAlpha = 0.3f
                    ) {
                        Column {
                            Text(
                                text = "TACTICAL ADVICE:",
                                color = theme.primaryColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = report.tacticalAdvice,
                                color = theme.textColor,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// Helper to generate a clean tactical blueprint test bitmap
private fun createSampleTargetBitmap(name: String, colorInt: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(400, 320, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        isAntiAlias = true
    }
    // Background dark navy
    paint.color = 0xFF0A101C.toInt()
    canvas.drawRect(0f, 0f, 400f, 320f, paint)

    // Center circular reactor
    paint.color = colorInt
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 6f
    canvas.drawCircle(200f, 140f, 60f, paint)
    canvas.drawCircle(200f, 140f, 30f, paint)

    paint.style = Paint.Style.FILL
    canvas.drawCircle(200f, 140f, 14f, paint)

    // Label
    paint.color = 0xFFFFFFFF.toInt()
    paint.textSize = 24f
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText("TARGET: $name", 200f, 260f, paint)

    return bitmap
}
