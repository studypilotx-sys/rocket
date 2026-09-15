package com.studypilot.app.guardian

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Manages native Android TextToSpeech for Focus Guardian voice alerts.
 * Speaks the specified alert: "Please return to your study session."
 */
class VoiceReminderManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                isInitialized = true
            }
        }
    }

    fun speakWarning(onComplete: (() -> Unit)? = null) {
        val engine = tts ?: run {
            onComplete?.invoke()
            return
        }

        if (!isInitialized) {
            onComplete?.invoke()
            return
        }

        val utteranceId = "guardian_absence_warning_${System.currentTimeMillis()}"

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) {
                if (id == utteranceId) {
                    onComplete?.invoke()
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                if (id == utteranceId) {
                    onComplete?.invoke()
                }
            }
        })

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        engine.speak(
            "Please return to your study session.",
            TextToSpeech.QUEUE_FLUSH,
            params,
            utteranceId
        )
    }

    /**
     * Speaks the absence warning and suspends until speech is finished (or timeout/cancel).
     */
    suspend fun speakWarningAwait(): Boolean = withTimeoutOrNull(4000L) {
        suspendCancellableCoroutine { cont ->
            val engine = tts
            if (engine == null || !isInitialized) {
                if (cont.isActive) cont.resume(true)
                return@suspendCancellableCoroutine
            }

            val utteranceId = "guardian_absence_warning_${System.currentTimeMillis()}"

            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {}
                override fun onDone(id: String?) {
                    if (id == utteranceId && cont.isActive) {
                        cont.resume(true)
                    }
                }
                @Deprecated("Deprecated in Java")
                override fun onError(id: String?) {
                    if (id == utteranceId && cont.isActive) {
                        cont.resume(false)
                    }
                }
            })

            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }

            val result = engine.speak(
                "Please return to your study session.",
                TextToSpeech.QUEUE_FLUSH,
                params,
                utteranceId
            )

            if (result != TextToSpeech.SUCCESS && cont.isActive) {
                cont.resume(false)
            }

            cont.invokeOnCancellation {
                try {
                    engine.stop()
                } catch (_: Exception) {}
            }
        }
    } ?: false

    fun stop() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        isInitialized = false
    }
}
