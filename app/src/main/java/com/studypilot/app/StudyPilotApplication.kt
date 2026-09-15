package com.studypilot.app

import android.app.Application
import com.studypilot.app.data.auth.GoogleAuthManager
import com.studypilot.app.data.local.StudyPilotDatabase
import com.studypilot.app.data.notifications.StudyNotificationManager
import com.studypilot.app.data.repository.StudyPilotRepository
import com.studypilot.app.data.security.AppLockManager

class StudyPilotApplication : Application() {

    val database by lazy { StudyPilotDatabase.getDatabase(this) }
    val repository by lazy { StudyPilotRepository(database) }
    val appLockManager by lazy { AppLockManager(this) }
    val googleAuthManager by lazy { GoogleAuthManager(this) }
    val notificationManager by lazy { StudyNotificationManager(this) }

    override fun onCreate() {
        super.onCreate()
    }
}
