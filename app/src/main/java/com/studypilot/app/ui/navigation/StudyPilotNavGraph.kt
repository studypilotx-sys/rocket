package com.studypilot.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.studypilot.app.ui.screens.*
import com.studypilot.app.ui.viewmodel.StudyPilotViewModel

@Composable
fun StudyPilotNavGraph(
    navController: NavHostController,
    viewModel: StudyPilotViewModel,
    startDestination: String,
    onLaunchGoogleSignIn: () -> Unit = {}
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val academicStats by viewModel.academicStats.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val currentSubject by viewModel.currentSubject.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val currentChapter by viewModel.currentChapter.collectAsState()
    val topics by viewModel.topics.collectAsState()
    val guardianSettings by viewModel.guardianSettings.collectAsState()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onOnboardingComplete = { country, system, grade ->
                    viewModel.completeOnboarding(country, system, grade)
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                userProfile = userProfile,
                academicStats = academicStats,
                onStartStudying = { navController.navigate(Screen.Subjects.route) },
                onNavigateToSubjects = { navController.navigate(Screen.Subjects.route) },
                onNavigateToMaterials = { navController.navigate(Screen.Materials.route) },
                onNavigateToPlanner = { navController.navigate(Screen.Planner.route) },
                onNavigateToProgress = { navController.navigate(Screen.Progress.route) },
                onNavigateToPluto = { navController.navigate(Screen.Pluto.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Subjects.route) {
            SubjectsScreen(
                subjects = subjects,
                onSubjectClick = { subjectId ->
                    viewModel.selectSubject(subjectId)
                    navController.navigate(Screen.Chapters.createRoute(subjectId))
                },
                onAddSubject = { name -> viewModel.addSubject(name) },
                onRenameSubject = { id, name -> viewModel.renameSubject(id, name) },
                onDeleteSubject = { id -> viewModel.deleteSubject(id) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Chapters.route,
            arguments = listOf(navArgument("subjectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
            ChaptersScreen(
                subject = currentSubject,
                chapters = chapters,
                onChapterClick = { chapterId ->
                    viewModel.selectChapter(chapterId)
                    navController.navigate(Screen.ChapterOverview.createRoute(subjectId, chapterId))
                },
                onAddChapter = { name -> viewModel.addChapter(subjectId, name) },
                onRenameChapter = { chapterId, name -> viewModel.renameChapter(chapterId, name, subjectId) },
                onDeleteChapter = { chapterId -> viewModel.deleteChapter(chapterId, subjectId) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ChapterOverview.route,
            arguments = listOf(
                navArgument("subjectId") { type = NavType.StringType },
                navArgument("chapterId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
            val chapterId = backStackEntry.arguments?.getString("chapterId") ?: ""
            ChapterOverviewScreen(
                viewModel = viewModel,
                subjectId = subjectId,
                chapterId = chapterId,
                onBackClick = { navController.popBackStack() },
                onTopicSelected = { topicId ->
                    navController.navigate(Screen.StudyScreen.createRoute(subjectId, chapterId, topicId))
                }
            )
        }

        composable(
            route = Screen.StudyScreen.route,
            arguments = listOf(
                navArgument("subjectId") { type = NavType.StringType },
                navArgument("chapterId") { type = NavType.StringType },
                navArgument("topicId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
            val chapterId = backStackEntry.arguments?.getString("chapterId") ?: ""
            val topicId = backStackEntry.arguments?.getString("topicId") ?: ""
            StudyScreen(
                viewModel = viewModel,
                subjectId = subjectId,
                chapterId = chapterId,
                topicId = topicId,
                onBackClick = { navController.popBackStack() },
                onStartFocusSession = {
                    navController.navigate(Screen.FocusSession.createRoute(subjectId, chapterId, topicId))
                },
                onOpenEvidence = {
                    navController.navigate(Screen.EvidencePhoto.createRoute(subjectId, chapterId, topicId))
                },
                onTakeTest = {
                    navController.navigate(Screen.Test.createRoute(subjectId, chapterId, topicId))
                }
            )
        }

        composable(
            route = Screen.FocusSession.route,
            arguments = listOf(
                navArgument("subjectId") { type = NavType.StringType },
                navArgument("chapterId") { type = NavType.StringType },
                navArgument("topicId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
            val chapterId = backStackEntry.arguments?.getString("chapterId") ?: ""
            val topicId = backStackEntry.arguments?.getString("topicId") ?: ""
            FocusSessionScreen(
                viewModel = viewModel,
                subjectId = subjectId,
                chapterId = chapterId,
                topicId = topicId,
                onBackClick = { navController.popBackStack() },
                onSessionFinished = {
                    val requireEvidence = userProfile?.evidenceRequired ?: true
                    if (requireEvidence) {
                        navController.navigate(Screen.EvidencePhoto.createRoute(subjectId, chapterId, topicId))
                    } else {
                        navController.navigate(Screen.Test.createRoute(subjectId, chapterId, topicId))
                    }
                }
            )
        }

        composable(
            route = Screen.EvidencePhoto.route,
            arguments = listOf(
                navArgument("subjectId") { type = NavType.StringType },
                navArgument("chapterId") { type = NavType.StringType },
                navArgument("topicId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
            val chapterId = backStackEntry.arguments?.getString("chapterId") ?: ""
            val topicId = backStackEntry.arguments?.getString("topicId") ?: ""
            EvidencePhotoScreen(
                viewModel = viewModel,
                subjectId = subjectId,
                chapterId = chapterId,
                topicId = topicId,
                onBackClick = { navController.popBackStack() },
                onProceedToTest = {
                    navController.navigate(Screen.Test.createRoute(subjectId, chapterId, topicId))
                }
            )
        }

        composable(
            route = Screen.Test.route,
            arguments = listOf(
                navArgument("subjectId") { type = NavType.StringType },
                navArgument("chapterId") { type = NavType.StringType },
                navArgument("topicId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
            val chapterId = backStackEntry.arguments?.getString("chapterId") ?: ""
            val topicId = backStackEntry.arguments?.getString("topicId") ?: ""
            TestScreen(
                viewModel = viewModel,
                subjectId = subjectId,
                chapterId = chapterId,
                topicId = topicId,
                onBackClick = { navController.popBackStack() },
                onTestFinished = {
                    navController.navigate(Screen.TestResult.createRoute(subjectId, chapterId, topicId))
                }
            )
        }

        composable(
            route = Screen.TestResult.route,
            arguments = listOf(
                navArgument("subjectId") { type = NavType.StringType },
                navArgument("chapterId") { type = NavType.StringType },
                navArgument("topicId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
            val chapterId = backStackEntry.arguments?.getString("chapterId") ?: ""
            val topicId = backStackEntry.arguments?.getString("topicId") ?: ""
            TestResultScreen(
                viewModel = viewModel,
                subjectId = subjectId,
                chapterId = chapterId,
                topicId = topicId,
                onBackToChapter = {
                    navController.navigate(Screen.ChapterOverview.createRoute(subjectId, chapterId)) {
                        popUpTo(Screen.ChapterOverview.createRoute(subjectId, chapterId)) { inclusive = true }
                    }
                },
                onRetakeTest = {
                    navController.navigate(Screen.Test.createRoute(subjectId, chapterId, topicId))
                }
            )
        }

        composable(
            route = Screen.Topics.route,
            arguments = listOf(
                navArgument("subjectId") { type = NavType.StringType },
                navArgument("chapterId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
            val chapterId = backStackEntry.arguments?.getString("chapterId") ?: ""
            TopicsScreen(
                chapter = currentChapter,
                topics = topics,
                onAddTopic = { name -> viewModel.addTopic(chapterId, name, subjectId) },
                onRenameTopic = { topicId, name -> viewModel.renameTopic(topicId, name, chapterId) },
                onDeleteTopic = { topicId -> viewModel.deleteTopic(topicId, chapterId, subjectId) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Planner.route) {
            PlannerScreen(
                viewModel = viewModel,
                onStartTopicStudy = { subjectId, chapterId, topicId ->
                    viewModel.selectSubject(subjectId)
                    viewModel.selectChapter(chapterId)
                    viewModel.selectTopic(topicId)
                    navController.navigate(Screen.StudyScreen.createRoute(subjectId, chapterId, topicId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Materials.route) {
            MaterialsScreen(
                viewModel = viewModel,
                onStudyMaterial = { materialId ->
                    navController.navigate(Screen.MaterialStudy.createRoute(materialId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.MaterialStudy.route,
            arguments = listOf(navArgument("materialId") { type = NavType.StringType })
        ) { backStackEntry ->
            val materialId = backStackEntry.arguments?.getString("materialId") ?: ""
            MaterialStudyScreen(
                viewModel = viewModel,
                materialId = materialId,
                onBack = { navController.popBackStack() },
                onFinished = { navController.popBackStack() }
            )
        }

        composable(Screen.Progress.route) {
            ProgressScreen(
                viewModel = viewModel,
                onStartStudying = { navController.navigate(Screen.Subjects.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Pluto.route) {
            PlutoScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                userProfile = userProfile,
                guardianSettings = guardianSettings,
                onUpdateGuardianEnabled = { viewModel.updateGuardianEnabled(it) },
                onUpdateAudioReminderEnabled = { viewModel.updateAudioReminderEnabled(it) },
                onUpdateAlertType = { viewModel.updateAlertType(it) },
                onUpdateVolumeLevel = { viewModel.updateVolumeLevel(it) },
                onUpdateVibrationEnabled = { viewModel.updateVibrationEnabled(it) },
                onNavigateToSubjects = { navController.navigate(Screen.Subjects.route) },
                onNavigateToMaterials = { navController.navigate(Screen.Materials.route) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) },
                onUpdateProfile = { country, system, grade ->
                    viewModel.updateProfile(country, system, grade)
                },
                onUpdateUserSettings = { dailyLimit, evidenceRequired ->
                    viewModel.updateUserSettings(dailyLimit, evidenceRequired)
                },
                onLaunchGoogleSignIn = onLaunchGoogleSignIn,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.About.route) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
