package com.studypilot.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "evidence_records",
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
data class Evidence(
    @PrimaryKey
    val id: String,
    val topicId: String,
    val sessionId: String? = null,
    val imagePath: String,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
