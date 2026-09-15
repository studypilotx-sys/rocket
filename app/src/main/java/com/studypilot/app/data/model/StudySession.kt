package com.studypilot.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "study_sessions",
    foreignKeys = [
        ForeignKey(
            entity = Topic::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["topicId"])]
)
data class StudySession(
    @PrimaryKey
    val id: String,
    val topicId: String,
    val chapterId: String,
    val subjectId: String,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long,
    val targetMinutes: Int,
    val isCompleted: Boolean = true,
    val dateString: String,
    val activeSeconds: Long = durationSeconds,
    val pauseSeconds: Long = 0L,
    val breakSeconds: Long = 0L,
    val isGuardianEnabled: Boolean = false,
    val interruptionCount: Int = 0,
    val absenceEventsCount: Int = 0
)
