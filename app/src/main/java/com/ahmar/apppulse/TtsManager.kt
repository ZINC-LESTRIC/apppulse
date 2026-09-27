package com.ahmar.apppulse

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Simple singleton-style TTS manager.
 * Initialized lazily; speak() queues utterances with QUEUE_ADD.
 */
object TtsManager {
    private const val TAG = "TtsManager"

    private var tts: TextToSpeech? = null
    private val isReady = AtomicBoolean(false)
    private val isInitializing = AtomicBoolean(false)

    fun init(context: Context) {
        if (isReady.get() || isInitializing.getAndSet(true)) return

        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale.getDefault())
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.w(TAG, "TTS language not fully supported, falling back to US English")
                        tts?.setLanguage(Locale.US)
                    }
                    isReady.set(true)
                    Log.i(TAG, "TTS initialized successfully")
                } else {
                    Log.e(TAG, "TTS initialization failed with status $status")
                    isReady.set(false)
                }
                isInitializing.set(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while creating TextToSpeech", e)
            isReady.set(false)
            isInitializing.set(false)
        }
    }

    /**
     * Speak the given text. Uses QUEUE_ADD so multiple notifications are spoken in sequence.
     * Returns true if the utterance was successfully queued.
     */
    fun speak(text: String): Boolean {
        val engine = tts
        if (!isReady.get() || engine == null) {
            Log.w(TAG, "TTS not ready, skipping speak")
            return false
        }
        return try {
            val result = engine.speak(text, TextToSpeech.QUEUE_ADD, null, "apppulse_${System.currentTimeMillis()}")
            result == TextToSpeech.SUCCESS
        } catch (e: Exception) {
            Log.e(TAG, "Error speaking text", e)
            false
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS", e)
        }
        tts = null
        isReady.set(false)
        isInitializing.set(false)
    }
}
