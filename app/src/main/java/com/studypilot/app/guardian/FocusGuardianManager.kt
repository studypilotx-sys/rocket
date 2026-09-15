package com.studypilot.app.guardian

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.studypilot.app.data.model.GuardianAlertType
import com.studypilot.app.data.model.GuardianSettings
import com.studypilot.app.data.model.PresenceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

enum class GuardianWarningStage {
    NONE,
    VOICE_WARNING,
    REVEILLE_ALARM
}

/**
 * Focus Guardian Core Manager.
 * Orchestrates native CameraX front preview, on-device presence detection,
 * debounce stabilization, TTS voice warning, Reveille bugle audio, and vibration.
 */
class FocusGuardianManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private val reveillePlayer = ReveillePlayer()
    private val voiceReminderManager = VoiceReminderManager(context)
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private var cameraProvider: ProcessCameraProvider? = null

    // State flows
    private val _presenceState = MutableStateFlow(PresenceState.UNKNOWN)
    val presenceState: StateFlow<PresenceState> = _presenceState.asStateFlow()

    private val _warningStage = MutableStateFlow(GuardianWarningStage.NONE)
    val warningStage: StateFlow<GuardianWarningStage> = _warningStage.asStateFlow()

    private val _absenceCountdown = MutableStateFlow(0)
    val absenceCountdown: StateFlow<Int> = _absenceCountdown.asStateFlow()

    private val _isCameraActive = MutableStateFlow(false)
    val isCameraActive: StateFlow<Boolean> = _isCameraActive.asStateFlow()

    private val _isMonitoringPaused = MutableStateFlow(false)
    val isMonitoringPaused: StateFlow<Boolean> = _isMonitoringPaused.asStateFlow()

    private val _interruptionCount = MutableStateFlow(0)
    val interruptionCount: StateFlow<Int> = _interruptionCount.asStateFlow()

    private val _absenceEventsCount = MutableStateFlow(0)
    val absenceEventsCount: StateFlow<Int> = _absenceEventsCount.asStateFlow()

    private var guardianSettings = GuardianSettings()

    // Internal debounce counters
    private var consecutivePresentSamples = 0
    private var consecutiveAbsentSamples = 0
    private var absenceStabilizationJob: Job? = null
    private var warningSequenceJob: Job? = null

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun updateSettings(settings: GuardianSettings) {
        guardianSettings = settings
        if (!settings.isGuardianEnabled) {
            stopAllAlerts()
        }
    }

    /**
     * Attaches CameraX front camera preview and on-device presence analyzer.
     */
    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onError: (String) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                provider.unbindAll()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(
                    cameraExecutor,
                    PresenceAnalyzer { isPersonDetected ->
                        handlePresenceSample(isPersonDetected)
                    }
                )

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )

                _isCameraActive.value = true
                _isMonitoringPaused.value = false
            } catch (e: Exception) {
                _isCameraActive.value = false
                onError(e.localizedMessage ?: "Camera initialization failed")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Handles presence sampled from frame analysis with strict debounce stabilization.
     */
    private fun handlePresenceSample(isPersonDetected: Boolean) {
        if (_isMonitoringPaused.value || !guardianSettings.isGuardianEnabled) return

        if (isPersonDetected) {
            consecutivePresentSamples++
            consecutiveAbsentSamples = 0

            if (consecutivePresentSamples >= 2) {
                // Confirmed student returned or present
                if (_presenceState.value != PresenceState.PRESENT) {
                    _presenceState.value = PresenceState.PRESENT
                }
                absenceStabilizationJob?.cancel()
                absenceStabilizationJob = null
                _absenceCountdown.value = 0
                stopAllAlerts()
            }
        } else {
            consecutiveAbsentSamples++
            consecutivePresentSamples = 0

            // If 2 consecutive absent samples: start absence stabilization
            if (consecutiveAbsentSamples >= 2 && _presenceState.value != PresenceState.ABSENT) {
                _presenceState.value = PresenceState.UNKNOWN
                startAbsenceCountdown()
            }
        }
    }

    private fun startAbsenceCountdown() {
        if (absenceStabilizationJob != null) return

        absenceStabilizationJob = coroutineScope.launch(Dispatchers.Main) {
            // 5 second debounce stabilization period
            for (sec in 5 downTo 1) {
                _absenceCountdown.value = sec
                delay(1000L)
            }
            _absenceCountdown.value = 0

            // Confirmed ABSENT
            _presenceState.value = PresenceState.ABSENT
            _absenceEventsCount.value += 1
            _interruptionCount.value += 1

            triggerWarningSequence()
        }
    }

    private fun triggerWarningSequence() {
        warningSequenceJob?.cancel()
        warningSequenceJob = coroutineScope.launch(Dispatchers.Main) {
            val alertType = guardianSettings.alertType
            if (alertType == GuardianAlertType.OFF) return@launch

            // 1. Start the first voice warning
            _warningStage.value = GuardianWarningStage.VOICE_WARNING
            if (guardianSettings.isAudioReminderEnabled) {
                // 2. Wait for the warning to finish
                voiceReminderManager.speakWarningAwait()
            } else {
                delay(2000L)
            }

            // If user returned or sequence cancelled, stop immediately
            if (!isActive || _presenceState.value != PresenceState.ABSENT) return@launch

            // Brief pause before second voice warning
            delay(500L)
            if (!isActive || _presenceState.value != PresenceState.ABSENT) return@launch

            // 3. If the user is still absent, repeat the SAME voice warning a second time
            if (guardianSettings.isAudioReminderEnabled) {
                // Wait for the warning to finish
                voiceReminderManager.speakWarningAwait()
            } else {
                delay(2000L)
            }

            if (!isActive || _presenceState.value != PresenceState.ABSENT) return@launch

            // 4. Wait briefly after the second warning
            delay(1200L)

            // 5. If the user is still absent, THEN trigger the reveille/alarm + vibration
            if (!isActive || _presenceState.value != PresenceState.ABSENT) return@launch

            if (alertType == GuardianAlertType.VOICE_AND_REVEILLE) {
                _warningStage.value = GuardianWarningStage.REVEILLE_ALARM

                // Start native Reveille horn audio
                reveillePlayer.start(guardianSettings.volumeLevel)

                // Start native repeating vibration
                if (guardianSettings.isVibrationEnabled) {
                    startVibration()
                }
            }
        }
    }

    private fun startVibration() {
        try {
            val pattern = longArrayOf(0, 350, 200, 350, 600)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(pattern, 0)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (_: Exception) {}
    }

    private fun stopVibration() {
        try {
            vibrator?.cancel()
        } catch (_: Exception) {}
    }

    /**
     * Immediately cuts off all voice warnings, reveille alarm, and vibration.
     */
    fun stopAllAlerts() {
        warningSequenceJob?.cancel()
        warningSequenceJob = null
        _warningStage.value = GuardianWarningStage.NONE

        voiceReminderManager.stop()
        reveillePlayer.stop()
        stopVibration()
    }

    /**
     * Pauses monitoring during student break or app backgrounding.
     */
    fun pauseMonitoring() {
        _isMonitoringPaused.value = true
        absenceStabilizationJob?.cancel()
        absenceStabilizationJob = null
        stopAllAlerts()
    }

    /**
     * Resumes monitoring when returning from break or foregrounding.
     */
    fun resumeMonitoring() {
        _isMonitoringPaused.value = false
        consecutivePresentSamples = 0
        consecutiveAbsentSamples = 0
        _presenceState.value = PresenceState.UNKNOWN
    }

    /**
     * For developer and settings testing: tests Voice Warning -> Reveille -> Vibration sequence.
     */
    fun triggerTestAlert(onFinished: () -> Unit) {
        coroutineScope.launch(Dispatchers.Main) {
            voiceReminderManager.speakWarning()
            delay(3000L)
            reveillePlayer.start(guardianSettings.volumeLevel)
            if (guardianSettings.isVibrationEnabled) {
                startVibration()
            }
            delay(4000L)
            stopAllAlerts()
            onFinished()
        }
    }

    /**
     * Full clean shutdown of camera, audio, TTS, vibrator, and background threads.
     */
    fun release() {
        stopAllAlerts()
        absenceStabilizationJob?.cancel()
        absenceStabilizationJob = null

        try {
            cameraProvider?.unbindAll()
        } catch (_: Exception) {}
        cameraProvider = null
        _isCameraActive.value = false

        voiceReminderManager.shutdown()
        reveillePlayer.stop()
        cameraExecutor.shutdown()
    }
}
