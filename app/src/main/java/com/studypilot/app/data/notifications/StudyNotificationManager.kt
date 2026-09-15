package com.studypilot.app.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.studypilot.app.MainActivity
import com.studypilot.app.R
import com.studypilot.app.data.model.NotificationPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import java.util.concurrent.TimeUnit

enum class NotificationType(val title: String, val channelId: String) {
    STUDY_REMINDER("Study Session Reminder", "studypilot_reminders"),
    PLANNED_SESSION("Planned Study Block", "studypilot_reminders"),
    REVIEW_REMINDER("Retention Review", "studypilot_reminders"),
    TEST_REMINDER("Topic Test Evaluation", "studypilot_reminders"),
    DAILY_GOAL("Daily Academic Target", "studypilot_reminders"),
    STREAK_REMINDER("Study Streak Alert", "studypilot_reminders")
}

class StudyNotificationManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("studypilot_notification_prefs", Context.MODE_PRIVATE)

    private val _preferences = MutableStateFlow(loadPreferences())
    val preferences: StateFlow<NotificationPreferences> = _preferences.asStateFlow()

    init {
        createNotificationChannels()
        schedulePeriodicReminders(_preferences.value)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "StudyPilot Academic Reminders"
            val descriptionText = "Notifications for study sessions, tests, streaks, and review reminders"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID_REMINDERS, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun loadPreferences(): NotificationPreferences {
        return NotificationPreferences(
            areNotificationsEnabled = prefs.getBoolean(KEY_ALL_ENABLED, true),
            studyRemindersEnabled = prefs.getBoolean(KEY_STUDY_REMINDERS, true),
            streakAlertsEnabled = prefs.getBoolean(KEY_STREAK_ALERTS, true),
            reviewRemindersEnabled = prefs.getBoolean(KEY_REVIEW_REMINDERS, true),
            testRemindersEnabled = prefs.getBoolean(KEY_TEST_REMINDERS, true),
            dailyGoalAlertsEnabled = prefs.getBoolean(KEY_DAILY_GOAL, true),
            reminderHour = prefs.getInt(KEY_REMINDER_HOUR, 18),
            reminderMinute = prefs.getInt(KEY_REMINDER_MINUTE, 0)
        )
    }

    fun updatePreferences(newPrefs: NotificationPreferences) {
        prefs.edit()
            .putBoolean(KEY_ALL_ENABLED, newPrefs.areNotificationsEnabled)
            .putBoolean(KEY_STUDY_REMINDERS, newPrefs.studyRemindersEnabled)
            .putBoolean(KEY_STREAK_ALERTS, newPrefs.streakAlertsEnabled)
            .putBoolean(KEY_REVIEW_REMINDERS, newPrefs.reviewRemindersEnabled)
            .putBoolean(KEY_TEST_REMINDERS, newPrefs.testRemindersEnabled)
            .putBoolean(KEY_DAILY_GOAL, newPrefs.dailyGoalAlertsEnabled)
            .putInt(KEY_REMINDER_HOUR, newPrefs.reminderHour)
            .putInt(KEY_REMINDER_MINUTE, newPrefs.reminderMinute)
            .apply()
        _preferences.value = newPrefs
        schedulePeriodicReminders(newPrefs)
    }

    fun schedulePeriodicReminders(notificationPrefs: NotificationPreferences) {
        try {
            val workManager = WorkManager.getInstance(context)
            if (!notificationPrefs.areNotificationsEnabled || !notificationPrefs.studyRemindersEnabled) {
                workManager.cancelUniqueWork(StudyReminderWorker.TAG_DAILY_REMINDER)
                return
            }

            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, notificationPrefs.reminderHour)
                set(Calendar.MINUTE, notificationPrefs.reminderMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val initialDelay = target.timeInMillis - now.timeInMillis
            val dailyWork = PeriodicWorkRequestBuilder<StudyReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .addTag(StudyReminderWorker.TAG_DAILY_REMINDER)
                .build()

            workManager.enqueueUniquePeriodicWork(
                StudyReminderWorker.TAG_DAILY_REMINDER,
                ExistingPeriodicWorkPolicy.UPDATE,
                dailyWork
            )
        } catch (_: Exception) {
            // Defensive catch for non-Android test environments
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun sendNotification(
        type: NotificationType,
        message: String,
        notificationId: Int = (System.currentTimeMillis() % 10000).toInt()
    ): Boolean {
        val currentPrefs = _preferences.value
        if (!currentPrefs.areNotificationsEnabled) return false

        // Check category preferences
        when (type) {
            NotificationType.STUDY_REMINDER -> if (!currentPrefs.studyRemindersEnabled) return false
            NotificationType.PLANNED_SESSION -> if (!currentPrefs.studyRemindersEnabled) return false
            NotificationType.REVIEW_REMINDER -> if (!currentPrefs.reviewRemindersEnabled) return false
            NotificationType.TEST_REMINDER -> if (!currentPrefs.testRemindersEnabled) return false
            NotificationType.DAILY_GOAL -> if (!currentPrefs.dailyGoalAlertsEnabled) return false
            NotificationType.STREAK_REMINDER -> if (!currentPrefs.streakAlertsEnabled) return false
        }

        if (!hasNotificationPermission()) return false

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(type.title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            return true
        } catch (e: SecurityException) {
            return false
        } catch (e: Exception) {
            return false
        }
    }

    companion object {
        const val CHANNEL_ID_REMINDERS = "studypilot_reminders"

        private const val KEY_ALL_ENABLED = "key_all_notifications_enabled"
        private const val KEY_STUDY_REMINDERS = "key_study_reminders"
        private const val KEY_STREAK_ALERTS = "key_streak_alerts"
        private const val KEY_REVIEW_REMINDERS = "key_review_reminders"
        private const val KEY_TEST_REMINDERS = "key_test_reminders"
        private const val KEY_DAILY_GOAL = "key_daily_goal"
        private const val KEY_REMINDER_HOUR = "key_reminder_hour"
        private const val KEY_REMINDER_MINUTE = "key_reminder_minute"
    }
}
