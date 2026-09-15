package com.studypilot.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MaterialType {
    PDF,
    VIDEO,
    IMAGE,
    DOCUMENT,
    NOTES
}

@Entity(
    tableName = "study_materials",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Chapter::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["subjectId"]),
        Index(value = ["chapterId"]),
        Index(value = ["topicId"])
    ]
)
data class StudyMaterial(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: MaterialType,
    val uriOrPath: String,
    val subjectId: String,
    val chapterId: String,
    val topicId: String? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val lastOpened: Long? = null,
    val durationMinutes: Int? = null,
    val notes: String? = null
)

data class MaterialWithDetails(
    val material: StudyMaterial,
    val subjectName: String,
    val chapterName: String,
    val topicName: String?
)
