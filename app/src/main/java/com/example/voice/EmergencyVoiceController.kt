package com.example.voice

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class EmergencyVoiceController(
    private val context: Context,
    private val onCommandRecognized: (String) -> Unit
) : TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _recognizedTranscript = MutableStateFlow("")
    val recognizedTranscript: StateFlow<String> = _recognizedTranscript.asStateFlow()

    private val _audioFeedbackMessage = MutableStateFlow("")
    val audioFeedbackMessage: StateFlow<String> = _audioFeedbackMessage.asStateFlow()

    init {
        try {
            textToSpeech = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e("VoiceController", "Failed to init TextToSpeech", e)
        }
        setupSpeechRecognizer()
    }

    private fun setupSpeechRecognizer() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _isListening.value = true
                        }
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {
                            _isListening.value = false
                        }
                        override fun onError(error: Int) {
                            _isListening.value = false
                            Log.w("VoiceController", "SpeechRecognizer error: $error")
                        }
                        override fun onResults(results: Bundle?) {
                            _isListening.value = false
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val recognized = matches?.firstOrNull() ?: ""
                            if (recognized.isNotBlank()) {
                                _recognizedTranscript.value = recognized
                                onCommandRecognized(recognized)
                            }
                        }
                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val partial = matches?.firstOrNull() ?: ""
                            if (partial.isNotBlank()) {
                                _recognizedTranscript.value = partial
                            }
                        }
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceController", "Failed to setup speech recognizer", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.language = Locale.US
            isTtsInitialized = true
        }
    }

    fun startListening() {
        if (_isListening.value) return
        _recognizedTranscript.value = ""
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        try {
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            _isListening.value = false
            Log.e("VoiceController", "Could not start speech recognition", e)
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        _isListening.value = false
    }

    fun triggerSimulatedVoiceCommand(spokenText: String) {
        _recognizedTranscript.value = spokenText
        onCommandRecognized(spokenText)
    }

    fun speak(text: String) {
        _audioFeedbackMessage.value = text
        if (isTtsInitialized && textToSpeech != null) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "asvasa_tts")
        }
    }

    fun dialPhoneNumber(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("VoiceController", "Error opening dialer for $phoneNumber", e)
        }
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (_: Exception) {}
    }
}
