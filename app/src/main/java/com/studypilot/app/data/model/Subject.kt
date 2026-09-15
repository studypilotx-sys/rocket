package com.studypilot.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey
    val id: String,
    val name: String,
    val orderIndex: Int = 0,
    val colorHex: String = "#C4975A",
    val createdAt: Long = System.currentTimeMillis()
)
