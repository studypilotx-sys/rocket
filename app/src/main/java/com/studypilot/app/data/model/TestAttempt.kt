package com.studypilot.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "test_attempts",
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
data class TestAttempt(
    @PrimaryKey
    val id: String,
    val topicId: String,
    val score: Int,
    val totalQuestions: Int = 5,
    val percentage: Int,
    val isPassed: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
