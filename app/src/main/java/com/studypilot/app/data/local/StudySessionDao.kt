package com.studypilot.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.studypilot.app.data.model.StudySession
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: StudySession)

    @Query("SELECT * FROM study_sessions WHERE topicId = :topicId ORDER BY startTime DESC")
    fun getSessionsForTopicFlow(topicId: String): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions WHERE topicId = :topicId ORDER BY startTime DESC")
    suspend fun getSessionsForTopic(topicId: String): List<StudySession>

    @Query("SELECT * FROM study_sessions WHERE chapterId = :chapterId")
    suspend fun getSessionsForChapter(chapterId: String): List<StudySession>

    @Query("SELECT * FROM study_sessions WHERE subjectId = :subjectId")
    suspend fun getSessionsForSubject(subjectId: String): List<StudySession>

    @Query("SELECT * FROM study_sessions WHERE dateString = :dateString")
    suspend fun getSessionsForDate(dateString: String): List<StudySession>

    @Query("SELECT * FROM study_sessions ORDER BY startTime DESC")
    fun getAllSessionsFlow(): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions ORDER BY startTime DESC")
    suspend fun getAllSessions(): List<StudySession>

    @Query("SELECT SUM(durationSeconds) FROM study_sessions")
    suspend fun getTotalStudySeconds(): Long?

    @Query("SELECT SUM(durationSeconds) FROM study_sessions WHERE dateString = :dateString")
    suspend fun getTodayStudySeconds(dateString: String): Long?

    @Query("SELECT SUM(durationSeconds) FROM study_sessions WHERE chapterId = :chapterId")
    suspend fun getChapterStudySeconds(chapterId: String): Long?

    @Query("SELECT SUM(durationSeconds) FROM study_sessions WHERE subjectId = :subjectId")
    suspend fun getSubjectStudySeconds(subjectId: String): Long?
}
