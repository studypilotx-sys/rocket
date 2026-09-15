package com.studypilot.app.data.model

enum class GuardianAlertType {
    VOICE_AND_REVEILLE,
    VOICE_ONLY,
    OFF
}

enum class GuardianVolumeLevel {
    LOW,
    MEDIUM,
    HIGH
}

data class GuardianSettings(
    val isGuardianEnabled: Boolean = true,
    val isAudioReminderEnabled: Boolean = true,
    val alertType: GuardianAlertType = GuardianAlertType.VOICE_AND_REVEILLE,
    val volumeLevel: GuardianVolumeLevel = GuardianVolumeLevel.MEDIUM,
    val isVibrationEnabled: Boolean = true
)
