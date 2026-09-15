package com.studypilot.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "test_questions",
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
data class TestQuestion(
    @PrimaryKey
    val id: String,
    val topicId: String,
    val questionNumber: Int,
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctOptionIndex: Int, // 0 for A, 1 for B, 2 for C, 3 for D
    val explanation: String
)
