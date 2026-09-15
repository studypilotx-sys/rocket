package com.studypilot.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["subjectId"])]
)
data class Chapter(
    @PrimaryKey
    val id: String,
    val subjectId: String,
    val name: String,
    val orderIndex: Int = 0,
    val estimatedMinutes: Int = 60,
    val createdAt: Long = System.currentTimeMillis()
)
