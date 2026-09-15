package com.studypilot.app.guardian

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.studypilot.app.data.model.GuardianVolumeLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Native Android Reveille bugle alarm player using synthesized PCM over AudioTrack.
 * Guaranteed to play reliably on any Android device without needing external audio files.
 */
class ReveillePlayer {

    private var audioTrack: AudioTrack? = null
    private var loopJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    @Volatile
    private var isPlaying = false

    // Classic Reveille bugle call frequencies (in Hz)
    // G3 = 196Hz, C4 = 261.63Hz, E4 = 329.63Hz, G4 = 392Hz, C5 = 523.25Hz
    private val noteG3 = 196.0
    private val noteC4 = 261.63
    private val noteE4 = 329.63
    private val noteG4 = 392.0
    private val noteC5 = 523.25

    // Reveille call sequence: (frequency, durationMs)
    private val reveilleMotif = listOf(
        Pair(noteG3, 140),
        Pair(noteC4, 280),
        Pair(noteE4, 140),
        Pair(noteC4, 140),
        Pair(noteG3, 140),
        Pair(noteC4, 280),
        Pair(noteE4, 140),
        Pair(noteC4, 140),
        Pair(noteG3, 140),
        Pair(noteC4, 140),
        Pair(noteE4, 140),
        Pair(noteG4, 280),
        Pair(noteE4, 140),
        Pair(noteC4, 280),
        Pair(noteE4, 140),
        Pair(noteC4, 140),
        Pair(noteG3, 280),
        Pair(0.0, 100), // rest
        Pair(noteC4, 140),
        Pair(noteE4, 140),
        Pair(noteG4, 140),
        Pair(noteC5, 380)
    )

    fun start(volumeLevel: GuardianVolumeLevel) {
        if (isPlaying) return
        isPlaying = true

        val gain = when (volumeLevel) {
            GuardianVolumeLevel.LOW -> 0.35f
            GuardianVolumeLevel.MEDIUM -> 0.70f
            GuardianVolumeLevel.HIGH -> 1.0f
        }

        loopJob?.cancel()
        loopJob = scope.launch {
            val sampleRate = 44100
            val pcmData = generateReveillePcm(sampleRate, gain)

            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(pcmData.size * 2)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack = track

            try {
                track.play()
                while (isActive && isPlaying) {
                    track.write(pcmData, 0, pcmData.size)
                    // Short 800ms gap between reveille repeats
                    val silenceSamples = (sampleRate * 0.8).toInt()
                    val silence = ShortArray(silenceSamples)
                    track.write(silence, 0, silence.size)
                }
            } catch (e: Exception) {
                // Audio interrupted or cancelled
            } finally {
                try {
                    track.stop()
                    track.flush()
                    track.release()
                } catch (_: Exception) {}
            }
        }
    }

    fun stop() {
        isPlaying = false
        loopJob?.cancel()
        loopJob = null
        try {
            audioTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                    stop()
                }
                flush()
                release()
            }
        } catch (_: Exception) {}
        audioTrack = null
    }

    private fun generateReveillePcm(sampleRate: Int, gain: Float): ShortArray {
        var totalSamples = 0
        for ((_, durMs) in reveilleMotif) {
            totalSamples += (sampleRate * durMs / 1000.0).toInt()
        }

        val result = ShortArray(totalSamples)
        var cursor = 0

        for ((freq, durMs) in reveilleMotif) {
            val noteSamples = (sampleRate * durMs / 1000.0).toInt()
            if (freq <= 0.0) {
                // Rest note
                cursor += noteSamples
                continue
            }

            val angularFreq = 2.0 * PI * freq
            for (i in 0 until noteSamples) {
                val time = i.toDouble() / sampleRate
                // Attack-Decay envelope to simulate brass horn sound
                val attackSamples = (noteSamples * 0.15).coerceAtLeast(1.0)
                val decaySamples = (noteSamples * 0.20).coerceAtLeast(1.0)
                val envelope = when {
                    i < attackSamples -> i / attackSamples
                    i > noteSamples - decaySamples -> (noteSamples - i) / decaySamples
                    else -> 1.0
                }

                // Fundamental + 2nd and 3rd harmonics for brassy bugle character
                val signal = (sin(angularFreq * time)
                        + 0.45 * sin(2 * angularFreq * time)
                        + 0.20 * sin(3 * angularFreq * time)) * envelope * gain

                val sampleVal = (signal * 24000.0).coerceIn(
                    Short.MIN_VALUE.toDouble(),
                    Short.MAX_VALUE.toDouble()
                ).toInt().toShort()

                if (cursor < result.size) {
                    result[cursor++] = sampleVal
                }
            }
        }

        return result
    }
}
