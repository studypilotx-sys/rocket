package com.studypilot.app.data.model

data class AcademicStats(
    val totalTopicsCount: Int = 0,
    val completedTopicsCount: Int = 0,
    val inProgressTopicsCount: Int = 0,
    val completionPercentage: Int = 0,
    val totalStudySeconds: Long = 0L,
    val todayStudySeconds: Long = 0L,
    val dailyLimitMinutes: Int = 120,
    val testsAttempted: Int = 0,
    val testsPassed: Int = 0,
    val overallAverageScore: Int? = null,
    val subjectProgressList: List<SubjectProgress> = emptyList(),
    val totalMaterialsCount: Int = 0,
    val totalMaterialStudySeconds: Long = 0L
)

data class SubjectProgress(
    val subjectId: String,
    val subjectName: String,
    val colorHex: String,
    val totalTopics: Int,
    val completedTopics: Int,
    val totalStudySeconds: Long
)
