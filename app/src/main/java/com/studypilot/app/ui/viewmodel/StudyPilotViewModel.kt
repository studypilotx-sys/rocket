package com.studypilot.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.fragment.app.FragmentActivity
import com.studypilot.app.data.auth.GoogleAuthManager
import com.studypilot.app.data.auth.GoogleUserData
import com.studypilot.app.data.model.*
import com.studypilot.app.data.notifications.NotificationType
import com.studypilot.app.data.notifications.StudyNotificationManager
import com.studypilot.app.data.pluto.PlutoEngine
import com.studypilot.app.data.security.AppLockManager
import com.studypilot.app.data.repository.StudyPilotRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class GuardianCorner {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}

data class QuestionReview(
    val question: TestQuestion,
    val selectedOptionIndex: Int,
    val isCorrect: Boolean
)

data class TestResultData(
    val score: Int,
    val totalQuestions: Int,
    val percentage: Int,
    val isPassed: Boolean,
    val reviews: List<QuestionReview>
)

class StudyPilotViewModel(
    private val repository: StudyPilotRepository,
    private val appLockManager: AppLockManager? = null,
    private val googleAuthManager: GoogleAuthManager? = null,
    private val notificationManager: StudyNotificationManager? = null
) : ViewModel() {

    // --- Phase 5: Pluto AI Assistant State ---
    private val _plutoMessages = MutableStateFlow<List<PlutoMessage>>(
        listOf(
            PlutoMessage(
                id = "welcome",
                sender = "Pluto",
                content = "Hello! I am Pluto, your StudyPilot academic assistant. I can chat casually, explain concepts step-by-step, provide study motivation, or generate practice quizzes from your study files and notes!",
                intent = PlutoIntent.CASUAL_CONVERSATION
            )
        )
    )
    val plutoMessages: StateFlow<List<PlutoMessage>> = _plutoMessages.asStateFlow()

    private val _plutoAttachment = MutableStateFlow<PlutoAttachment?>(null)
    val plutoAttachment: StateFlow<PlutoAttachment?> = _plutoAttachment.asStateFlow()

    private val _activePlutoQuiz = MutableStateFlow<PlutoQuiz?>(null)
    val activePlutoQuiz: StateFlow<PlutoQuiz?> = _activePlutoQuiz.asStateFlow()

    private val _isPlutoThinking = MutableStateFlow(false)
    val isPlutoThinking: StateFlow<Boolean> = _isPlutoThinking.asStateFlow()

    // --- Phase 5: Google Authentication State ---
    private val _googleUser = MutableStateFlow<GoogleUserData?>(googleAuthManager?.getLastSignedInAccount())
    val googleUser: StateFlow<GoogleUserData?> = _googleUser.asStateFlow()

    // --- Phase 5: App Lock & Security State ---
    val isAppLocked: StateFlow<Boolean> = appLockManager?.isLocked ?: MutableStateFlow(false)
    val appLockSettings: StateFlow<AppLockSettings> = appLockManager?.settings ?: MutableStateFlow(AppLockSettings())

    // --- Phase 5: Notifications State ---
    val notificationPreferences: StateFlow<NotificationPreferences> = notificationManager?.preferences ?: MutableStateFlow(NotificationPreferences())

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _subjects = MutableStateFlow<List<SubjectWithCounts>>(emptyList())
    val subjects: StateFlow<List<SubjectWithCounts>> = _subjects.asStateFlow()

    private val _currentSubject = MutableStateFlow<Subject?>(null)
    val currentSubject: StateFlow<Subject?> = _currentSubject.asStateFlow()

    private val _chapters = MutableStateFlow<List<ChapterWithCounts>>(emptyList())
    val chapters: StateFlow<List<ChapterWithCounts>> = _chapters.asStateFlow()

    private val _currentChapter = MutableStateFlow<Chapter?>(null)
    val currentChapter: StateFlow<Chapter?> = _currentChapter.asStateFlow()

    private val _topics = MutableStateFlow<List<Topic>>(emptyList())
    val topics: StateFlow<List<Topic>> = _topics.asStateFlow()

    private val _currentTopic = MutableStateFlow<Topic?>(null)
    val currentTopic: StateFlow<Topic?> = _currentTopic.asStateFlow()

    private val _chapterStats = MutableStateFlow<ChapterStats?>(null)
    val chapterStats: StateFlow<ChapterStats?> = _chapterStats.asStateFlow()

    private val _academicStats = MutableStateFlow<AcademicStats?>(null)
    val academicStats: StateFlow<AcademicStats?> = _academicStats.asStateFlow()

    private val _topicSessions = MutableStateFlow<List<StudySession>>(emptyList())
    val topicSessions: StateFlow<List<StudySession>> = _topicSessions.asStateFlow()

    private val _topicEvidence = MutableStateFlow<List<Evidence>>(emptyList())
    val topicEvidence: StateFlow<List<Evidence>> = _topicEvidence.asStateFlow()

    private val _testQuestions = MutableStateFlow<List<TestQuestion>>(emptyList())
    val testQuestions: StateFlow<List<TestQuestion>> = _testQuestions.asStateFlow()

    private val _lastCompletedSession = MutableStateFlow<StudySession?>(null)
    val lastCompletedSession: StateFlow<StudySession?> = _lastCompletedSession.asStateFlow()

    private val _lastTestResult = MutableStateFlow<TestResultData?>(null)
    val lastTestResult: StateFlow<TestResultData?> = _lastTestResult.asStateFlow()

    // --- Phase 3: Study Materials State ---
    private val _materials = MutableStateFlow<List<MaterialWithDetails>>(emptyList())
    val materials: StateFlow<List<MaterialWithDetails>> = _materials.asStateFlow()

    private val _currentMaterial = MutableStateFlow<MaterialWithDetails?>(null)
    val currentMaterial: StateFlow<MaterialWithDetails?> = _currentMaterial.asStateFlow()

    private val _isMaterialStudying = MutableStateFlow(false)
    val isMaterialStudying: StateFlow<Boolean> = _isMaterialStudying.asStateFlow()

    private val _activeMaterialStudySeconds = MutableStateFlow(0L)
    val activeMaterialStudySeconds: StateFlow<Long> = _activeMaterialStudySeconds.asStateFlow()

    private val _guardianCorner = MutableStateFlow(GuardianCorner.TOP_RIGHT)
    val guardianCorner: StateFlow<GuardianCorner> = _guardianCorner.asStateFlow()

    // --- Phase 4: Focus Guardian Settings ---
    private val _guardianSettings = MutableStateFlow(GuardianSettings())
    val guardianSettings: StateFlow<GuardianSettings> = _guardianSettings.asStateFlow()

    private var materialStudyJob: Job? = null
    private var materialSessionStartTime: Long = 0L

    // --- Phase 3: Adaptive Planner State ---
    private val _plannedTasks = MutableStateFlow<List<PlannerTaskWithDetails>>(emptyList())
    val plannedTasks: StateFlow<List<PlannerTaskWithDetails>> = _plannedTasks.asStateFlow()

    private val _selectedPlannerDate = MutableStateFlow(
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    )
    val selectedPlannerDate: StateFlow<String> = _selectedPlannerDate.asStateFlow()

    private val _isGeneratingPlan = MutableStateFlow(false)
    val isGeneratingPlan: StateFlow<Boolean> = _isGeneratingPlan.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadUserProfile()
        loadAcademicStats()
        loadMaterials()
        loadPlannedTasks()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val profile = repository.getUserProfile()
                _userProfile.value = profile
                if (profile != null && profile.isOnboarded) {
                    refreshSubjects()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load profile: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun completeOnboarding(country: String, educationSystem: String, grade: String) {
        viewModelScope.launch {
            try {
                val profile = UserProfile(
                    country = country,
                    educationSystem = educationSystem,
                    grade = grade,
                    isOnboarded = true
                )
                repository.saveUserProfile(profile)
                _userProfile.value = profile
                repository.seedInitialCurriculum(educationSystem, grade)
                refreshSubjects()
            } catch (e: Exception) {
                _errorMessage.value = "Error during onboarding: ${e.localizedMessage}"
            }
        }
    }

    fun refreshSubjects() {
        viewModelScope.launch {
            try {
                _subjects.value = repository.getSubjectsWithCounts()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load subjects: ${e.localizedMessage}"
            }
        }
    }

    fun addSubject(name: String) {
        if (name.isBlank()) {
            _errorMessage.value = "Subject name cannot be empty"
            return
        }
        viewModelScope.launch {
            try {
                repository.addSubject(name)
                refreshSubjects()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add subject: ${e.localizedMessage}"
            }
        }
    }

    fun renameSubject(subjectId: String, newName: String) {
        if (newName.isBlank()) {
            _errorMessage.value = "Subject name cannot be empty"
            return
        }
        viewModelScope.launch {
            try {
                repository.renameSubject(subjectId, newName)
                refreshSubjects()
                if (_currentSubject.value?.id == subjectId) {
                    _currentSubject.value = repository.getSubjectById(subjectId)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to rename subject: ${e.localizedMessage}"
            }
        }
    }

    fun deleteSubject(subjectId: String) {
        viewModelScope.launch {
            try {
                repository.deleteSubject(subjectId)
                refreshSubjects()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete subject: ${e.localizedMessage}"
            }
        }
    }

    fun selectSubject(subjectId: String) {
        viewModelScope.launch {
            try {
                _currentSubject.value = repository.getSubjectById(subjectId)
                refreshChapters(subjectId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to select subject: ${e.localizedMessage}"
            }
        }
    }

    fun refreshChapters(subjectId: String) {
        viewModelScope.launch {
            try {
                _chapters.value = repository.getChaptersWithCounts(subjectId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load chapters: ${e.localizedMessage}"
            }
        }
    }

    fun addChapter(subjectId: String, name: String) {
        if (name.isBlank()) {
            _errorMessage.value = "Chapter name cannot be empty"
            return
        }
        viewModelScope.launch {
            try {
                repository.addChapter(subjectId, name)
                refreshChapters(subjectId)
                refreshSubjects()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add chapter: ${e.localizedMessage}"
            }
        }
    }

    fun renameChapter(chapterId: String, newName: String, subjectId: String) {
        if (newName.isBlank()) {
            _errorMessage.value = "Chapter name cannot be empty"
            return
        }
        viewModelScope.launch {
            try {
                repository.renameChapter(chapterId, newName)
                refreshChapters(subjectId)
                if (_currentChapter.value?.id == chapterId) {
                    _currentChapter.value = repository.getChapterById(chapterId)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to rename chapter: ${e.localizedMessage}"
            }
        }
    }

    fun deleteChapter(chapterId: String, subjectId: String) {
        viewModelScope.launch {
            try {
                repository.deleteChapter(chapterId)
                refreshChapters(subjectId)
                refreshSubjects()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete chapter: ${e.localizedMessage}"
            }
        }
    }

    fun selectChapter(chapterId: String) {
        viewModelScope.launch {
            try {
                _currentChapter.value = repository.getChapterById(chapterId)
                refreshTopics(chapterId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to select chapter: ${e.localizedMessage}"
            }
        }
    }

    fun refreshTopics(chapterId: String) {
        viewModelScope.launch {
            try {
                _topics.value = repository.getTopics(chapterId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load topics: ${e.localizedMessage}"
            }
        }
    }

    fun addTopic(chapterId: String, name: String, subjectId: String) {
        if (name.isBlank()) {
            _errorMessage.value = "Topic name cannot be empty"
            return
        }
        viewModelScope.launch {
            try {
                repository.addTopic(chapterId, name)
                refreshTopics(chapterId)
                refreshChapters(subjectId)
                refreshSubjects()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add topic: ${e.localizedMessage}"
            }
        }
    }

    fun renameTopic(topicId: String, newName: String, chapterId: String) {
        if (newName.isBlank()) {
            _errorMessage.value = "Topic name cannot be empty"
            return
        }
        viewModelScope.launch {
            try {
                repository.renameTopic(topicId, newName)
                refreshTopics(chapterId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to rename topic: ${e.localizedMessage}"
            }
        }
    }

    fun deleteTopic(topicId: String, chapterId: String, subjectId: String) {
        viewModelScope.launch {
            try {
                repository.deleteTopic(topicId)
                refreshTopics(chapterId)
                refreshChapters(subjectId)
                refreshSubjects()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete topic: ${e.localizedMessage}"
            }
        }
    }

    fun updateProfile(country: String, educationSystem: String, grade: String) {
        viewModelScope.launch {
            val current = _userProfile.value
            val updated = current?.copy(
                country = country,
                educationSystem = educationSystem,
                grade = grade
            ) ?: UserProfile(
                country = country,
                educationSystem = educationSystem,
                grade = grade
            )
            repository.saveUserProfile(updated)
            _userProfile.value = updated
        }
    }

    fun updateUserSettings(dailyLimitMinutes: Int, evidenceRequired: Boolean) {
        viewModelScope.launch {
            try {
                repository.updateUserSettings(dailyLimitMinutes, evidenceRequired)
                val current = _userProfile.value
                _userProfile.value = current?.copy(
                    dailyStudyLimitMinutes = dailyLimitMinutes,
                    evidenceRequired = evidenceRequired
                )
                loadAcademicStats()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update settings: ${e.localizedMessage}"
            }
        }
    }

    // -------------------------------------------------------------
    // Phase 2: Study Flow Actions
    // -------------------------------------------------------------

    fun loadChapterOverview(chapterId: String) {
        viewModelScope.launch {
            try {
                _currentChapter.value = repository.getChapterById(chapterId)
                _chapterStats.value = repository.getChapterStats(chapterId)
                refreshTopics(chapterId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load chapter overview: ${e.localizedMessage}"
            }
        }
    }

    fun selectTopic(topicId: String) {
        viewModelScope.launch {
            try {
                val topic = repository.getTopicById(topicId)
                _currentTopic.value = topic
                if (topic != null) {
                    _topicSessions.value = repository.getSessionsForTopic(topicId)
                    _topicEvidence.value = repository.getEvidenceForTopic(topicId)
                    loadTestQuestions(topicId, topic.name)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to select topic: ${e.localizedMessage}"
            }
        }
    }

    fun loadAcademicStats() {
        viewModelScope.launch {
            try {
                _academicStats.value = repository.getAcademicStats()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load academic stats: ${e.localizedMessage}"
            }
        }
    }

    fun updateGuardianEnabled(enabled: Boolean) {
        _guardianSettings.value = _guardianSettings.value.copy(isGuardianEnabled = enabled)
    }

    fun updateAudioReminderEnabled(enabled: Boolean) {
        _guardianSettings.value = _guardianSettings.value.copy(isAudioReminderEnabled = enabled)
    }

    fun updateAlertType(type: GuardianAlertType) {
        _guardianSettings.value = _guardianSettings.value.copy(alertType = type)
    }

    fun updateVolumeLevel(level: GuardianVolumeLevel) {
        _guardianSettings.value = _guardianSettings.value.copy(volumeLevel = level)
    }

    fun updateVibrationEnabled(enabled: Boolean) {
        _guardianSettings.value = _guardianSettings.value.copy(isVibrationEnabled = enabled)
    }

    fun recordStudySession(
        topicId: String,
        startTime: Long,
        endTime: Long,
        durationSeconds: Long,
        targetMinutes: Int,
        activeSeconds: Long = durationSeconds,
        pauseSeconds: Long = 0L,
        breakSeconds: Long = 0L,
        isGuardianEnabled: Boolean = false,
        interruptionCount: Int = 0,
        absenceEventsCount: Int = 0,
        onSessionRecorded: (StudySession) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val topic = repository.getTopicById(topicId) ?: return@launch
                val chapter = repository.getChapterById(topic.chapterId)
                val subjectId = chapter?.subjectId ?: ""

                val session = repository.recordStudySession(
                    topicId = topicId,
                    chapterId = topic.chapterId,
                    subjectId = subjectId,
                    startTime = startTime,
                    endTime = endTime,
                    durationSeconds = durationSeconds,
                    targetMinutes = targetMinutes,
                    activeSeconds = activeSeconds,
                    pauseSeconds = pauseSeconds,
                    breakSeconds = breakSeconds,
                    isGuardianEnabled = isGuardianEnabled,
                    interruptionCount = interruptionCount,
                    absenceEventsCount = absenceEventsCount
                )

                // Update topic state if not yet tested or passed
                val isEvidenceReq = _userProfile.value?.evidenceRequired ?: true
                val nextState = if (isEvidenceReq) {
                    TopicState.EVIDENCE_REQUIRED
                } else {
                    TopicState.TEST_AVAILABLE
                }

                if (topic.state == TopicState.NOT_STARTED || topic.state == TopicState.STUDYING) {
                    repository.updateTopicState(topicId, nextState)
                    _currentTopic.value = topic.copy(state = nextState)
                }

                _lastCompletedSession.value = session
                _topicSessions.value = repository.getSessionsForTopic(topicId)
                loadAcademicStats()
                chapter?.let { loadChapterOverview(it.id) }
                onSessionRecorded(session)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to record study session: ${e.localizedMessage}"
            }
        }
    }

    fun saveEvidence(
        topicId: String,
        sessionId: String?,
        imagePath: String,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val evidence = repository.saveEvidence(topicId, sessionId, imagePath, notes)
                // Advance topic state to TEST_AVAILABLE
                val current = _currentTopic.value
                if (current?.id == topicId) {
                    val nextState = TopicState.TEST_AVAILABLE
                    repository.updateTopicState(topicId, nextState)
                    _currentTopic.value = current.copy(state = nextState)
                }
                _topicEvidence.value = repository.getEvidenceForTopic(topicId)
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to save evidence: ${e.localizedMessage}"
            }
        }
    }

    fun skipEvidence(topicId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val current = _currentTopic.value
                if (current?.id == topicId) {
                    val nextState = TopicState.TEST_AVAILABLE
                    repository.updateTopicState(topicId, nextState)
                    _currentTopic.value = current.copy(state = nextState)
                }
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update state: ${e.localizedMessage}"
            }
        }
    }

    fun loadTestQuestions(topicId: String, topicName: String? = null) {
        viewModelScope.launch {
            try {
                val name = topicName ?: repository.getTopicById(topicId)?.name ?: "Study Topic"
                val questions = repository.getOrGenerateTestQuestions(topicId, name)
                _testQuestions.value = questions
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load test questions: ${e.localizedMessage}"
            }
        }
    }

    fun submitTest(
        topicId: String,
        selectedAnswers: Map<Int, Int>, // questionNumber (1..5) -> selectedOptionIndex (0..3)
        onTestEvaluated: (TestResultData) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val questions = _testQuestions.value.ifEmpty {
                    val topic = repository.getTopicById(topicId)
                    repository.getOrGenerateTestQuestions(topicId, topic?.name ?: "Topic")
                }

                var score = 0
                val reviews = mutableListOf<QuestionReview>()

                questions.forEach { question ->
                    val selected = selectedAnswers[question.questionNumber] ?: -1
                    val isCorrect = selected == question.correctOptionIndex
                    if (isCorrect) score++
                    reviews.add(QuestionReview(question, selected, isCorrect))
                }

                val total = questions.size.coerceAtLeast(1)
                val isPassed = score >= 4 // 70% threshold (4 out of 5)
                val percentage = ((score.toDouble() / total.toDouble()) * 100).toInt()

                repository.recordTestAttempt(topicId, score, total)

                // Refresh topic
                val updatedTopic = repository.getTopicById(topicId)
                _currentTopic.value = updatedTopic

                // Refresh chapter stats and academic progress
                updatedTopic?.let {
                    loadChapterOverview(it.chapterId)
                }
                loadAcademicStats()

                val resultData = TestResultData(
                    score = score,
                    totalQuestions = total,
                    percentage = percentage,
                    isPassed = isPassed,
                    reviews = reviews
                )
                _lastTestResult.value = resultData
                onTestEvaluated(resultData)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to evaluate test: ${e.localizedMessage}"
            }
        }
    }

    // -------------------------------------------------------------
    // Phase 3: Study Materials Methods
    // -------------------------------------------------------------
    fun loadMaterials() {
        viewModelScope.launch {
            try {
                repository.getAllMaterialsWithDetailsFlow().collectLatest { list ->
                    _materials.value = list
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load materials: ${e.localizedMessage}"
            }
        }
    }

    fun selectMaterial(materialId: String) {
        viewModelScope.launch {
            try {
                _currentMaterial.value = repository.getMaterialWithDetails(materialId)
                repository.updateMaterialLastOpened(materialId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to select material: ${e.localizedMessage}"
            }
        }
    }

    fun addStudyMaterial(
        name: String,
        type: MaterialType,
        uriOrPath: String,
        subjectId: String,
        chapterId: String,
        topicId: String? = null,
        durationMinutes: Int? = null,
        notes: String? = null,
        onAdded: (() -> Unit)? = null
    ) {
        if (name.isBlank()) {
            _errorMessage.value = "Material name cannot be empty"
            return
        }
        if (uriOrPath.isBlank()) {
            _errorMessage.value = "File path or content cannot be empty"
            return
        }
        viewModelScope.launch {
            try {
                repository.saveStudyMaterial(
                    name = name,
                    type = type,
                    uriOrPath = uriOrPath,
                    subjectId = subjectId,
                    chapterId = chapterId,
                    topicId = topicId,
                    durationMinutes = durationMinutes,
                    notes = notes
                )
                loadAcademicStats()
                onAdded?.invoke()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add material: ${e.localizedMessage}"
            }
        }
    }

    fun updateStudyMaterial(material: StudyMaterial, onUpdated: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                repository.updateStudyMaterial(material)
                _currentMaterial.value = repository.getMaterialWithDetails(material.id)
                onUpdated?.invoke()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update material: ${e.localizedMessage}"
            }
        }
    }

    fun deleteStudyMaterial(materialId: String) {
        viewModelScope.launch {
            try {
                repository.deleteStudyMaterial(materialId)
                if (_currentMaterial.value?.material?.id == materialId) {
                    _currentMaterial.value = null
                }
                loadAcademicStats()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete material: ${e.localizedMessage}"
            }
        }
    }

    // Material Study Tracking + Floating Guardian Mode
    fun startMaterialStudy(materialId: String) {
        viewModelScope.launch {
            val details = repository.getMaterialWithDetails(materialId) ?: return@launch
            _currentMaterial.value = details
            _isMaterialStudying.value = true
            _activeMaterialStudySeconds.value = 0L
            materialSessionStartTime = System.currentTimeMillis()

            materialStudyJob?.cancel()
            materialStudyJob = viewModelScope.launch {
                while (_isMaterialStudying.value) {
                    delay(1000L)
                    _activeMaterialStudySeconds.value += 1
                }
            }
        }
    }

    fun stopMaterialStudy(onFinished: (() -> Unit)? = null) {
        val mat = _currentMaterial.value ?: return
        val activeSecs = _activeMaterialStudySeconds.value
        val startTime = materialSessionStartTime
        val endTime = System.currentTimeMillis()

        materialStudyJob?.cancel()
        _isMaterialStudying.value = false

        viewModelScope.launch {
            try {
                if (activeSecs >= 5) {
                    repository.recordMaterialStudySession(
                        materialId = mat.material.id,
                        subjectId = mat.material.subjectId,
                        chapterId = mat.material.chapterId,
                        topicId = mat.material.topicId,
                        startTime = startTime,
                        endTime = endTime,
                        activeDurationSeconds = activeSecs
                    )
                    loadAcademicStats()
                }
                onFinished?.invoke()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to save material study session: ${e.localizedMessage}"
            }
        }
    }

    fun cycleGuardianCorner() {
        val next = when (_guardianCorner.value) {
            GuardianCorner.TOP_RIGHT -> GuardianCorner.BOTTOM_RIGHT
            GuardianCorner.BOTTOM_RIGHT -> GuardianCorner.BOTTOM_LEFT
            GuardianCorner.BOTTOM_LEFT -> GuardianCorner.TOP_LEFT
            GuardianCorner.TOP_LEFT -> GuardianCorner.TOP_RIGHT
        }
        _guardianCorner.value = next
    }

    // -------------------------------------------------------------
    // Phase 3: Adaptive Planner Methods
    // -------------------------------------------------------------
    fun loadPlannedTasks(dateString: String? = null) {
        val date = dateString ?: _selectedPlannerDate.value
        _selectedPlannerDate.value = date
        viewModelScope.launch {
            try {
                repository.getPlannedTasksWithDetailsFlow(date).collectLatest { tasks ->
                    _plannedTasks.value = tasks
                    if (tasks.isEmpty() && !_isGeneratingPlan.value) {
                        generateAdaptivePlan(date)
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load planner: ${e.localizedMessage}"
            }
        }
    }

    fun generateAdaptivePlan(dateString: String? = null, activeChapterId: String? = null) {
        val date = dateString ?: _selectedPlannerDate.value
        viewModelScope.launch {
            _isGeneratingPlan.value = true
            try {
                val generated = repository.generateAdaptivePlan(date, activeChapterId)
                _plannedTasks.value = generated
            } catch (e: Exception) {
                _errorMessage.value = "Failed to generate study plan: ${e.localizedMessage}"
            } finally {
                _isGeneratingPlan.value = false
            }
        }
    }

    fun setPlannerDate(dateString: String) {
        _selectedPlannerDate.value = dateString
        loadPlannedTasks(dateString)
    }

    fun markPlannerTaskCompleted(taskId: String) {
        viewModelScope.launch {
            try {
                repository.updatePlannerTaskStatus(taskId, PlannerTaskStatus.COMPLETED)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to complete task: ${e.localizedMessage}"
            }
        }
    }

    fun skipPlannerTask(taskId: String) {
        viewModelScope.launch {
            try {
                repository.updatePlannerTaskStatus(taskId, PlannerTaskStatus.SKIPPED)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to skip task: ${e.localizedMessage}"
            }
        }
    }

    fun deletePlannerTask(taskId: String) {
        viewModelScope.launch {
            try {
                repository.deletePlannerTask(taskId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to remove task: ${e.localizedMessage}"
            }
        }
    }

    // -------------------------------------------------------------
    // Phase 5: Pluto AI Assistant Actions
    // -------------------------------------------------------------
    fun sendPlutoMessage(query: String) {
        if (query.isBlank()) return
        val currentAttach = _plutoAttachment.value
        val userMsg = PlutoMessage(
            sender = "Student",
            content = query,
            attachment = currentAttach
        )
        _plutoMessages.value = _plutoMessages.value + userMsg
        _isPlutoThinking.value = true

        viewModelScope.launch {
            delay(280) // Smooth response timing
            val subjs = _subjects.value.map { it.subject }
            val response = PlutoEngine.generateResponse(
                query = query,
                attachment = currentAttach,
                userProfile = _userProfile.value,
                subjects = subjs,
                recentMessages = _plutoMessages.value.takeLast(8)
            )

            val plutoMsg = PlutoMessage(
                sender = "Pluto",
                content = response.content,
                intent = response.intent,
                isOfflineFallback = response.isOfflineFallback
            )
            _plutoMessages.value = _plutoMessages.value + plutoMsg

            if (response.generatedQuiz != null) {
                _activePlutoQuiz.value = response.generatedQuiz
            }
            _isPlutoThinking.value = false
        }
    }

    fun attachFileToPluto(attachment: PlutoAttachment) {
        _plutoAttachment.value = attachment
        val notice = PlutoMessage(
            sender = "Pluto",
            content = "Attached: **${attachment.name}** (${if (attachment.isImage) "Image" else "Document"}). You can ask questions about this file, or tap **'Quiz me from this'** to generate a practice quiz.",
            attachment = attachment,
            intent = if (attachment.isImage) PlutoIntent.IMAGE_QUESTION else PlutoIntent.FILE_QUESTION
        )
        _plutoMessages.value = _plutoMessages.value + notice
    }

    fun clearPlutoAttachment() {
        _plutoAttachment.value = null
    }

    fun startPlutoQuizFromAttachment() {
        val attachment = _plutoAttachment.value
        if (attachment == null) {
            _plutoMessages.value = _plutoMessages.value + PlutoMessage(
                sender = "Pluto",
                content = "Please attach a document, study file, or notes first before starting a practice quiz.",
                intent = PlutoIntent.QUIZ_FROM_ATTACHMENT
            )
            return
        }

        val quiz = PlutoEngine.generatePracticeQuiz(attachment, _userProfile.value)
        _activePlutoQuiz.value = quiz
        _plutoMessages.value = _plutoMessages.value + PlutoMessage(
            sender = "Pluto",
            content = "Generated practice quiz from **${attachment.name}**. Answer each question below to test your recall.\n\n*Reminder: Practice quizzes are separate practice activities and do NOT alter official StudyPilot topic test scores or mastery progress.*",
            intent = PlutoIntent.QUIZ_FROM_ATTACHMENT
        )
    }

    fun answerPlutoQuizQuestion(questionIndex: Int, selectedOptionIndex: Int) {
        val currentQuiz = _activePlutoQuiz.value ?: return
        val updated = currentQuiz.userSelectedAnswers.toMutableMap()
        updated[questionIndex] = selectedOptionIndex
        _activePlutoQuiz.value = currentQuiz.copy(userSelectedAnswers = updated)
    }

    fun nextPlutoQuizQuestion() {
        val currentQuiz = _activePlutoQuiz.value ?: return
        if (currentQuiz.currentQuestionIndex < currentQuiz.totalQuestions - 1) {
            _activePlutoQuiz.value = currentQuiz.copy(
                currentQuestionIndex = currentQuiz.currentQuestionIndex + 1
            )
        } else {
            finishPlutoQuiz()
        }
    }

    fun finishPlutoQuiz() {
        val currentQuiz = _activePlutoQuiz.value ?: return
        val completedQuiz = currentQuiz.copy(isCompleted = true)
        _activePlutoQuiz.value = completedQuiz

        val scoreSummary = "Practice Quiz Completed for **${currentQuiz.materialTitle}**:\n\n" +
                "• Questions Attempted: ${completedQuiz.attemptedQuestions} / ${completedQuiz.totalQuestions}\n" +
                "• Correct Answers: ${completedQuiz.correctAnswers}\n" +
                "• Incorrect Answers: ${completedQuiz.incorrectAnswers}\n" +
                "• Final Score: ${completedQuiz.percentage}%\n\n" +
                "Note: Official topic mastery remains governed exclusively by official StudyPilot tests."

        _plutoMessages.value = _plutoMessages.value + PlutoMessage(
            sender = "Pluto",
            content = scoreSummary,
            intent = PlutoIntent.QUIZ_FROM_ATTACHMENT
        )
    }

    fun resetPlutoQuiz() {
        _activePlutoQuiz.value = null
    }

    // -------------------------------------------------------------
    // Phase 5: Google Authentication Actions
    // -------------------------------------------------------------
    fun updateGoogleAccount(user: GoogleUserData) {
        _googleUser.value = user
        viewModelScope.launch {
            val current = _userProfile.value
            if (current != null) {
                val updated = current.copy(
                    googleId = user.id,
                    googleEmail = user.email,
                    googleDisplayName = user.displayName,
                    googlePhotoUrl = user.photoUrl
                )
                repository.saveUserProfile(updated)
                _userProfile.value = updated
            }
        }
    }

    fun signOutGoogle() {
        viewModelScope.launch {
            googleAuthManager?.signOut()
            _googleUser.value = null
            val current = _userProfile.value
            if (current != null) {
                val updated = current.copy(
                    googleId = null,
                    googleEmail = null,
                    googleDisplayName = null,
                    googlePhotoUrl = null
                )
                repository.saveUserProfile(updated)
                _userProfile.value = updated
            }
        }
    }

    // -------------------------------------------------------------
    // Phase 5: App Lock Actions
    // -------------------------------------------------------------
    fun setAppLockPin(pin: String) {
        appLockManager?.setPin(pin)
    }

    fun verifyAppLockPin(pin: String): Boolean {
        return appLockManager?.verifyPin(pin) ?: true
    }

    fun unlockApp() {
        appLockManager?.unlock()
    }

    fun lockApp() {
        appLockManager?.lockManually()
    }

    fun toggleAppLock(enabled: Boolean) {
        appLockManager?.toggleAppLock(enabled)
    }

    fun toggleBiometric(enabled: Boolean) {
        appLockManager?.toggleBiometric(enabled)
    }

    fun setAutoLockTimeout(timeout: AutoLockTimeout) {
        appLockManager?.setAutoLockTimeout(timeout)
    }

    fun promptBiometric(activity: FragmentActivity, onSuccess: () -> Unit, onError: (String) -> Unit) {
        appLockManager?.promptBiometric(activity, onSuccess, onError)
    }

    fun isBiometricHardwareAvailable(): Boolean {
        return appLockManager?.isBiometricHardwareAvailable() ?: false
    }

    // -------------------------------------------------------------
    // Phase 5: Notification Actions
    // -------------------------------------------------------------
    fun updateNotificationPreferences(prefs: NotificationPreferences) {
        notificationManager?.updatePreferences(prefs)
    }

    fun triggerTestNotification(): Boolean {
        return notificationManager?.sendNotification(
            NotificationType.STUDY_REMINDER,
            "StudyPilot Reminder: Consistent daily study is the key to academic mastery. Open your syllabus to begin today's focus block!"
        ) ?: false
    }

    fun hasNotificationPermission(): Boolean {
        return notificationManager?.hasNotificationPermission() ?: true
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

class StudyPilotViewModelFactory(
    private val repository: StudyPilotRepository,
    private val appLockManager: AppLockManager? = null,
    private val googleAuthManager: GoogleAuthManager? = null,
    private val notificationManager: StudyNotificationManager? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudyPilotViewModel::class.java)) {
            return StudyPilotViewModel(repository, appLockManager, googleAuthManager, notificationManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
