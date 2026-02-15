// ABOUTME: Controller for voice-driven mode (speak next todo and listen for "check"/"skip").
// ABOUTME: Uses TextToSpeech + SpeechRecognizer and only navigates top-level unchecked todos.
package com.example.niche_todos

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class VoiceDrivenModeController(
    private val context: Context,
    private val promptTextProvider: () -> String,
    private val listCompletedTextProvider: () -> String,
    private val permissionDeniedTextProvider: () -> String,
    private val getVisibleTodos: () -> List<Todo>,
    private val toggleComplete: (String) -> Unit,
    private val onUserMessage: (String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTtsReady: Boolean = false
    private var running: Boolean = false

    private var lastTodoId: String? = null
    private var currentTodoId: String? = null
    private val pendingCheckedIds: MutableSet<String> = mutableSetOf()

    private enum class Phase {
        Idle,
        Speaking,
        Listening
    }

    private var phase: Phase = Phase.Idle
    private var consecutiveErrors: Int = 0

    fun isRunning(): Boolean = running

    fun start(): Boolean {
        if (running) return true
        if (!hasRecordAudioPermission()) {
            onUserMessage(permissionDeniedTextProvider())
            return false
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            return false
        }
        Log.d(TAG, "start")
        running = true
        ensureTts()
        ensureSpeechRecognizer()
        advance()
        return true
    }

    fun stop() {
        Log.d(TAG, "stop")
        running = false
        phase = Phase.Idle
        currentTodoId = null
        lastTodoId = null
        pendingCheckedIds.clear()
        speechRecognizer?.cancel()
        tts?.stop()
    }

    fun shutdown() {
        stop()
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.shutdown()
        tts = null
        isTtsReady = false
    }

    fun onVisibleTodosChanged() {
        if (!running) return

        val visible = getVisibleTodos()
        val visibleById = visible.associateBy { it.id }
        val resolved = pendingCheckedIds.filter { id ->
            val todo = visibleById[id]
            todo == null || todo.isCompleted
        }
        pendingCheckedIds.removeAll(resolved.toSet())

        if (phase == Phase.Idle && currentTodoId == null) {
            advance()
        }
    }

    fun debugSimulatePhrase(phrase: String) {
        if (!running) return
        handleRecognizedPhrases(listOf(phrase))
    }

    private fun ensureTts() {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            isTtsReady = status == TextToSpeech.SUCCESS
            if (isTtsReady) {
                tts?.language = Locale.US
            }
            if (running) {
                advance()
            }
        }.apply {
            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    phase = Phase.Speaking
                }

                override fun onDone(utteranceId: String?) {
                    if (!running) return
                    when (utteranceId) {
                        UTTERANCE_ITEM -> {
                            Log.d(TAG, "tts done (item), start listening")
                            // SpeechRecognizer must be driven from the main thread.
                            mainHandler.post { startListening() }
                        }
                        UTTERANCE_COMPLETED -> {
                            // Stay idle. If list changes, onVisibleTodosChanged() will advance again.
                            Log.d(TAG, "tts done (completed)")
                            phase = Phase.Idle
                            currentTodoId = null
                        }
                        else -> advance()
                    }
                }

                override fun onError(utteranceId: String?) {
                    if (!running) return
                    phase = Phase.Idle
                    advance()
                }
            })
        }
    }

    private fun ensureSpeechRecognizer() {
        if (speechRecognizer != null) return
        val appContext = context.applicationContext
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d(TAG, "onReadyForSpeech")
                }

                override fun onBeginningOfSpeech() {
                    Log.d(TAG, "onBeginningOfSpeech")
                }

                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    Log.d(TAG, "onEndOfSpeech")
                }

                override fun onError(error: Int) {
                    if (!running) return
                    consecutiveErrors += 1
                    Log.d(TAG, "onError=$error consecutiveErrors=$consecutiveErrors")
                    phase = Phase.Idle
                    // Reprompt by re-speaking the current item.
                    currentTodoId?.let { _ -> speakCurrentTodoAgain() } ?: advance()
                }

                override fun onResults(results: Bundle?) {
                    if (!running) return
                    consecutiveErrors = 0
                    val phrases = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        .orEmpty()
                    handleRecognizedPhrases(phrases)
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun handleRecognizedPhrases(phrases: List<String>) {
        val normalized = phrases.joinToString(" ").lowercase(Locale.US)
        Log.d(TAG, "recognized: $normalized")
        when {
            normalized.contains("check") -> {
                val todoId = currentTodoId
                if (todoId != null) {
                    pendingCheckedIds.add(todoId)
                    toggleComplete(todoId)
                    lastTodoId = todoId
                }
                currentTodoId = null
                phase = Phase.Idle
                advance()
            }

            normalized.contains("skip") -> {
                lastTodoId = currentTodoId ?: lastTodoId
                currentTodoId = null
                phase = Phase.Idle
                advance()
            }

            else -> {
                phase = Phase.Idle
                speakCurrentTodoAgain()
            }
        }
    }

    private fun startListening() {
        if (!running) return
        if (speechRecognizer == null) return
        if (!hasRecordAudioPermission()) {
            Log.d(TAG, "startListening: missing RECORD_AUDIO permission")
            onUserMessage(permissionDeniedTextProvider())
            stop()
            return
        }
        phase = Phase.Listening
        Log.d(TAG, "startListening")
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 900L)
        }
        speechRecognizer?.cancel()
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: SecurityException) {
            Log.d(TAG, "startListening: SecurityException", e)
            onUserMessage(permissionDeniedTextProvider())
            stop()
        }
    }

    private fun hasRecordAudioPermission(): Boolean {
        return context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun advance() {
        if (!running) return
        if (!isTtsReady) return
        if (phase != Phase.Idle) return

        val visible = getVisibleTodos()
        val effectiveVisible = visible.filterNot { pendingCheckedIds.contains(it.id) }
        val next = VoiceDrivenTodoNavigator.nextTopLevelUnchecked(
            visibleTodos = effectiveVisible,
            lastTodoId = lastTodoId
        )

        if (next == null) {
            Log.d(TAG, "no candidates; speaking completed")
            val completedText = listCompletedTextProvider()
            tts?.speak(completedText, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_COMPLETED)
            return
        }

        currentTodoId = next.id
        Log.d(TAG, "speaking todoId=${next.id} title=${next.title}")
        val title = next.title.ifBlank { "Untitled todo" }
        val prompt = promptTextProvider()
        val speech = "$title. $prompt."
        tts?.speak(speech, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ITEM)
    }

    private fun speakCurrentTodoAgain() {
        if (!running) return
        val id = currentTodoId ?: return
        val todo = getVisibleTodos().firstOrNull { it.id == id }
        if (todo == null) {
            currentTodoId = null
            phase = Phase.Idle
            advance()
            return
        }
        phase = Phase.Idle
        val title = todo.title.ifBlank { "Untitled todo" }
        val prompt = promptTextProvider()
        val speech = "$title. $prompt."
        tts?.speak(speech, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ITEM)
    }

    private companion object {
        private const val TAG = "VoiceMode"
        private const val UTTERANCE_ITEM = "voice_item"
        private const val UTTERANCE_COMPLETED = "voice_completed"
    }
}
