package com.studypilot.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.studypilot.app.data.model.TestAttempt
import com.studypilot.app.data.model.TestQuestion
import kotlinx.coroutines.flow.Flow

@Dao
interface TestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<TestQuestion>)

    @Query("SELECT * FROM test_questions WHERE topicId = :topicId ORDER BY questionNumber ASC")
    suspend fun getQuestionsForTopic(topicId: String): List<TestQuestion>

    @Query("SELECT COUNT(*) FROM test_questions WHERE topicId = :topicId")
    suspend fun countQuestionsForTopic(topicId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: TestAttempt)

    @Query("SELECT * FROM test_attempts WHERE topicId = :topicId ORDER BY timestamp DESC")
    fun getAttemptsForTopicFlow(topicId: String): Flow<List<TestAttempt>>

    @Query("SELECT * FROM test_attempts WHERE topicId = :topicId ORDER BY timestamp DESC")
    suspend fun getAttemptsForTopic(topicId: String): List<TestAttempt>

    @Query("SELECT * FROM test_attempts ORDER BY timestamp DESC")
    fun getAllAttemptsFlow(): Flow<List<TestAttempt>>

    @Query("SELECT * FROM test_attempts ORDER BY timestamp DESC")
    suspend fun getAllAttempts(): List<TestAttempt>

    @Query("SELECT * FROM test_attempts INNER JOIN topics ON test_attempts.topicId = topics.id WHERE topics.chapterId = :chapterId")
    suspend fun getAttemptsForChapter(chapterId: String): List<TestAttempt>

    @Query("SELECT AVG(percentage) FROM test_attempts INNER JOIN topics ON test_attempts.topicId = topics.id WHERE topics.chapterId = :chapterId")
    suspend fun getAveragePercentageForChapter(chapterId: String): Double?

    @Query("SELECT AVG(percentage) FROM test_attempts")
    suspend fun getOverallAveragePercentage(): Double?

    @Query("SELECT COUNT(*) FROM test_attempts WHERE isPassed = 1")
    suspend fun countPassedTests(): Int
}
