package com.example.data.model

data class StarkProtocol(
    val id: String,
    val codeName: String,
    val title: String,
    val description: String,
    val voiceTrigger: String,
    val status: ProtocolStatus = ProtocolStatus.STANDBY,
    val severityLevel: String = "ALPHA",
    val suitUnits: List<String> = emptyList(),
    val iconName: String = "shield"
)

enum class ProtocolStatus {
    STANDBY,
    INITIALIZING,
    ACTIVE,
    COOLDOWN
}

object StarkProtocolsRepository {
    val defaultProtocols = listOf(
        StarkProtocol(
            id = "house_party",
            codeName = "PROTOCOL 42-HP",
            title = "House Party Protocol",
            description = "Emergency deployment of all autonomous Iron Man armors to your exact tactical coordinates.",
            voiceTrigger = "House Party Protocol",
            severityLevel = "OMEGA",
            suitUnits = listOf("Mark XLII (Autonomous)", "Mark XXXIII (Silver Centurion)", "Mark XXXVIII (Igor)", "Mark XVI (Nightclub)", "Mark XVII (Heartbreaker)", "Mark XL (Shotgun)")
        ),
        StarkProtocol(
            id = "veronica",
            codeName = "VERONICA ORBITAL",
            title = "Veronica (Hulkbuster)",
            description = "Commands orbital deployment pod 13-A to deliver the Mark XLIV Hulkbuster modular armor framework.",
            voiceTrigger = "Veronica",
            severityLevel = "HEAVY",
            suitUnits = listOf("Mark XLIV (Hulkbuster Main Frame)", "Orbital Drop Pod 13-A", "Magnetic Repulsor Clamps")
        ),
        StarkProtocol(
            id = "tactical_diag",
            codeName = "DIAG-SYS 9",
            title = "Tactical Diagnostics",
            description = "Deep sweep of Arc Reactor output, quantum telemetry, repulsor capacitance, and nanotech density.",
            voiceTrigger = "Tactical Diagnostics",
            severityLevel = "NORMAL",
            suitUnits = listOf("Arc Reactor Core IV", "Cryo-Fluid Manifold", "Optic HUD Sensors", "Nanite Assembly Bay")
        ),
        StarkProtocol(
            id = "clean_slate",
            codeName = "PROTOCOL ZERO",
            title = "Clean Slate Protocol",
            description = "High-security protocol to detonate micro-charges and purge non-essential autonomous armor cores.",
            voiceTrigger = "Clean Slate",
            severityLevel = "CRITICAL",
            suitUnits = listOf("Pyrotechnic Disconnectors", "Thermal Core Purge")
        ),
        StarkProtocol(
            id = "nightclub",
            codeName = "STEALTH-X",
            title = "Nightclub Protocol",
            description = "Radar-absorbing chromatic cloaking, silent repulsor damping, and thermal signature suppression.",
            voiceTrigger = "Nightclub",
            severityLevel = "TACTICAL",
            suitUnits = listOf("Mark XVI Nightclub", "Radar Camouflage Arrays")
        ),
        StarkProtocol(
            id = "barnett",
            codeName = "MED-SYNC 1",
            title = "Barnett Protocol",
            description = "Life support override: syncs vital signs, adrenaline telemetry, and emergency distress beacon.",
            voiceTrigger = "Barnett Protocol",
            severityLevel = "MEDICAL",
            suitUnits = listOf("Trauma Compression Mesh", "Subcutaneous Stim Injector")
        )
    )
}
