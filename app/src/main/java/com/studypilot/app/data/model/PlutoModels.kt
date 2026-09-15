package com.studypilot.app.data.model

import java.util.UUID

enum class PlutoIntent(val label: String) {
    CASUAL_CONVERSATION("Casual Chat"),
    GENERAL_QUESTION("General Knowledge"),
    MOTIVATION("Study Motivation"),
    STUDY_QUESTION("Study Question"),
    CONCEPT_EXPLANATION("Concept Deep Dive"),
    STUDYPILOT_APP_HELP("App Guide"),
    STUDY_RECOMMENDATION("Study Recommendation"),
    IMAGE_QUESTION("Image Inquiry"),
    FILE_QUESTION("Document Inquiry"),
    QUIZ_FROM_ATTACHMENT("Practice Quiz"),
    UNKNOWN("Assistant Response")
}

data class PlutoAttachment(
    val uri: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long = 0L,
    val isImage: Boolean = false
)

data class PlutoQuizQuestion(
    val id: String = UUID.randomUUID().toString(),
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String
)

data class PlutoQuiz(
    val id: String = UUID.randomUUID().toString(),
    val materialTitle: String,
    val questions: List<PlutoQuizQuestion>,
    val currentQuestionIndex: Int = 0,
    val userSelectedAnswers: Map<Int, Int> = emptyMap(), // questionIndex -> selectedOptionIndex
    val isCompleted: Boolean = false
) {
    val totalQuestions: Int get() = questions.size
    val attemptedQuestions: Int get() = userSelectedAnswers.size
    val correctAnswers: Int get() = userSelectedAnswers.count { (index, selected) ->
        index < questions.size && questions[index].correctAnswerIndex == selected
    }
    val incorrectAnswers: Int get() = attemptedQuestions - correctAnswers
    val percentage: Int get() = if (totalQuestions > 0) (correctAnswers * 100) / totalQuestions else 0
}

data class PlutoMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String, // "Pluto" or "Student"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val intent: PlutoIntent = PlutoIntent.UNKNOWN,
    val attachment: PlutoAttachment? = null,
    val isAiGenerated: Boolean = true,
    val isOfflineFallback: Boolean = false
)
