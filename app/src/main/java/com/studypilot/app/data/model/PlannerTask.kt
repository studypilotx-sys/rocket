package com.studypilot.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PlannerTaskStatus {
    PLANNED,
    IN_PROGRESS,
    COMPLETED,
    SKIPPED
}

@Entity(
    tableName = "planner_tasks",
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
        ),
        ForeignKey(
            entity = Topic::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["plannedDate"]),
        Index(value = ["topicId"]),
        Index(value = ["chapterId"]),
        Index(value = ["subjectId"])
    ]
)
data class PlannerTask(
    @PrimaryKey
    val id: String,
    val subjectId: String,
    val chapterId: String,
    val topicId: String,
    val plannedDate: String, // yyyy-MM-dd
    val plannedDurationMinutes: Int,
    val priority: Int, // 1 = Critical/High, 2 = Medium, 3 = Normal
    val reason: String,
    val status: PlannerTaskStatus = PlannerTaskStatus.PLANNED,
    val createdAt: Long = System.currentTimeMillis()
)

data class PlannerTaskWithDetails(
    val task: PlannerTask,
    val subjectName: String,
    val subjectColorHex: String,
    val chapterName: String,
    val topicName: String,
    val topicEstimatedMinutes: Int,
    val topicState: TopicState
)
