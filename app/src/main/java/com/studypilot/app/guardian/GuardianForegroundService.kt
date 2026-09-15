package com.studypilot.app.guardian

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.studypilot.app.MainActivity
import com.studypilot.app.R
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
import java.util.Locale
import java.util.concurrent.Executors
import android.os.Vibrator
import android.os.VibratorManager
import android.os.VibrationEffect

/**
 * Android Foreground Service for Guardian Anywhere.
 * 
 * Runs continuously in the background when StudyPilot is minimized, keeping:
 * 1. Focus session timer ticking
 * 2. Real CameraX front-camera presence detection running
 * 3. Movable "Guardian Active" floating overlay on screen above other apps (YouTube, browser, etc.)
 * 4. Automatic absence detection -> Timer pause -> Voice warning -> Reveille horn + Vibration
 * 5. Return to camera view -> Immediate alerts cessation & resume timer
 * 6. End Focus Session -> cleanly stops service, camera, timer, alerts, overlay
 */
class GuardianForegroundService : LifecycleService() {

    inner class LocalBinder : Binder() {
        fun getService(): GuardianForegroundService = this@GuardianForegroundService
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    // State flows
    private val _activeSeconds = MutableStateFlow(0L)
    val activeSeconds: StateFlow<Long> = _activeSeconds.asStateFlow()

    private val _pauseSeconds = MutableStateFlow(0L)
    val pauseSeconds: StateFlow<Long> = _pauseSeconds.asStateFlow()

    private val _presenceState = MutableStateFlow(PresenceState.UNKNOWN)
    val presenceState: StateFlow<PresenceState> = _presenceState.asStateFlow()

    private val _interruptionCount = MutableStateFlow(0)
    val interruptionCount: StateFlow<Int> = _interruptionCount.asStateFlow()

    private val _absenceEventsCount = MutableStateFlow(0)
    val absenceEventsCount: StateFlow<Int> = _absenceEventsCount.asStateFlow()

    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    // Managers
    private var overlayManager: GuardianOverlayManager? = null
    private var reveillePlayer: ReveillePlayer? = null
    private var voiceReminderManager: VoiceReminderManager? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private var guardianSettings = GuardianSettings()
    private var timerJob: Job? = null
    private var absenceCountdownJob: Job? = null
    private var alertSequenceJob: Job? = null

    private var consecutivePresentSamples = 0
    private var consecutiveAbsentSamples = 0
    private var isSessionPausedDueToAbsence = false

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    companion object {
        const val CHANNEL_ID = "guardian_anywhere_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START = "com.studypilot.app.guardian.START"
        const val ACTION_STOP = "com.studypilot.app.guardian.STOP"
        const val ACTION_PAUSE = "com.studypilot.app.guardian.PAUSE"
        const val ACTION_RESUME = "com.studypilot.app.guardian.RESUME"

        const val EXTRA_TOPIC_NAME = "extra_topic_name"
        const val EXTRA_INITIAL_SECONDS = "extra_initial_seconds"

        var isRunning = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        reveillePlayer = ReveillePlayer()
        voiceReminderManager = VoiceReminderManager(this)

        overlayManager = GuardianOverlayManager(
            context = this,
            onEndSessionClicked = {
                stopGuardianSession()
            },
            onOpenAppClicked = {
                bringAppToForeground()
            }
        )

        createNotificationChannel()
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START -> {
                val topicName = intent.getStringExtra(EXTRA_TOPIC_NAME) ?: "Focus Session"
                val initialSec = intent.getLongExtra(EXTRA_INITIAL_SECONDS, 0L)
                startGuardianSession(topicName, initialSec)
            }
            ACTION_STOP -> {
                stopGuardianSession()
            }
            ACTION_PAUSE -> {
                pauseSession()
            }
            ACTION_RESUME -> {
                resumeSession()
            }
        }

        return START_STICKY
    }

    fun updateSettings(settings: GuardianSettings) {
        this.guardianSettings = settings
        if (!settings.isGuardianEnabled) {
            stopAllAlerts()
        }
    }

    private fun startGuardianSession(topicName: String, initialSeconds: Long) {
        _activeSeconds.value = initialSeconds
        _isSessionActive.value = true

        val notification = buildNotification(topicName, "Guardian Anywhere active — tracking focus")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Show floating overlay
        overlayManager?.showOverlay()

        // Start background CameraX
        startCameraX()

        // Start 1-second ticker loop
        startTimerLoop()
    }

    private fun startTimerLoop() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (_isSessionActive.value) {
                delay(1000L)
                if (!isSessionPausedDueToAbsence && (_presenceState.value == PresenceState.PRESENT || !guardianSettings.isGuardianEnabled)) {
                    _activeSeconds.value += 1
                } else {
                    _pauseSeconds.value += 1
                }
                overlayManager?.updateTimer(_activeSeconds.value)
            }
        }
    }

    private fun startCameraX() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider
                provider.unbindAll()

                val preview = Preview.Builder().build()
                val overlayPreview = overlayManager?.getCameraPreviewView()
                if (overlayPreview != null) {
                    preview.setSurfaceProvider(overlayPreview.surfaceProvider)
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
                    this, // LifecycleService acts as LifecycleOwner
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                // Safe error handling for camera bind
            }
        }, cameraExecutor)
    }

    private fun handlePresenceSample(isPersonDetected: Boolean) {
        if (!_isSessionActive.value || !guardianSettings.isGuardianEnabled) return

        if (isPersonDetected) {
            consecutivePresentSamples++
            consecutiveAbsentSamples = 0

            if (consecutivePresentSamples >= 2) {
                if (_presenceState.value != PresenceState.PRESENT) {
                    _presenceState.value = PresenceState.PRESENT
                    overlayManager?.updatePresenceState(PresenceState.PRESENT)
                }
                absenceCountdownJob?.cancel()
                absenceCountdownJob = null
                stopAllAlerts()
            }
        } else {
            consecutiveAbsentSamples++
            consecutivePresentSamples = 0

            if (consecutiveAbsentSamples >= 2 && _presenceState.value != PresenceState.ABSENT) {
                _presenceState.value = PresenceState.UNKNOWN
                overlayManager?.updatePresenceState(PresenceState.UNKNOWN)
                startAbsenceCountdown()
            }
        }
    }

    private fun startAbsenceCountdown() {
        if (absenceCountdownJob != null) return

        absenceCountdownJob = serviceScope.launch {
            // 5 second debounce stabilization
            for (sec in 5 downTo 1) {
                overlayManager?.updatePresenceState(PresenceState.ABSENT, sec)
                delay(1000L)
            }

            // Confirmed ABSENT
            _presenceState.value = PresenceState.ABSENT
            isSessionPausedDueToAbsence = true
            _absenceEventsCount.value += 1
            _interruptionCount.value += 1
            overlayManager?.updatePresenceState(PresenceState.ABSENT, 0)

            triggerWarningSequence()
        }
    }

    private fun triggerWarningSequence() {
        alertSequenceJob?.cancel()
        alertSequenceJob = serviceScope.launch {
            val alertType = guardianSettings.alertType
            if (alertType == GuardianAlertType.OFF) return@launch

            // 1. Start the first voice warning
            if (guardianSettings.isAudioReminderEnabled) {
                // 2. Wait for the warning to finish
                voiceReminderManager?.speakWarningAwait()
            } else {
                delay(2000L)
            }

            // If user returned or sequence was cancelled, stop immediately
            if (!isActive || _presenceState.value != PresenceState.ABSENT) return@launch

            // Brief pause before second voice warning
            delay(500L)
            if (!isActive || _presenceState.value != PresenceState.ABSENT) return@launch

            // 3. If the user is still absent, repeat the SAME voice warning a second time
            if (guardianSettings.isAudioReminderEnabled) {
                // Wait for the warning to finish
                voiceReminderManager?.speakWarningAwait()
            } else {
                delay(2000L)
            }

            if (!isActive || _presenceState.value != PresenceState.ABSENT) return@launch

            // 4. Wait briefly after the second warning
            delay(1200L)

            // 5. If the user is still absent, THEN trigger the reveille/alarm + vibration
            if (!isActive || _presenceState.value != PresenceState.ABSENT) return@launch

            if (alertType == GuardianAlertType.VOICE_AND_REVEILLE) {
                reveillePlayer?.start(guardianSettings.volumeLevel)
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

    fun stopAllAlerts() {
        alertSequenceJob?.cancel()
        alertSequenceJob = null
        voiceReminderManager?.stop()
        reveillePlayer?.stop()
        stopVibration()
    }

    fun pauseSession() {
        _isSessionActive.value = false
        isSessionPausedDueToAbsence = true
        timerJob?.cancel()
        absenceCountdownJob?.cancel()
        absenceCountdownJob = null
        stopAllAlerts()
    }

    fun resumeSession() {
        isSessionPausedDueToAbsence = false
        if (!_isSessionActive.value) {
            _isSessionActive.value = true
            consecutivePresentSamples = 0
            consecutiveAbsentSamples = 0
            _presenceState.value = PresenceState.UNKNOWN
            startTimerLoop()
        }
    }

    fun stopGuardianSession() {
        _isSessionActive.value = false
        timerJob?.cancel()
        timerJob = null
        absenceCountdownJob?.cancel()
        absenceCountdownJob = null

        stopAllAlerts()
        overlayManager?.hideOverlay()

        try {
            cameraProvider?.unbindAll()
        } catch (_: Exception) {}
        cameraProvider = null

        voiceReminderManager?.shutdown()
        reveillePlayer?.stop()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun bringAppToForeground() {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(launchIntent)
    }

    private fun buildNotification(title: String, content: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Guardian Anywhere: $title")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus Guardian Anywhere",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Focus Guardian monitoring and floating overlay active while multitasking."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        isRunning = false
        stopGuardianSession()
        cameraExecutor.shutdown()
        super.onDestroy()
    }
}
