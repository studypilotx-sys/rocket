package com.studypilot.app.data.model

enum class AutoLockTimeout(val label: String, val millis: Long) {
    IMMEDIATELY("Immediately", 0L),
    AFTER_1_MIN("After 1 minute", 60_000L),
    AFTER_5_MIN("After 5 minutes", 300_000L),
    AFTER_15_MIN("After 15 minutes", 900_000L),
    NEVER("Never", Long.MAX_VALUE)
}

data class AppLockSettings(
    val isAppLockEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val autoLockTimeout: AutoLockTimeout = AutoLockTimeout.AFTER_1_MIN,
    val hasPinSet: Boolean = false
)

data class NotificationPreferences(
    val areNotificationsEnabled: Boolean = true,
    val studyRemindersEnabled: Boolean = true,
    val streakAlertsEnabled: Boolean = true,
    val reviewRemindersEnabled: Boolean = true,
    val testRemindersEnabled: Boolean = true,
    val dailyGoalAlertsEnabled: Boolean = true,
    val reminderHour: Int = 18, // 6:00 PM default
    val reminderMinute: Int = 0
)
