package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ConversationLogEntity
import com.example.data.local.JarvisDatabase
import com.example.data.local.JarvisMemoryEntity
import com.example.data.local.SessionBackupManager
import com.example.data.local.UserProfileEntity
import com.example.data.model.ProtocolStatus
import com.example.data.model.StarkProtocol
import com.example.data.model.StarkProtocolsRepository
import com.example.data.remote.GeminiClient
import com.example.emotion.EmotionConfidence
import com.example.emotion.EmotionDetector
import com.example.emotion.EmotionalEstimate
import com.example.emotion.UserEmotion
import com.example.ui.theme.HudThemeId
import com.example.voice.JarvisVoiceEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

enum class ScreenDestination {
    DASHBOARD,
    LIVE_CALL,
    PROTOCOLS,
    SCANNER,
    THEMES,
    MEMORY
}

data class TacticalAnalysisReport(
    val targetClassification: String,
    val threatLevel: String, // MINIMAL, MODERATE, CRITICAL
    val confidence: String,
    val structuralComposition: String,
    val tacticalAdvice: String,
    val fullRawResponse: String
)

@OptIn(ExperimentalCoroutinesApi::class)
class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val db = JarvisDatabase.getDatabase(application)
    private val dao = db.jarvisDao()

    val voiceEngine = JarvisVoiceEngine(application, viewModelScope)

    // Current navigation screen
    private val _currentScreen = MutableStateFlow(ScreenDestination.DASHBOARD)
    val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

    // All available user profiles
    val allUserProfiles: StateFlow<List<UserProfileEntity>> = dao.getAllUserProfilesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently active user profile
    val userProfile: StateFlow<UserProfileEntity?> = dao.getActiveUserProfileFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Memories for current active user
    val memories: StateFlow<List<JarvisMemoryEntity>> = userProfile.flatMapLatest { profile ->
        if (profile != null) dao.getMemoriesForUserFlow(profile.userId)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Conversation history for current active user
    val conversations: StateFlow<List<ConversationLogEntity>> = userProfile.flatMapLatest { profile ->
        if (profile != null) dao.getRecentConversationsForUserFlow(profile.userId)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active HUD Theme
    private val _activeTheme = MutableStateFlow(HudThemeId.CLASSIC_CYAN)
    val activeTheme: StateFlow<HudThemeId> = _activeTheme.asStateFlow()

    // Backup Sync Status
    private val _backupStatus = MutableStateFlow("SESSION BACKUP: SYNCED")
    val backupStatus: StateFlow<String> = _backupStatus.asStateFlow()

    // Emotion-Aware Intelligence State
    private val _currentEmotion = MutableStateFlow(EmotionalEstimate(UserEmotion.NEUTRAL, EmotionConfidence.MEDIUM))
    val currentEmotion: StateFlow<EmotionalEstimate> = _currentEmotion.asStateFlow()

    // Stark Protocols state
    private val _protocols = MutableStateFlow(StarkProtocolsRepository.defaultProtocols)
    val protocols: StateFlow<List<StarkProtocol>> = _protocols.asStateFlow()

    // Live Voice Call State
    private val _isLiveCallActive = MutableStateFlow(false)
    val isLiveCallActive: StateFlow<Boolean> = _isLiveCallActive.asStateFlow()

    private val _callDurationSeconds = MutableStateFlow(0)
    val callDurationSeconds: StateFlow<Int> = _callDurationSeconds.asStateFlow()

    private val _liveCallStatus = MutableStateFlow("STANDBY")
    val liveCallStatus: StateFlow<String> = _liveCallStatus.asStateFlow()

    private val _lastTranscript = MutableStateFlow("")
    val lastTranscript: StateFlow<String> = _lastTranscript.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private var callTimerJob: Job? = null

    // Optical Camera Scanner State
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _latestAnalysis = MutableStateFlow<TacticalAnalysisReport?>(null)
    val latestAnalysis: StateFlow<TacticalAnalysisReport?> = _latestAnalysis.asStateFlow()

    private val _capturedBitmap = MutableStateFlow<Bitmap?>(null)
    val capturedBitmap: StateFlow<Bitmap?> = _capturedBitmap.asStateFlow()

    init {
        viewModelScope.launch {
            // Guard against page reloads and restarts:
            // 1. Check existing profiles in Room
            var active = dao.getActiveUserProfile()

            // 2. If null, attempt restore from permanent session backup storage (JSON + SharedPreferences)
            if (active == null) {
                active = SessionBackupManager.restoreBackupIfAvailable(getApplication(), dao)
            }

            // 3. If still empty, initialize primary default profile
            if (active == null) {
                val defaultProfile = UserProfileEntity(
                    userId = "user_default",
                    userName = "Sir",
                    formalTitle = "Sir",
                    roleOrDesignation = "Commander",
                    preferredLanguage = "Hinglish",
                    activeThemeId = "CLASSIC_CYAN",
                    isActiveUser = true
                )
                dao.saveUserProfile(defaultProfile)
                active = defaultProfile
                persistSessionBackup(active)
            }

            // Apply active theme
            try {
                _activeTheme.value = HudThemeId.valueOf(active.activeThemeId)
            } catch (_: Exception) {}

            // Proactive Recall & Greeting on Session Boot
            delay(600)
            val name = active.userName
            val greeting = if (name != "Sir") {
                "Welcome back, $name. Neural session restored from permanent storage. Jarvis is fully online."
            } else {
                "Jarvis online. Core systems and persistent memory architecture ready for your commands, Sir."
            }
            _lastTranscript.value = greeting
            voiceEngine.speak(greeting, active.preferredLanguage)
        }
    }

    private fun persistSessionBackup(activeProfile: UserProfileEntity?) {
        viewModelScope.launch {
            try {
                val allProfiles = dao.getAllUserProfiles()
                val allMemories = dao.getAllMemories()
                val recentLogs = dao.getAllConversations()
                SessionBackupManager.saveBackup(
                    context = getApplication(),
                    activeProfile = activeProfile,
                    allProfiles = allProfiles,
                    allMemories = allMemories,
                    recentLogs = recentLogs
                )
                _backupStatus.value = "BACKUP SYNCED (${activeProfile?.userName ?: "Sir"})"
            } catch (_: Exception) {}
        }
    }

    fun navigateTo(screen: ScreenDestination) {
        _currentScreen.value = screen
    }

    fun setTheme(themeId: HudThemeId) {
        _activeTheme.value = themeId
        viewModelScope.launch {
            val current = dao.getActiveUserProfile() ?: return@launch
            val updated = current.copy(activeThemeId = themeId.name, lastUpdated = System.currentTimeMillis())
            dao.saveUserProfile(updated)
            persistSessionBackup(updated)
        }
    }

    fun setPreferredLanguage(lang: String) {
        viewModelScope.launch {
            val current = dao.getActiveUserProfile() ?: return@launch
            val updated = current.copy(preferredLanguage = lang, lastUpdated = System.currentTimeMillis())
            dao.saveUserProfile(updated)
            persistSessionBackup(updated)
        }
    }

    fun setUserName(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val current = dao.getActiveUserProfile() ?: return@launch
            val updated = current.copy(userName = name.trim(), lastUpdated = System.currentTimeMillis())
            dao.saveUserProfile(updated)
            dao.insertMemory(
                JarvisMemoryEntity(
                    userId = updated.userId,
                    category = "IDENTITY",
                    fact = "User's stated name: ${updated.userName}"
                )
            )
            persistSessionBackup(updated)
            voiceEngine.speak("Identity updated and committed to permanent storage. Welcome, ${updated.userName}.", updated.preferredLanguage)
        }
    }

    // --- Multi-User Management ---

    fun switchActiveUser(userId: String) {
        viewModelScope.launch {
            dao.switchActiveUser(userId)
            val newActive = dao.getActiveUserProfile() ?: return@launch
            try {
                _activeTheme.value = HudThemeId.valueOf(newActive.activeThemeId)
            } catch (_: Exception) {}
            persistSessionBackup(newActive)
            val switchAnnouncement = "Profile switched to ${newActive.userName}. Personal preferences and historical memory dossier loaded."
            _lastTranscript.value = switchAnnouncement
            voiceEngine.speak(switchAnnouncement, newActive.preferredLanguage)
        }
    }

    fun createNewUserProfile(name: String, title: String = "Sir", language: String = "Hinglish") {
        if (name.isBlank()) return
        viewModelScope.launch {
            val cleanId = "user_" + name.lowercase().replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' } + "_" + UUID.randomUUID().toString().take(4)
            dao.clearActiveFlags()
            val newProfile = UserProfileEntity(
                userId = cleanId,
                userName = name.trim(),
                formalTitle = title,
                roleOrDesignation = "Guest Commander",
                preferredLanguage = language,
                activeThemeId = "CLASSIC_CYAN",
                isActiveUser = true
            )
            dao.saveUserProfile(newProfile)
            dao.insertMemory(
                JarvisMemoryEntity(
                    userId = cleanId,
                    category = "IDENTITY",
                    fact = "Profile initialized for ${newProfile.userName} ($title)"
                )
            )
            persistSessionBackup(newProfile)
            val announcement = "New neural profile initialized for ${newProfile.userName}. At your service."
            _lastTranscript.value = announcement
            voiceEngine.speak(announcement, language)
        }
    }

    fun deleteUserProfile(userId: String) {
        viewModelScope.launch {
            val currentActive = dao.getActiveUserProfile()
            dao.deleteUserProfile(userId)
            dao.clearMemoriesForUser(userId)
            dao.clearConversationsForUser(userId)
            if (currentActive?.userId == userId) {
                val remaining = dao.getAllUserProfiles()
                val fallback = remaining.firstOrNull() ?: UserProfileEntity(userId = "user_default", userName = "Sir", isActiveUser = true)
                dao.saveUserProfile(fallback.copy(isActiveUser = true))
                persistSessionBackup(fallback)
            }
        }
    }

    // --- Live Voice Call Controls ---

    fun startLiveCall() {
        _isLiveCallActive.value = true
        _callDurationSeconds.value = 0
        _liveCallStatus.value = "CONNECTING SECURE UPLINK..."
        voiceEngine.playTone(JarvisVoiceEngine.ToneType.CONFIRM)

        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            while (isActive && _isLiveCallActive.value) {
                delay(1000)
                _callDurationSeconds.value += 1
            }
        }

        viewModelScope.launch {
            delay(700)
            val profile = dao.getActiveUserProfile() ?: UserProfileEntity()
            val greeting = when {
                profile.preferredLanguage.contains("Hindi", ignoreCase = true) ->
                    "Namaste ${profile.userName}! Stark satellite link online hai. Aap kya aadesh dena chahenge?"
                profile.preferredLanguage.contains("Bangla", ignoreCase = true) ->
                    "Shubheccha ${profile.userName}, Stark link connect hoyeche. Ami apnar kotha shunchi."
                profile.preferredLanguage.contains("Hinglish", ignoreCase = true) ->
                    "Greetings ${profile.userName}! Jarvis online hai. Repulsor and comms systems all green. Kahiye Sir?"
                else ->
                    "At your service, ${profile.userName}. Secure audio channel established. What are your coordinates, Sir?"
            }
            _liveCallStatus.value = "JARVIS SPEAKING..."
            _lastTranscript.value = greeting
            voiceEngine.speak(greeting, profile.preferredLanguage) {
                if (_isLiveCallActive.value && !_isMuted.value) {
                    startListeningCycle()
                }
            }
        }
    }

    fun endLiveCall() {
        _isLiveCallActive.value = false
        callTimerJob?.cancel()
        voiceEngine.stopListening()
        voiceEngine.stopSpeaking()
        voiceEngine.playTone(JarvisVoiceEngine.ToneType.BEEP)
        _liveCallStatus.value = "DISCONNECTED"
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
        if (_isMuted.value) {
            voiceEngine.stopListening()
            _liveCallStatus.value = "MUTED"
        } else if (_isLiveCallActive.value) {
            startListeningCycle()
        }
    }

    fun triggerQuickVoiceInput() {
        val profile = userProfile.value ?: UserProfileEntity()
        voiceEngine.startListening(profile.preferredLanguage) { speechText ->
            processUserInput(speechText)
        }
    }

    private fun startListeningCycle() {
        if (!_isLiveCallActive.value || _isMuted.value) return
        _liveCallStatus.value = "LISTENING FOR YOUR VOICE..."
        val profile = userProfile.value ?: UserProfileEntity()
        voiceEngine.startListening(profile.preferredLanguage) { recognizedText ->
            _lastTranscript.value = recognizedText
            processUserInput(recognizedText)
        }
    }

    fun processUserInput(text: String) {
        if (text.isBlank()) return
        val profile = userProfile.value ?: UserProfileEntity()

        viewModelScope.launch {
            dao.insertConversation(
                ConversationLogEntity(
                    userId = profile.userId,
                    sender = "USER",
                    text = text,
                    isVoice = true
                )
            )

            // Dynamic User Profile & State Persistence check:
            checkAndCaptureUserIdentityAndMemories(text, profile)

            // Check if Stark Protocol voice command
            val triggeredProtocol = checkProtocolVoiceTrigger(text)
            if (triggeredProtocol != null) {
                activateProtocol(triggeredProtocol.id)
                return@launch
            }

            // Silently estimate user's emotional state
            val currentAmp = voiceEngine.amplitude.value
            val isVoice = voiceEngine.isListening.value || _isLiveCallActive.value
            val estimated = EmotionDetector.estimateEmotion(text, isVoiceInput = isVoice, amplitude = currentAmp)
            _currentEmotion.value = estimated

            // Auto-detect language if Bangla characters are present
            val isBanglaText = text.any { it in '\u0980'..'\u09FF' }
            val effectiveLang = if (isBanglaText) "Bangla" else profile.preferredLanguage

            _liveCallStatus.value = "PROCESSING QUERY..."

            val memoryList = dao.getMemoriesForUser(profile.userId).map { "${it.category}: ${it.fact}" }
            val jarvisResponse = GeminiClient.queryJarvis(
                userPrompt = text,
                userName = profile.userName,
                userMemories = memoryList,
                preferredLanguage = effectiveLang,
                estimatedEmotion = estimated.emotion,
                emotionConfidence = estimated.confidence
            )

            dao.insertConversation(
                ConversationLogEntity(
                    userId = profile.userId,
                    sender = "JARVIS",
                    text = jarvisResponse,
                    isVoice = true
                )
            )
            _lastTranscript.value = jarvisResponse
            _liveCallStatus.value = "JARVIS SPEAKING..."

            // Flush state to permanent session backup
            persistSessionBackup(profile.copy(totalInteractions = profile.totalInteractions + 1))

            // Adapt voice pitch and speed based on estimated emotion
            val adaptivePitch = estimated.emotion.pitchModifier
            val adaptiveSpeed = estimated.emotion.speedModifier

            voiceEngine.speak(jarvisResponse, effectiveLang, pitch = adaptivePitch, speechRate = adaptiveSpeed) {
                if (_isLiveCallActive.value && !_isMuted.value && profile.isAutoListenEnabled) {
                    startListeningCycle()
                } else {
                    _liveCallStatus.value = "STANDBY"
                }
            }
        }
    }

    private suspend fun checkAndCaptureUserIdentityAndMemories(text: String, currentProfile: UserProfileEntity) {
        val lower = text.lowercase()

        // 1. Dynamic User Name Recognition & Switching / Creation
        val namePatterns = listOf(
            "my name is ", "mera naam ", "call me ", "i am ", "amar naam ", "switch user to ", "switch to "
        )

        for (pattern in namePatterns) {
            if (lower.contains(pattern)) {
                val extracted = text.substring(lower.indexOf(pattern) + pattern.length)
                    .trim()
                    .takeWhile { it.isLetterOrDigit() || it.isWhitespace() }
                    .trim()

                if (extracted.isNotBlank() && extracted.length in 2..30 && !extracted.equals(currentProfile.userName, ignoreCase = true)) {
                    // Check if a profile with this name already exists
                    val existing = dao.getUserProfileByName(extracted)
                    if (existing != null) {
                        dao.switchActiveUser(existing.userId)
                        persistSessionBackup(existing.copy(isActiveUser = true))
                    } else {
                        // Create and switch to new profile
                        val newId = "user_" + extracted.lowercase().replace(" ", "_") + "_" + UUID.randomUUID().toString().take(4)
                        dao.clearActiveFlags()
                        val newProf = UserProfileEntity(
                            userId = newId,
                            userName = extracted,
                            formalTitle = "Sir",
                            preferredLanguage = currentProfile.preferredLanguage,
                            activeThemeId = currentProfile.activeThemeId,
                            isActiveUser = true
                        )
                        dao.saveUserProfile(newProf)
                        dao.insertMemory(
                            JarvisMemoryEntity(
                                userId = newId,
                                category = "IDENTITY",
                                fact = "Stated name: $extracted"
                            )
                        )
                        persistSessionBackup(newProf)
                    }
                    return
                }
            }
        }

        // 2. Preferences
        if (lower.contains("i like ") || lower.contains("mujhe pasand hai ") || lower.contains("i prefer ") || lower.contains("amar pochondo ")) {
            val memory = JarvisMemoryEntity(userId = currentProfile.userId, category = "PREFERENCE", fact = text.trim())
            dao.insertMemory(memory)
            persistSessionBackup(currentProfile)
        }
        // 3. Projects & Work
        else if (lower.contains("i am working on ") || lower.contains("my project is ") || lower.contains("mera project ") || lower.contains("amar project ")) {
            val memory = JarvisMemoryEntity(userId = currentProfile.userId, category = "PROJECT", fact = text.trim())
            dao.insertMemory(memory)
            persistSessionBackup(currentProfile)
        }
        // 4. Instructions & Constraints
        else if (lower.contains("always ") || lower.contains("remember to ") || lower.contains("yaad rakhna ") || lower.contains("mone rakhbe ")) {
            val memory = JarvisMemoryEntity(userId = currentProfile.userId, category = "INSTRUCTION", fact = text.trim())
            dao.insertMemory(memory)
            persistSessionBackup(currentProfile)
        }
        // 5. Task History & Notes
        else if (lower.contains("task finished ") || lower.contains("completed ") || lower.contains("note that ") || lower.contains("deadline is ")) {
            val memory = JarvisMemoryEntity(userId = currentProfile.userId, category = "TASK_HISTORY", fact = text.trim())
            dao.insertMemory(memory)
            persistSessionBackup(currentProfile)
        }
    }

    private fun checkProtocolVoiceTrigger(text: String): StarkProtocol? {
        val lower = text.lowercase()
        return _protocols.value.firstOrNull { protocol ->
            lower.contains(protocol.voiceTrigger.lowercase()) ||
            lower.contains(protocol.title.lowercase())
        }
    }

    // --- Stark Protocols Execution ---

    fun activateProtocol(protocolId: String) {
        val protocol = _protocols.value.firstOrNull { it.id == protocolId } ?: return
        voiceEngine.playTone(JarvisVoiceEngine.ToneType.ALERT)

        _protocols.value = _protocols.value.map {
            if (it.id == protocolId) it.copy(status = ProtocolStatus.INITIALIZING) else it
        }

        viewModelScope.launch {
            val profile = dao.getActiveUserProfile() ?: UserProfileEntity()
            val updated = profile.copy(totalProtocolsInitiated = profile.totalProtocolsInitiated + 1)
            dao.saveUserProfile(updated)
            persistSessionBackup(updated)

            val announce = "Initiating ${protocol.title}, ${profile.userName}. Engaging telemetry subroutines."
            _lastTranscript.value = announce
            voiceEngine.speak(announce, profile.preferredLanguage)

            delay(2500)
            voiceEngine.playTone(JarvisVoiceEngine.ToneType.SHIELD_LOCK)

            _protocols.value = _protocols.value.map {
                if (it.id == protocolId) it.copy(status = ProtocolStatus.ACTIVE) else it
            }

            val confirmMsg = "${protocol.title} is now FULLY ACTIVE. All designated units deployed."
            _lastTranscript.value = confirmMsg
            voiceEngine.speak(confirmMsg, profile.preferredLanguage)
        }
    }

    fun resetProtocol(protocolId: String) {
        _protocols.value = _protocols.value.map {
            if (it.id == protocolId) it.copy(status = ProtocolStatus.STANDBY) else it
        }
        voiceEngine.playTone(JarvisVoiceEngine.ToneType.CONFIRM)
    }

    // --- Optical Camera Scanner ---

    fun setCapturedImage(bitmap: Bitmap) {
        _capturedBitmap.value = bitmap
        analyzeTarget(bitmap)
    }

    fun analyzeTarget(bitmap: Bitmap) {
        _isScanning.value = true
        _latestAnalysis.value = null
        voiceEngine.playTone(JarvisVoiceEngine.ToneType.ALERT)

        viewModelScope.launch {
            val profile = dao.getActiveUserProfile() ?: UserProfileEntity()
            val prompt = """
                You are J.A.R.V.I.S. conducting an immediate tactical HUD scan of this image for ${profile.userName}.
                Provide a structured Iron Man style tactical readout:
                1. TARGET CLASSIFICATION: (Short name and category)
                2. THREAT ASSESSMENT: (MINIMAL / MODERATE / HIGH / CRITICAL)
                3. STRUCTURAL COMPOSITION: (Materials, weak points, or environmental conditions)
                4. TACTICAL RECOMMENDATION: (Immediate action for ${profile.userName} in 1-2 punchy sentences)
            """.trimIndent()

            val rawResult = GeminiClient.queryJarvis(
                userPrompt = prompt,
                userName = profile.userName,
                userMemories = emptyList(),
                preferredLanguage = profile.preferredLanguage,
                image = bitmap
            )

            val parsedReport = parseTacticalReport(rawResult)
            _latestAnalysis.value = parsedReport
            _isScanning.value = false
            voiceEngine.playTone(JarvisVoiceEngine.ToneType.CONFIRM)

            val vocalSummary = "Tactical scan complete, ${profile.userName}. Target classified as ${parsedReport.targetClassification}. Threat level: ${parsedReport.threatLevel}."
            voiceEngine.speak(vocalSummary, profile.preferredLanguage)
        }
    }

    private fun parseTacticalReport(raw: String): TacticalAnalysisReport {
        var classification = "Unknown Entity"
        var threat = "MODERATE"
        var composition = "Organic / Synthetic Composite"
        var advice = "Maintain tactical distance and observe telemetry."

        val lines = raw.lines()
        for (line in lines) {
            val upper = line.uppercase()
            if (upper.contains("CLASSIFICATION") || upper.contains("TARGET")) {
                classification = line.substringAfter(":").trim().ifEmpty { classification }
            } else if (upper.contains("THREAT")) {
                threat = when {
                    upper.contains("CRITICAL") || upper.contains("LETHAL") || upper.contains("HIGH") -> "CRITICAL"
                    upper.contains("MODERATE") || upper.contains("MEDIUM") -> "MODERATE"
                    else -> "MINIMAL"
                }
            } else if (upper.contains("COMPOSITION") || upper.contains("STRUCTURAL")) {
                composition = line.substringAfter(":").trim().ifEmpty { composition }
            } else if (upper.contains("RECOMMENDATION") || upper.contains("ACTION")) {
                advice = line.substringAfter(":").trim().ifEmpty { advice }
            }
        }

        return TacticalAnalysisReport(
            targetClassification = classification,
            threatLevel = threat,
            confidence = "98.4%",
            structuralComposition = composition,
            tacticalAdvice = advice,
            fullRawResponse = raw
        )
    }

    fun addCustomMemory(category: String, fact: String) {
        if (fact.isBlank()) return
        viewModelScope.launch {
            val profile = dao.getActiveUserProfile() ?: return@launch
            val memory = JarvisMemoryEntity(userId = profile.userId, category = category.uppercase(), fact = fact.trim())
            dao.insertMemory(memory)
            persistSessionBackup(profile)
            voiceEngine.speak("Recorded into neural backup for ${profile.userName}.", profile.preferredLanguage)
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            dao.deleteMemory(id)
            val profile = dao.getActiveUserProfile()
            persistSessionBackup(profile)
        }
    }

    fun clearAllMemoriesForActiveUser() {
        viewModelScope.launch {
            val profile = dao.getActiveUserProfile() ?: return@launch
            dao.clearMemoriesForUser(profile.userId)
            persistSessionBackup(profile)
        }
    }

    fun triggerManualBackupSync() {
        val profile = userProfile.value
        persistSessionBackup(profile)
        voiceEngine.playTone(JarvisVoiceEngine.ToneType.CONFIRM)
    }

    override fun onCleared() {
        super.onCleared()
        voiceEngine.destroy()
    }
}
