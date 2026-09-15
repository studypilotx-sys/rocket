package com.studypilot.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey
    val userId: String = "default_student",
    val country: String,
    val educationSystem: String,
    val grade: String,
    val createdDate: Long = System.currentTimeMillis(),
    val dailyStudyLimitMinutes: Int = 120,
    val evidenceRequired: Boolean = true,
    val isOnboarded: Boolean = true,
    val googleId: String? = null,
    val googleEmail: String? = null,
    val googleDisplayName: String? = null,
    val googlePhotoUrl: String? = null
)
