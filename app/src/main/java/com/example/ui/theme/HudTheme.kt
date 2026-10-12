package com.example.ui.theme

import androidx.compose.ui.graphics.Color

enum class HudThemeId(
    val displayName: String,
    val subtitle: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val surfaceVariantColor: Color,
    val glowColor: Color,
    val textColor: Color,
    val textMutedColor: Color
) {
    CLASSIC_CYAN(
        displayName = "Classic Cyan",
        subtitle = "Mark VII Arc Core",
        primaryColor = Color(0xFF00E5FF),
        secondaryColor = Color(0xFF00B0FF),
        accentColor = Color(0xFF80D8FF),
        backgroundColor = Color(0xFF050B14),
        surfaceColor = Color(0xFF0B1726),
        surfaceVariantColor = Color(0xFF10253D),
        glowColor = Color(0x6600E5FF),
        textColor = Color(0xFFE0F7FA),
        textMutedColor = Color(0xFF80DEEA)
    ),
    MARK_L_GOLD(
        displayName = "Mark L Gold",
        subtitle = "Nano-tech Alloy & Crimson",
        primaryColor = Color(0xFFFFD700),
        secondaryColor = Color(0xFFE53935),
        accentColor = Color(0xFFFFB300),
        backgroundColor = Color(0xFF140807),
        surfaceColor = Color(0xFF24100E),
        surfaceVariantColor = Color(0xFF381916),
        glowColor = Color(0x66FFD700),
        textColor = Color(0xFFFFF8E1),
        textMutedColor = Color(0xFFFFCC80)
    ),
    STEALTH_VIOLET(
        displayName = "Stealth Violet",
        subtitle = "Black Ops Jammer",
        primaryColor = Color(0xFFD500F9),
        secondaryColor = Color(0xFFAA00FF),
        accentColor = Color(0xFFEA80FC),
        backgroundColor = Color(0xFF0D0616),
        surfaceColor = Color(0xFF190D2A),
        surfaceVariantColor = Color(0xFF281544),
        glowColor = Color(0x66D500F9),
        textColor = Color(0xFFF3E5F5),
        textMutedColor = Color(0xFFCE93D8)
    ),
    VERONICA_EMERALD(
        displayName = "Veronica Emerald",
        subtitle = "Orbital Hulkbuster Lock",
        primaryColor = Color(0xFF00E676),
        secondaryColor = Color(0xFF00BFA5),
        accentColor = Color(0xFF69F0AE),
        backgroundColor = Color(0xFF04130C),
        surfaceColor = Color(0xFF0A2218),
        surfaceVariantColor = Color(0xFF123627),
        glowColor = Color(0x6600E676),
        textColor = Color(0xFFE8F5E9),
        textMutedColor = Color(0xFFA5D6A7)
    ),
    SOLAR_AMBER(
        displayName = "Solar Amber",
        subtitle = "Overcharged Arc Capacitor",
        primaryColor = Color(0xFFFF9100),
        secondaryColor = Color(0xFFFF6D00),
        accentColor = Color(0xFFFFAB40),
        backgroundColor = Color(0xFF140A02),
        surfaceColor = Color(0xFF241407),
        surfaceVariantColor = Color(0xFF3B210C),
        glowColor = Color(0x66FF9100),
        textColor = Color(0xFFFFF3E0),
        textMutedColor = Color(0xFFFFCC80)
    ),
    CRIMSON_RED(
        displayName = "Crimson Red",
        subtitle = "War Machine Threat Level",
        primaryColor = Color(0xFFFF1744),
        secondaryColor = Color(0xFFD50000),
        accentColor = Color(0xFFFF5252),
        backgroundColor = Color(0xFF160407),
        surfaceColor = Color(0xFF280A10),
        surfaceVariantColor = Color(0xFF40121C),
        glowColor = Color(0x66FF1744),
        textColor = Color(0xFFFFEBEE),
        textMutedColor = Color(0xFFEF9A9A)
    ),
    NEON_GREEN(
        displayName = "Neon Green",
        subtitle = "Matrix Telemetry Grid",
        primaryColor = Color(0xFF76FF03),
        secondaryColor = Color(0xFF00E676),
        accentColor = Color(0xFFB2FF59),
        backgroundColor = Color(0xFF051305),
        surfaceColor = Color(0xFF0C240E),
        surfaceVariantColor = Color(0xFF153818),
        glowColor = Color(0x6676FF03),
        textColor = Color(0xFFF1F8E9),
        textMutedColor = Color(0xFFC5E1A5)
    ),
    DEEP_BLUE(
        displayName = "Deep Blue",
        subtitle = "Deep Space Stark-Net",
        primaryColor = Color(0xFF2979FF),
        secondaryColor = Color(0xFF3D5AFE),
        accentColor = Color(0xFF448AFF),
        backgroundColor = Color(0xFF040A1A),
        surfaceColor = Color(0xFF0A1636),
        surfaceVariantColor = Color(0xFF112352),
        glowColor = Color(0x662979FF),
        textColor = Color(0xFFE3F2FD),
        textMutedColor = Color(0xFF90CAF9)
    )
}
