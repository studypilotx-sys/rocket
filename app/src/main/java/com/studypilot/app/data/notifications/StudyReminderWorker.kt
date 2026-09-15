package com.studypilot.app.data.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.studypilot.app.StudyPilotApplication

class StudyReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? StudyPilotApplication ?: return Result.success()
        val notificationManager = app.notificationManager
        val prefs = notificationManager.loadPreferences()

        if (!prefs.areNotificationsEnabled) {
            return Result.success()
        }

        if (prefs.studyRemindersEnabled) {
            notificationManager.sendNotification(
                type = NotificationType.STUDY_REMINDER,
                message = "Time for your scheduled StudyPilot study session! Tap to start your focus timer."
            )
        }

        // Check streak maintenance alert if user has not logged study today
        if (prefs.streakAlertsEnabled) {
            try {
                val stats = app.database.userStatsDao().getUserStats()
                // If study minutes today is 0, alert the student
                if (stats?.studyStreakDays != null && stats.studyStreakDays > 0) {
                    notificationManager.sendNotification(
                        type = NotificationType.STREAK_REMINDER,
                        message = "Protect your ${stats.studyStreakDays}-day streak! Complete a 25-minute topic study session before midnight."
                    )
                }
            } catch (_: Exception) {
                // Ignore query failure
            }
        }

        return Result.success()
    }

    companion object {
        const val TAG_DAILY_REMINDER = "studypilot_daily_reminder_worker"
        const val TAG_STREAK_ALERT = "studypilot_streak_alert_worker"
    }
}
