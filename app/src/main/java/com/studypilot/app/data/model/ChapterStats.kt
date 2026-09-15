package com.studypilot.app.data.model

data class ChapterStats(
    val totalTopics: Int = 0,
    val completedTopics: Int = 0,
    val inProgressTopics: Int = 0,
    val remainingTopics: Int = 0,
    val totalStudySeconds: Long = 0L,
    val averageScorePercentage: Int? = null,
    val testsTaken: Int = 0
)
