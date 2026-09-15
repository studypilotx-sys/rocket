package com.studypilot.app.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Subjects : Screen("subjects")
    object Chapters : Screen("chapters/{subjectId}") {
        fun createRoute(subjectId: String) = "chapters/$subjectId"
    }
    object Topics : Screen("topics/{subjectId}/{chapterId}") {
        fun createRoute(subjectId: String, chapterId: String) = "topics/$subjectId/$chapterId"
    }
    object ChapterOverview : Screen("chapter_overview/{subjectId}/{chapterId}") {
        fun createRoute(subjectId: String, chapterId: String) = "chapter_overview/$subjectId/$chapterId"
    }
    object StudyScreen : Screen("study_screen/{subjectId}/{chapterId}/{topicId}") {
        fun createRoute(subjectId: String, chapterId: String, topicId: String) = "study_screen/$subjectId/$chapterId/$topicId"
    }
    object FocusSession : Screen("focus_session/{subjectId}/{chapterId}/{topicId}") {
        fun createRoute(subjectId: String, chapterId: String, topicId: String) = "focus_session/$subjectId/$chapterId/$topicId"
    }
    object EvidencePhoto : Screen("evidence_photo/{subjectId}/{chapterId}/{topicId}") {
        fun createRoute(subjectId: String, chapterId: String, topicId: String) = "evidence_photo/$subjectId/$chapterId/$topicId"
    }
    object Test : Screen("test_screen/{subjectId}/{chapterId}/{topicId}") {
        fun createRoute(subjectId: String, chapterId: String, topicId: String) = "test_screen/$subjectId/$chapterId/$topicId"
    }
    object TestResult : Screen("test_result/{subjectId}/{chapterId}/{topicId}") {
        fun createRoute(subjectId: String, chapterId: String, topicId: String) = "test_result/$subjectId/$chapterId/$topicId"
    }
    object Planner : Screen("planner")
    object Progress : Screen("progress")
    object Materials : Screen("materials")
    object MaterialStudy : Screen("material_study/{materialId}") {
        fun createRoute(materialId: String) = "material_study/$materialId"
    }
    object Pluto : Screen("pluto")
    object Settings : Screen("settings")
    object About : Screen("about")
}
