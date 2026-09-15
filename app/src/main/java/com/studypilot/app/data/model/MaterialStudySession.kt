package com.studypilot.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "material_study_sessions",
    foreignKeys = [
        ForeignKey(
            entity = StudyMaterial::class,
            parentColumns = ["id"],
            childColumns = ["materialId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["materialId"]),
        Index(value = ["dateString"])
    ]
)
data class MaterialStudySession(
    @PrimaryKey
    val id: String,
    val materialId: String,
    val subjectId: String,
    val chapterId: String,
    val topicId: String? = null,
    val startTime: Long,
    val endTime: Long,
    val activeDurationSeconds: Long,
    val dateString: String
)
