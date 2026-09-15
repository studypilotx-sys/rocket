package com.studypilot.app.data.model

data class SubjectWithCounts(
    val subject: Subject,
    val chapterCount: Int = 0,
    val topicCount: Int = 0
)

data class ChapterWithCounts(
    val chapter: Chapter,
    val topicCount: Int = 0
)
