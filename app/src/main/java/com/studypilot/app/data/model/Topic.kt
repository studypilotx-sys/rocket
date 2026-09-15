package com.studypilot.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "topics",
    foreignKeys = [
        ForeignKey(
            entity = Chapter::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["chapterId"])]
)
data class Topic(
    @PrimaryKey
    val id: String,
    val chapterId: String,
    val name: String,
    val orderIndex: Int = 0,
    val state: TopicState = TopicState.NOT_STARTED,
    val estimatedMinutes: Int = 25,
    val createdAt: Long = System.currentTimeMillis()
)
