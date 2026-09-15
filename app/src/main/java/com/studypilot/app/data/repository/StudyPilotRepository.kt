package com.studypilot.app.data.repository

import com.studypilot.app.data.local.QuestionBank
import com.studypilot.app.data.local.StudyPilotDatabase
import com.studypilot.app.data.model.AcademicStats
import com.studypilot.app.data.model.Chapter
import com.studypilot.app.data.model.ChapterStats
import com.studypilot.app.data.model.ChapterWithCounts
import com.studypilot.app.data.model.Evidence
import com.studypilot.app.data.model.MaterialStudySession
import com.studypilot.app.data.model.MaterialType
import com.studypilot.app.data.model.MaterialWithDetails
import com.studypilot.app.data.model.PlannerTask
import com.studypilot.app.data.model.PlannerTaskStatus
import com.studypilot.app.data.model.PlannerTaskWithDetails
import com.studypilot.app.data.model.StudyMaterial
import com.studypilot.app.data.model.StudySession
import com.studypilot.app.data.model.Subject
import com.studypilot.app.data.model.SubjectProgress
import com.studypilot.app.data.model.SubjectWithCounts
import com.studypilot.app.data.model.TestAttempt
import com.studypilot.app.data.model.TestQuestion
import com.studypilot.app.data.model.Topic
import com.studypilot.app.data.model.TopicState
import com.studypilot.app.data.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class StudyPilotRepository(private val database: StudyPilotDatabase) {

    private val userProfileDao = database.userProfileDao()
    private val subjectDao = database.subjectDao()
    private val chapterDao = database.chapterDao()
    private val topicDao = database.topicDao()
    private val studySessionDao = database.studySessionDao()
    private val evidenceDao = database.evidenceDao()
    private val testDao = database.testDao()
    private val studyMaterialDao = database.studyMaterialDao()
    private val materialStudySessionDao = database.materialStudySessionDao()
    private val plannerTaskDao = database.plannerTaskDao()

    // -------------------------------------------------------------
    // User Profile
    // -------------------------------------------------------------
    fun getUserProfileFlow(): Flow<UserProfile?> = userProfileDao.getProfileFlow()

    suspend fun getUserProfile(): UserProfile? = withContext(Dispatchers.IO) {
        userProfileDao.getProfile()
    }

    suspend fun saveUserProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        userProfileDao.insertOrUpdate(profile)
    }

    suspend fun clearUserProfile() = withContext(Dispatchers.IO) {
        userProfileDao.clearProfile()
    }

    // -------------------------------------------------------------
    // Subjects
    // -------------------------------------------------------------
    fun getSubjectsWithCountsFlow(): Flow<List<SubjectWithCounts>> {
        return combine(
            subjectDao.getAllSubjectsFlow(),
            chapterDao.getChaptersForSubjectFlow(""), // trigger emission or fetch dynamically
            topicDao.getTopicsForChapterFlow("")
        ) { subjects, _, _ ->
            // Flow combine fallback or custom mapping
            subjects.map { subject ->
                val chCount = chapterDao.countChaptersForSubject(subject.id)
                val topCount = topicDao.countTopicsForSubject(subject.id)
                SubjectWithCounts(subject, chCount, topCount)
            }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getSubjectsWithCounts(): List<SubjectWithCounts> = withContext(Dispatchers.IO) {
        val subjects = subjectDao.getAllSubjects()
        subjects.map { subject ->
            val chCount = chapterDao.countChaptersForSubject(subject.id)
            val topCount = topicDao.countTopicsForSubject(subject.id)
            SubjectWithCounts(subject, chCount, topCount)
        }
    }

    suspend fun getSubjectById(subjectId: String): Subject? = withContext(Dispatchers.IO) {
        subjectDao.getSubjectById(subjectId)
    }

    suspend fun addSubject(name: String, colorHex: String = "#C4975A"): Subject = withContext(Dispatchers.IO) {
        val existing = subjectDao.getAllSubjects()
        val subject = Subject(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            orderIndex = existing.size,
            colorHex = colorHex
        )
        subjectDao.insert(subject)
        subject
    }

    suspend fun renameSubject(subjectId: String, newName: String) = withContext(Dispatchers.IO) {
        val subject = subjectDao.getSubjectById(subjectId) ?: return@withContext
        subjectDao.update(subject.copy(name = newName.trim()))
    }

    suspend fun deleteSubject(subjectId: String) = withContext(Dispatchers.IO) {
        subjectDao.deleteById(subjectId)
    }

    suspend fun reorderSubjects(orderedIds: List<String>) = withContext(Dispatchers.IO) {
        val subjects = subjectDao.getAllSubjects()
        val subjectMap = subjects.associateBy { it.id }
        orderedIds.forEachIndexed { index, id ->
            subjectMap[id]?.let {
                if (it.orderIndex != index) {
                    subjectDao.update(it.copy(orderIndex = index))
                }
            }
        }
    }

    // -------------------------------------------------------------
    // Chapters
    // -------------------------------------------------------------
    suspend fun getChaptersWithCounts(subjectId: String): List<ChapterWithCounts> = withContext(Dispatchers.IO) {
        val chapters = chapterDao.getChaptersForSubject(subjectId)
        chapters.map { chapter ->
            val topCount = topicDao.countTopicsForChapter(chapter.id)
            ChapterWithCounts(chapter, topCount)
        }
    }

    fun getChaptersFlow(subjectId: String): Flow<List<Chapter>> = chapterDao.getChaptersForSubjectFlow(subjectId)

    suspend fun getChapterById(chapterId: String): Chapter? = withContext(Dispatchers.IO) {
        chapterDao.getChapterById(chapterId)
    }

    suspend fun addChapter(subjectId: String, name: String, estimatedMinutes: Int = 45): Chapter = withContext(Dispatchers.IO) {
        val existing = chapterDao.getChaptersForSubject(subjectId)
        val chapter = Chapter(
            id = UUID.randomUUID().toString(),
            subjectId = subjectId,
            name = name.trim(),
            orderIndex = existing.size,
            estimatedMinutes = estimatedMinutes
        )
        chapterDao.insert(chapter)
        chapter
    }

    suspend fun renameChapter(chapterId: String, newName: String) = withContext(Dispatchers.IO) {
        val chapter = chapterDao.getChapterById(chapterId) ?: return@withContext
        chapterDao.update(chapter.copy(name = newName.trim()))
    }

    suspend fun deleteChapter(chapterId: String) = withContext(Dispatchers.IO) {
        chapterDao.deleteById(chapterId)
    }

    suspend fun reorderChapters(subjectId: String, orderedIds: List<String>) = withContext(Dispatchers.IO) {
        val chapters = chapterDao.getChaptersForSubject(subjectId)
        val chapterMap = chapters.associateBy { it.id }
        orderedIds.forEachIndexed { index, id ->
            chapterMap[id]?.let {
                if (it.orderIndex != index) {
                    chapterDao.update(it.copy(orderIndex = index))
                }
            }
        }
    }

    // -------------------------------------------------------------
    // Topics
    // -------------------------------------------------------------
    fun getTopicsFlow(chapterId: String): Flow<List<Topic>> = topicDao.getTopicsForChapterFlow(chapterId)

    suspend fun getTopics(chapterId: String): List<Topic> = withContext(Dispatchers.IO) {
        topicDao.getTopicsForChapter(chapterId)
    }

    suspend fun getTopicById(topicId: String): Topic? = withContext(Dispatchers.IO) {
        topicDao.getTopicById(topicId)
    }

    suspend fun addTopic(chapterId: String, name: String, estimatedMinutes: Int = 25): Topic = withContext(Dispatchers.IO) {
        val existing = topicDao.getTopicsForChapter(chapterId)
        val topic = Topic(
            id = UUID.randomUUID().toString(),
            chapterId = chapterId,
            name = name.trim(),
            orderIndex = existing.size,
            state = TopicState.NOT_STARTED,
            estimatedMinutes = estimatedMinutes
        )
        topicDao.insert(topic)
        topic
    }

    suspend fun renameTopic(topicId: String, newName: String) = withContext(Dispatchers.IO) {
        val topic = topicDao.getTopicById(topicId) ?: return@withContext
        topicDao.update(topic.copy(name = newName.trim()))
    }

    suspend fun deleteTopic(topicId: String) = withContext(Dispatchers.IO) {
        topicDao.deleteById(topicId)
    }

    suspend fun reorderTopics(chapterId: String, orderedIds: List<String>) = withContext(Dispatchers.IO) {
        val topics = topicDao.getTopicsForChapter(chapterId)
        val topicMap = topics.associateBy { it.id }
        orderedIds.forEachIndexed { index, id ->
            topicMap[id]?.let {
                if (it.orderIndex != index) {
                    topicDao.update(it.copy(orderIndex = index))
                }
            }
        }
    }

    suspend fun updateTopicState(topicId: String, state: TopicState) = withContext(Dispatchers.IO) {
        topicDao.updateTopicState(topicId, state)
    }

    // -------------------------------------------------------------
    // Study Sessions
    // -------------------------------------------------------------
    suspend fun recordStudySession(
        topicId: String,
        chapterId: String,
        subjectId: String,
        startTime: Long,
        endTime: Long,
        durationSeconds: Long,
        targetMinutes: Int,
        activeSeconds: Long = durationSeconds,
        pauseSeconds: Long = 0L,
        breakSeconds: Long = 0L,
        isGuardianEnabled: Boolean = false,
        interruptionCount: Int = 0,
        absenceEventsCount: Int = 0
    ): StudySession = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateString = dateFormat.format(Date(startTime))
        val session = StudySession(
            id = UUID.randomUUID().toString(),
            topicId = topicId,
            chapterId = chapterId,
            subjectId = subjectId,
            startTime = startTime,
            endTime = endTime,
            durationSeconds = durationSeconds,
            targetMinutes = targetMinutes,
            isCompleted = true,
            dateString = dateString,
            activeSeconds = activeSeconds,
            pauseSeconds = pauseSeconds,
            breakSeconds = breakSeconds,
            isGuardianEnabled = isGuardianEnabled,
            interruptionCount = interruptionCount,
            absenceEventsCount = absenceEventsCount
        )
        studySessionDao.insert(session)
        session
    }

    suspend fun getSessionsForTopic(topicId: String): List<StudySession> = withContext(Dispatchers.IO) {
        studySessionDao.getSessionsForTopic(topicId)
    }

    fun getAllSessionsFlow(): Flow<List<StudySession>> = studySessionDao.getAllSessionsFlow()

    // -------------------------------------------------------------
    // Evidence Verification
    // -------------------------------------------------------------
    suspend fun saveEvidence(
        topicId: String,
        sessionId: String?,
        imagePath: String,
        notes: String
    ): Evidence = withContext(Dispatchers.IO) {
        val evidence = Evidence(
            id = UUID.randomUUID().toString(),
            topicId = topicId,
            sessionId = sessionId,
            imagePath = imagePath,
            notes = notes.trim(),
            timestamp = System.currentTimeMillis()
        )
        evidenceDao.insert(evidence)
        evidence
    }

    suspend fun getEvidenceForTopic(topicId: String): List<Evidence> = withContext(Dispatchers.IO) {
        evidenceDao.getEvidenceForTopic(topicId)
    }

    // -------------------------------------------------------------
    // Tests & Test Attempts
    // -------------------------------------------------------------
    suspend fun getOrGenerateTestQuestions(topicId: String, topicName: String): List<TestQuestion> = withContext(Dispatchers.IO) {
        val existing = testDao.getQuestionsForTopic(topicId)
        if (existing.isNotEmpty() && existing.size >= 5) {
            return@withContext existing
        }
        val generated = QuestionBank.getQuestionsForTopic(topicId, topicName)
        testDao.insertQuestions(generated)
        generated
    }

    suspend fun recordTestAttempt(
        topicId: String,
        score: Int,
        totalQuestions: Int = 5
    ): TestAttempt = withContext(Dispatchers.IO) {
        val percentage = ((score.toDouble() / totalQuestions.toDouble()) * 100).toInt()
        val isPassed = score >= 4 // 70% threshold (4 out of 5)

        val attempt = TestAttempt(
            id = UUID.randomUUID().toString(),
            topicId = topicId,
            score = score,
            totalQuestions = totalQuestions,
            percentage = percentage,
            isPassed = isPassed,
            timestamp = System.currentTimeMillis()
        )
        testDao.insertAttempt(attempt)

        // Update Topic State accordingly
        val newState = if (isPassed) TopicState.PASSED else TopicState.NEEDS_REVIEW
        topicDao.updateTopicState(topicId, newState)

        attempt
    }

    suspend fun getTestAttemptsForTopic(topicId: String): List<TestAttempt> = withContext(Dispatchers.IO) {
        testDao.getAttemptsForTopic(topicId)
    }

    // -------------------------------------------------------------
    // Chapter Statistics (for Chapter Overview)
    // -------------------------------------------------------------
    suspend fun getChapterStats(chapterId: String): ChapterStats = withContext(Dispatchers.IO) {
        val totalTopics = topicDao.countTopicsForChapter(chapterId)
        val completed = topicDao.countCompletedTopicsForChapter(chapterId)
        val inProgress = topicDao.countInProgressTopicsForChapter(chapterId)
        val remaining = (totalTopics - completed).coerceAtLeast(0)
        val studySeconds = studySessionDao.getChapterStudySeconds(chapterId) ?: 0L
        val avgScore = testDao.getAveragePercentageForChapter(chapterId)?.toInt()
        val attempts = testDao.getAttemptsForChapter(chapterId)

        ChapterStats(
            totalTopics = totalTopics,
            completedTopics = completed,
            inProgressTopics = inProgress,
            remainingTopics = remaining,
            totalStudySeconds = studySeconds,
            averageScorePercentage = avgScore,
            testsTaken = attempts.size
        )
    }

    // -------------------------------------------------------------
    // Real Academic Progress (for Progress & Home screens)
    // -------------------------------------------------------------
    suspend fun getAcademicStats(): AcademicStats = withContext(Dispatchers.IO) {
        val profile = userProfileDao.getProfile()
        val dailyLimit = profile?.dailyStudyLimitMinutes ?: 120

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = dateFormat.format(Date())

        val totalTopics = topicDao.getAllTopics()
        val totalCount = totalTopics.size
        val completedCount = totalTopics.count { it.state == TopicState.PASSED || it.state == TopicState.COMPLETED }
        val inProgressCount = totalTopics.count {
            it.state != TopicState.NOT_STARTED && it.state != TopicState.PASSED && it.state != TopicState.COMPLETED
        }
        val completionPct = if (totalCount > 0) ((completedCount.toDouble() / totalCount) * 100).toInt() else 0

        val normalTotalSeconds = studySessionDao.getTotalStudySeconds() ?: 0L
        val materialTotalSeconds = materialStudySessionDao.getTotalMaterialStudySeconds() ?: 0L
        val totalSeconds = normalTotalSeconds + materialTotalSeconds

        val normalTodaySeconds = studySessionDao.getTodayStudySeconds(todayStr) ?: 0L
        val materialTodaySeconds = materialStudySessionDao.getTodayMaterialStudySeconds(todayStr) ?: 0L
        val todaySeconds = normalTodaySeconds + materialTodaySeconds

        val materialsCount = studyMaterialDao.countMaterials()

        val allAttempts = testDao.getAllAttempts()
        val passedCount = allAttempts.count { it.isPassed }
        val avgScore = testDao.getOverallAveragePercentage()?.toInt()

        // Breakdown per subject
        val subjects = subjectDao.getAllSubjects()
        val subjectProgress = subjects.map { subject ->
            val subTopics = topicDao.countTopicsForSubject(subject.id)
            val subCompleted = topicDao.countCompletedTopicsForSubject(subject.id)
            val subTime = studySessionDao.getSubjectStudySeconds(subject.id) ?: 0L
            val subMatTime = materialStudySessionDao.getSubjectMaterialStudySeconds(subject.id) ?: 0L
            SubjectProgress(
                subjectId = subject.id,
                subjectName = subject.name,
                colorHex = subject.colorHex,
                totalTopics = subTopics,
                completedTopics = subCompleted,
                totalStudySeconds = subTime + subMatTime
            )
        }

        AcademicStats(
            totalTopicsCount = totalCount,
            completedTopicsCount = completedCount,
            inProgressTopicsCount = inProgressCount,
            completionPercentage = completionPct,
            totalStudySeconds = totalSeconds,
            todayStudySeconds = todaySeconds,
            dailyLimitMinutes = dailyLimit,
            testsAttempted = allAttempts.size,
            testsPassed = passedCount,
            overallAverageScore = avgScore,
            subjectProgressList = subjectProgress,
            totalMaterialsCount = materialsCount,
            totalMaterialStudySeconds = materialTotalSeconds
        )
    }

    suspend fun updateUserSettings(dailyLimitMinutes: Int, evidenceRequired: Boolean) = withContext(Dispatchers.IO) {
        val current = userProfileDao.getProfile() ?: return@withContext
        val updated = current.copy(
            dailyStudyLimitMinutes = dailyLimitMinutes,
            evidenceRequired = evidenceRequired
        )
        userProfileDao.insertOrUpdate(updated)
    }

    // -------------------------------------------------------------
    // Initial Curriculum Generator
    // -------------------------------------------------------------
    suspend fun seedInitialCurriculum(educationSystem: String, grade: String) = withContext(Dispatchers.IO) {
        val currentSubjects = subjectDao.getAllSubjects()
        if (currentSubjects.isNotEmpty()) return@withContext

        data class SeedTopic(val name: String, val minutes: Int = 25)
        data class SeedChapter(val name: String, val topics: List<SeedTopic>)
        data class SeedSubject(val name: String, val color: String, val chapters: List<SeedChapter>)

        val defaultCurriculum = listOf(
            SeedSubject(
                name = "Physics",
                color = "#C4975A",
                chapters = listOf(
                    SeedChapter(
                        name = "Kinematics & Motion",
                        topics = listOf(
                            SeedTopic("Rectilinear Motion & Vectors"),
                            SeedTopic("Speed, Velocity & Acceleration"),
                            SeedTopic("Projectile Motion"),
                            SeedTopic("Relative Motion")
                        )
                    ),
                    SeedChapter(
                        name = "Laws of Motion",
                        topics = listOf(
                            SeedTopic("Newton's First & Second Laws"),
                            SeedTopic("Conservation of Linear Momentum"),
                            SeedTopic("Friction and Contact Forces"),
                            SeedTopic("Circular Motion Dynamics")
                        )
                    ),
                    SeedChapter(
                        name = "Work, Energy & Power",
                        topics = listOf(
                            SeedTopic("Work-Energy Theorem"),
                            SeedTopic("Conservative & Non-Conservative Forces"),
                            SeedTopic("Elastic & Inelastic Collisions")
                        )
                    )
                )
            ),
            SeedSubject(
                name = "Chemistry",
                color = "#8C6239",
                chapters = listOf(
                    SeedChapter(
                        name = "Atomic Structure",
                        topics = listOf(
                            SeedTopic("Bohr Model and Spectra"),
                            SeedTopic("Quantum Numbers & Orbitals"),
                            SeedTopic("Electronic Configurations")
                        )
                    ),
                    SeedChapter(
                        name = "Chemical Bonding",
                        topics = listOf(
                            SeedTopic("Ionic and Covalent Bonds"),
                            SeedTopic("VSEPR Theory & Molecular Geometry"),
                            SeedTopic("Hybridization & Molecular Orbitals")
                        )
                    ),
                    SeedChapter(
                        name = "Thermodynamics",
                        topics = listOf(
                            SeedTopic("First Law & Enthalpy"),
                            SeedTopic("Entropy & Gibbs Free Energy")
                        )
                    )
                )
            ),
            SeedSubject(
                name = "Mathematics",
                color = "#A87C4F",
                chapters = listOf(
                    SeedChapter(
                        name = "Differential Calculus",
                        topics = listOf(
                            SeedTopic("Limits & Continuity"),
                            SeedTopic("Standard Derivatives & Chain Rule"),
                            SeedTopic("Applications of Derivatives & Maxima-Minima")
                        )
                    ),
                    SeedChapter(
                        name = "Integral Calculus",
                        topics = listOf(
                            SeedTopic("Indefinite Integration Techniques"),
                            SeedTopic("Definite Integrals & Fundamental Theorem"),
                            SeedTopic("Areas Under Curves")
                        )
                    ),
                    SeedChapter(
                        name = "Vectors & 3D Geometry",
                        topics = listOf(
                            SeedTopic("Dot and Cross Products"),
                            SeedTopic("Equations of Lines & Planes in Space")
                        )
                    )
                )
            ),
            SeedSubject(
                name = "Biology",
                color = "#6E8B62",
                chapters = listOf(
                    SeedChapter(
                        name = "Cellular Biology",
                        topics = listOf(
                            SeedTopic("Cell Structure & Organelles"),
                            SeedTopic("Membrane Transport & Osmosis"),
                            SeedTopic("Mitosis & Meiosis")
                        )
                    ),
                    SeedChapter(
                        name = "Genetics & Evolution",
                        topics = listOf(
                            SeedTopic("Mendelian Inheritance"),
                            SeedTopic("DNA Replication & Protein Synthesis"),
                            SeedTopic("Mechanisms of Natural Selection")
                        )
                    )
                )
            )
        )

        defaultCurriculum.forEachIndexed { subIdx, seedSub ->
            val subId = UUID.randomUUID().toString()
            val subject = Subject(
                id = subId,
                name = seedSub.name,
                orderIndex = subIdx,
                colorHex = seedSub.color
            )
            subjectDao.insert(subject)

            seedSub.chapters.forEachIndexed { chapIdx, seedChap ->
                val chapId = UUID.randomUUID().toString()
                val chapter = Chapter(
                    id = chapId,
                    subjectId = subId,
                    name = seedChap.name,
                    orderIndex = chapIdx
                )
                chapterDao.insert(chapter)

                seedChap.topics.forEachIndexed { topIdx, seedTop ->
                    val topId = UUID.randomUUID().toString()
                    val topic = Topic(
                        id = topId,
                        chapterId = chapId,
                        name = seedTop.name,
                        orderIndex = topIdx,
                        state = TopicState.NOT_STARTED,
                        estimatedMinutes = seedTop.minutes
                    )
                    topicDao.insert(topic)
                }
            }
        }
    }

    // -------------------------------------------------------------
    // PART A: Study Materials Library
    // -------------------------------------------------------------
    fun getAllMaterialsFlow(): Flow<List<StudyMaterial>> = studyMaterialDao.getAllMaterialsFlow()

    fun getAllMaterialsWithDetailsFlow(): Flow<List<MaterialWithDetails>> {
        return combine(
            studyMaterialDao.getAllMaterialsFlow(),
            subjectDao.getAllSubjectsFlow(),
            chapterDao.getAllChaptersFlow(),
            topicDao.getAllTopicsFlow()
        ) { materials, subjects, chapters, topics ->
            val subMap = subjects.associateBy { it.id }
            val chMap = chapters.associateBy { it.id }
            val topMap = topics.associateBy { it.id }

            materials.map { mat ->
                MaterialWithDetails(
                    material = mat,
                    subjectName = subMap[mat.subjectId]?.name ?: "Unknown Subject",
                    chapterName = chMap[mat.chapterId]?.name ?: "Unknown Chapter",
                    topicName = mat.topicId?.let { topMap[it]?.name }
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getMaterialById(id: String): StudyMaterial? = withContext(Dispatchers.IO) {
        studyMaterialDao.getMaterialById(id)
    }

    suspend fun getMaterialWithDetails(id: String): MaterialWithDetails? = withContext(Dispatchers.IO) {
        val mat = studyMaterialDao.getMaterialById(id) ?: return@withContext null
        val sub = subjectDao.getSubjectById(mat.subjectId)
        val ch = chapterDao.getChapterById(mat.chapterId)
        val top = mat.topicId?.let { topicDao.getTopicById(it) }
        MaterialWithDetails(
            material = mat,
            subjectName = sub?.name ?: "Unknown Subject",
            chapterName = ch?.name ?: "Unknown Chapter",
            topicName = top?.name
        )
    }

    suspend fun saveStudyMaterial(
        name: String,
        type: MaterialType,
        uriOrPath: String,
        subjectId: String,
        chapterId: String,
        topicId: String? = null,
        durationMinutes: Int? = null,
        notes: String? = null
    ): StudyMaterial = withContext(Dispatchers.IO) {
        val material = StudyMaterial(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            type = type,
            uriOrPath = uriOrPath,
            subjectId = subjectId,
            chapterId = chapterId,
            topicId = topicId?.takeIf { it.isNotBlank() },
            dateAdded = System.currentTimeMillis(),
            lastOpened = null,
            durationMinutes = durationMinutes,
            notes = notes?.trim()
        )
        studyMaterialDao.insertMaterial(material)
        material
    }

    suspend fun updateStudyMaterial(material: StudyMaterial) = withContext(Dispatchers.IO) {
        studyMaterialDao.updateMaterial(material)
    }

    suspend fun deleteStudyMaterial(id: String) = withContext(Dispatchers.IO) {
        studyMaterialDao.deleteMaterialById(id)
    }

    suspend fun updateMaterialLastOpened(id: String) = withContext(Dispatchers.IO) {
        studyMaterialDao.updateLastOpened(id, System.currentTimeMillis())
    }

    fun countMaterialsFlow(): Flow<Int> = studyMaterialDao.countMaterialsFlow()

    // -------------------------------------------------------------
    // PART B & C: Material Study Tracking & Guardian Sessions
    // -------------------------------------------------------------
    suspend fun recordMaterialStudySession(
        materialId: String,
        subjectId: String,
        chapterId: String,
        topicId: String? = null,
        startTime: Long,
        endTime: Long,
        activeDurationSeconds: Long
    ): MaterialStudySession = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateString = dateFormat.format(Date(endTime))

        val session = MaterialStudySession(
            id = UUID.randomUUID().toString(),
            materialId = materialId,
            subjectId = subjectId,
            chapterId = chapterId,
            topicId = topicId,
            startTime = startTime,
            endTime = endTime,
            activeDurationSeconds = activeDurationSeconds,
            dateString = dateString
        )
        materialStudySessionDao.insertSession(session)
        session
    }

    suspend fun clearMaterialStudySessions() = withContext(Dispatchers.IO) {
        materialStudySessionDao.clearAll()
    }

    // -------------------------------------------------------------
    // PART D: Adaptive Planner Engine
    // -------------------------------------------------------------
    fun getPlannedTasksWithDetailsFlow(dateString: String): Flow<List<PlannerTaskWithDetails>> {
        return combine(
            plannerTaskDao.getTasksForDateFlow(dateString),
            subjectDao.getAllSubjectsFlow(),
            chapterDao.getAllChaptersFlow(),
            topicDao.getAllTopicsFlow()
        ) { tasks, subjects, chapters, topics ->
            val subMap = subjects.associateBy { it.id }
            val chMap = chapters.associateBy { it.id }
            val topMap = topics.associateBy { it.id }

            tasks.mapNotNull { task ->
                val top = topMap[task.topicId] ?: return@mapNotNull null
                val sub = subMap[task.subjectId]
                val ch = chMap[task.chapterId]
                PlannerTaskWithDetails(
                    task = task,
                    subjectName = sub?.name ?: "Subject",
                    subjectColorHex = sub?.colorHex ?: "#C4975A",
                    chapterName = ch?.name ?: "Chapter",
                    topicName = top.name,
                    topicEstimatedMinutes = top.estimatedMinutes,
                    topicState = top.state
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun generateAdaptivePlan(
        dateString: String,
        activeChapterId: String? = null
    ): List<PlannerTaskWithDetails> = withContext(Dispatchers.IO) {
        val profile = userProfileDao.getProfile()
        val dailyLimit = profile?.dailyStudyLimitMinutes ?: 120

        // Calculate actual study time today
        val regularTodaySeconds = studySessionDao.getTodayStudySeconds(dateString) ?: 0L
        val materialTodaySeconds = materialStudySessionDao.getTodayMaterialStudySeconds(dateString) ?: 0L
        val totalTodaySeconds = regularTodaySeconds + materialTodaySeconds
        val studiedTodayMinutes = (totalTodaySeconds / 60).toInt()
        val remainingDailyMinutes = maxOf(0, dailyLimit - studiedTodayMinutes)

        val allTopics = topicDao.getAllTopics()
        if (allTopics.isEmpty()) return@withContext emptyList()

        val allSubjects = subjectDao.getAllSubjects().associateBy { it.id }
        val allChapters = chapterDao.getAllChapters().associateBy { it.id }
        val allAttempts = testDao.getAllAttempts().groupBy { it.topicId }
        val allSessions = studySessionDao.getAllSessions().groupBy { it.topicId }

        // Find most active chapter if none provided
        val targetChapterId = activeChapterId ?: allSessions.values.flatten()
            .maxByOrNull { it.endTime }?.chapterId

        data class ScoredTopic(
            val topic: Topic,
            val priorityScore: Int, // 1 is highest priority
            val reason: String
        )

        val candidateList = mutableListOf<ScoredTopic>()

        for (topic in allTopics) {
            val attempts = allAttempts[topic.id] ?: emptyList()
            val latestAttempt = attempts.maxByOrNull { it.timestamp }
            val failedCount = attempts.count { !it.isPassed }
            val sessions = allSessions[topic.id] ?: emptyList()
            val isInActiveChapter = targetChapterId != null && topic.chapterId == targetChapterId
            val chName = allChapters[topic.chapterId]?.name ?: "Chapter"

            when {
                // Priority 1: Failed mastery test or marked for review
                topic.state == TopicState.NEEDS_REVIEW || (latestAttempt != null && !latestAttempt.isPassed) -> {
                    val scoreText = latestAttempt?.let { "${it.percentage}%" } ?: "<70%"
                    val reason = if (failedCount > 1) {
                        "Weak test performance: $scoreText ($failedCount failed attempts). Review urgently."
                    } else {
                        "Needs review: Scored $scoreText on recent 5-Q test (Target >= 70%)."
                    }
                    candidateList.add(ScoredTopic(topic, 1, reason))
                }
                // Priority 2: Evidence required
                topic.state == TopicState.EVIDENCE_REQUIRED -> {
                    candidateList.add(
                        ScoredTopic(
                            topic,
                            2,
                            "Focus session completed. Upload handwritten notes photo to unlock test."
                        )
                    )
                }
                // Priority 3: Test available
                topic.state == TopicState.TEST_AVAILABLE -> {
                    candidateList.add(
                        ScoredTopic(
                            topic,
                            2,
                            "Study and evidence completed. Ready for 5-question mastery assessment."
                        )
                    )
                }
                // Priority 4: In Progress in current chapter
                topic.state == TopicState.STUDYING && isInActiveChapter -> {
                    candidateList.add(
                        ScoredTopic(
                            topic,
                            3,
                            "In progress in current chapter: $chName."
                        )
                    )
                }
                // Priority 5: Next unstarted in current chapter
                topic.state == TopicState.NOT_STARTED && isInActiveChapter -> {
                    candidateList.add(
                        ScoredTopic(
                            topic,
                            4,
                            "Next sequential topic in current chapter: $chName."
                        )
                    )
                }
                // Priority 6: Other in-progress topics
                topic.state == TopicState.STUDYING -> {
                    candidateList.add(
                        ScoredTopic(
                            topic,
                            5,
                            "Study in progress in $chName."
                        )
                    )
                }
                // Priority 7: Not started topics
                topic.state == TopicState.NOT_STARTED -> {
                    val subName = allSubjects[allChapters[topic.chapterId]?.subjectId]?.name ?: "Curriculum"
                    candidateList.add(
                        ScoredTopic(
                            topic,
                            6,
                            "Foundational topic ready to start in $subName."
                        )
                    )
                }
                // Priority 8: Passed topics with lower scores (<85%) for retention
                (topic.state == TopicState.PASSED || topic.state == TopicState.COMPLETED) &&
                        latestAttempt != null && latestAttempt.percentage < 85 -> {
                    val daysAgo = (System.currentTimeMillis() - latestAttempt.timestamp) / (1000 * 60 * 60 * 24)
                    candidateList.add(
                        ScoredTopic(
                            topic,
                            7,
                            "Periodic spaced review (Scored ${latestAttempt.percentage}%, $daysAgo days ago)."
                        )
                    )
                }
            }
        }

        // Sort candidates: Lowest priority score first, then chapter order, then topic order
        val sortedCandidates = candidateList.sortedWith(
            compareBy<ScoredTopic> { it.priorityScore }
                .thenBy { allChapters[it.topic.chapterId]?.orderIndex ?: 99 }
                .thenBy { it.topic.orderIndex }
        )

        // Allocate tasks respecting daily study limit
        val allocatedTasks = mutableListOf<PlannerTask>()
        var allocatedMinutes = 0

        // If user has already reached or exceeded daily limit, still recommend top 1-2 priorities
        val availableTime = if (remainingDailyMinutes <= 0) 45 else remainingDailyMinutes

        for (item in sortedCandidates) {
            val duration = item.topic.estimatedMinutes.coerceIn(15, 60)
            if (allocatedMinutes + duration <= availableTime || allocatedTasks.isEmpty()) {
                val task = PlannerTask(
                    id = UUID.randomUUID().toString(),
                    subjectId = allChapters[item.topic.chapterId]?.subjectId ?: "",
                    chapterId = item.topic.chapterId,
                    topicId = item.topic.id,
                    plannedDate = dateString,
                    plannedDurationMinutes = duration,
                    priority = item.priorityScore,
                    reason = item.reason,
                    status = PlannerTaskStatus.PLANNED,
                    createdAt = System.currentTimeMillis()
                )
                allocatedTasks.add(task)
                allocatedMinutes += duration
            }

            // Cap at 4-5 tasks max per day to avoid overwhelming
            if (allocatedTasks.size >= 5) break
        }

        // Persist to Room: remove old PLANNED tasks for this date and insert newly scheduled tasks
        plannerTaskDao.deleteTasksForDate(dateString)
        plannerTaskDao.insertTasks(allocatedTasks)

        // Return with details
        allocatedTasks.mapNotNull { task ->
            val top = allTopics.find { it.id == task.topicId } ?: return@mapNotNull null
            val ch = allChapters[task.chapterId]
            val sub = allSubjects[task.subjectId]
            PlannerTaskWithDetails(
                task = task,
                subjectName = sub?.name ?: "Subject",
                subjectColorHex = sub?.colorHex ?: "#C4975A",
                chapterName = ch?.name ?: "Chapter",
                topicName = top.name,
                topicEstimatedMinutes = top.estimatedMinutes,
                topicState = top.state
            )
        }
    }

    suspend fun updatePlannerTaskStatus(taskId: String, status: PlannerTaskStatus) = withContext(Dispatchers.IO) {
        plannerTaskDao.updateTaskStatus(taskId, status)
    }

    suspend fun deletePlannerTask(taskId: String) = withContext(Dispatchers.IO) {
        plannerTaskDao.deleteTaskById(taskId)
    }

    suspend fun clearPlannerTasks(dateString: String) = withContext(Dispatchers.IO) {
        plannerTaskDao.deleteTasksForDate(dateString)
    }
}
