package com.example.voice

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sin

class JarvisVoiceEngine(
    private val context: Context,
    private val scope: CoroutineScope
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListeningInternal = false

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private var amplitudeSimulationJob: Job? = null

    private var onSpeechResultCallback: ((String) -> Unit)? = null
    private var onSpeakingFinishedCallback: (() -> Unit)? = null

    private var toneGen: ToneGenerator? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
        try {
            toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 60)
        } catch (_: Exception) {}
        initSpeechRecognizer()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            tts?.apply {
                language = Locale.UK
                setPitch(0.95f)
                setSpeechRate(1.02f)
                setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                        startSimulatedSpeakingAmplitude()
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        stopSimulatedSpeakingAmplitude()
                        scope.launch(Dispatchers.Main) {
                            onSpeakingFinishedCallback?.invoke()
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        stopSimulatedSpeakingAmplitude()
                        scope.launch(Dispatchers.Main) {
                            onSpeakingFinishedCallback?.invoke()
                        }
                    }
                })
            }
        }
    }

    private fun initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return

        scope.launch(Dispatchers.Main) {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _isListening.value = true
                        }

                        override fun onBeginningOfSpeech() {}

                        override fun onRmsChanged(rmsdB: Float) {
                            // rmsdB typically ranges from -2 to 10
                            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1f)
                            _amplitude.value = normalized
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            _isListening.value = false
                            _amplitude.value = 0f
                        }

                        override fun onError(error: Int) {
                            _isListening.value = false
                            isListeningInternal = false
                            _amplitude.value = 0f
                        }

                        override fun onResults(results: Bundle?) {
                            _isListening.value = false
                            isListeningInternal = false
                            _amplitude.value = 0f
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val recognizedText = matches?.firstOrNull()
                            if (!recognizedText.isNullOrBlank()) {
                                onSpeechResultCallback?.invoke(recognizedText)
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull()
                            if (!text.isNullOrBlank()) {
                                // Can broadcast partial if needed
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
            } catch (_: Exception) {}
        }
    }

    fun startListening(languagePreference: String, onResult: (String) -> Unit) {
        if (_isSpeaking.value) {
            stopSpeaking()
        }
        onSpeechResultCallback = onResult

        scope.launch(Dispatchers.Main) {
            try {
                if (speechRecognizer == null) {
                    initSpeechRecognizer()
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)

                    val targetLocale = when {
                        languagePreference.contains("Hindi", ignoreCase = true) -> "hi-IN"
                        languagePreference.contains("Bangla", ignoreCase = true) -> "bn-BD"
                        languagePreference.contains("Hinglish", ignoreCase = true) -> "en-IN"
                        else -> Locale.getDefault().toLanguageTag()
                    }
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, targetLocale)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, targetLocale)
                }

                speechRecognizer?.startListening(intent)
                isListeningInternal = true
                _isListening.value = true
                playTone(ToneType.BEEP)
            } catch (_: Exception) {
                _isListening.value = false
            }
        }
    }

    fun stopListening() {
        scope.launch(Dispatchers.Main) {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {}
            _isListening.value = false
            isListeningInternal = false
            _amplitude.value = 0f
        }
    }

    fun speak(
        text: String,
        languagePreference: String,
        pitch: Float = 0.96f,
        speechRate: Float = 1.02f,
        onFinished: (() -> Unit)? = null
    ) {
        onSpeakingFinishedCallback = onFinished
        if (!isTtsReady || tts == null) {
            onFinished?.invoke()
            return
        }

        // Configure speech language
        try {
            val locale = when {
                languagePreference.contains("Hindi", ignoreCase = true) -> Locale.forLanguageTag("hi-IN")
                languagePreference.contains("Bangla", ignoreCase = true) -> Locale.forLanguageTag("bn-BD")
                languagePreference.contains("Hinglish", ignoreCase = true) -> Locale.forLanguageTag("en-IN")
                else -> Locale.UK
            }
            tts?.language = locale
            tts?.setPitch(pitch)
            tts?.setSpeechRate(speechRate)
        } catch (_: Exception) {
            tts?.language = Locale.UK
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_speech_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
        _isSpeaking.value = false
        stopSimulatedSpeakingAmplitude()
    }

    private fun startSimulatedSpeakingAmplitude() {
        amplitudeSimulationJob?.cancel()
        amplitudeSimulationJob = scope.launch(Dispatchers.Default) {
            var tick = 0f
            while (isActive && _isSpeaking.value) {
                tick += 0.25f
                // Wave formula combining multiple harmonic frequencies to mimic speech fluctuations
                val wave = (sin(tick.toDouble()) * 0.4 + sin((tick * 2.3).toDouble()) * 0.3 + 0.3).toFloat().coerceIn(0.15f, 0.95f)
                _amplitude.value = wave
                delay(40)
            }
            _amplitude.value = 0f
        }
    }

    private fun stopSimulatedSpeakingAmplitude() {
        amplitudeSimulationJob?.cancel()
        amplitudeSimulationJob = null
        _amplitude.value = 0f
    }

    enum class ToneType {
        BEEP,
        CONFIRM,
        ALERT,
        SHIELD_LOCK
    }

    fun playTone(type: ToneType) {
        try {
            when (type) {
                ToneType.BEEP -> toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
                ToneType.CONFIRM -> toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 120)
                ToneType.ALERT -> toneGen?.startTone(ToneGenerator.TONE_PROP_PROMPT, 200)
                ToneType.SHIELD_LOCK -> toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 180)
            }
        } catch (_: Exception) {}
    }

    fun destroy() {
        try {
            tts?.shutdown()
            speechRecognizer?.destroy()
            toneGen?.release()
        } catch (_: Exception) {}
    }
}
