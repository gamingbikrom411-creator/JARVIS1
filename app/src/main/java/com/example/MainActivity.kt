package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.JarvisViewModel
import com.example.ui.ScreenDestination
import com.example.ui.screens.CameraScannerScreen
import com.example.ui.screens.LiveVoiceCallScreen
import com.example.ui.screens.MainDashboardScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.StarkProtocolsScreen
import com.example.ui.screens.ThemesScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val jarvisViewModel: JarvisViewModel = viewModel()
            val theme by jarvisViewModel.activeTheme.collectAsState()

            // Gracefully request microphone permission if not yet granted
            val audioPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                // Handled
            }

            LaunchedEffect(Unit) {
                if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.RECORD_AUDIO
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }

            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(theme.backgroundColor),
                    containerColor = theme.backgroundColor
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        JarvisAppNavHost(viewModel = jarvisViewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun JarvisAppNavHost(viewModel: JarvisViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    Crossfade(targetState = currentScreen, label = "screen_crossfade") { screen ->
        when (screen) {
            ScreenDestination.DASHBOARD -> MainDashboardScreen(viewModel = viewModel)
            ScreenDestination.LIVE_CALL -> LiveVoiceCallScreen(viewModel = viewModel)
            ScreenDestination.PROTOCOLS -> StarkProtocolsScreen(viewModel = viewModel)
            ScreenDestination.SCANNER -> CameraScannerScreen(viewModel = viewModel)
            ScreenDestination.THEMES -> ThemesScreen(viewModel = viewModel)
            ScreenDestination.MEMORY -> MemoryScreen(viewModel = viewModel)
        }
    }
}
