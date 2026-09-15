package com.studypilot.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studypilot.app.data.model.Topic
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {
    @Query("SELECT * FROM topics WHERE chapterId = :chapterId ORDER BY orderIndex ASC, createdAt ASC")
    fun getTopicsForChapterFlow(chapterId: String): Flow<List<Topic>>

    @Query("SELECT * FROM topics WHERE chapterId = :chapterId ORDER BY orderIndex ASC, createdAt ASC")
    suspend fun getTopicsForChapter(chapterId: String): List<Topic>

    @Query("SELECT * FROM topics ORDER BY orderIndex ASC")
    suspend fun getAllTopics(): List<Topic>

    @Query("SELECT * FROM topics WHERE id = :id LIMIT 1")
    suspend fun getTopicById(id: String): Topic?

    @Query("SELECT COUNT(*) FROM topics WHERE chapterId = :chapterId")
    suspend fun countTopicsForChapter(chapterId: String): Int

    @Query("SELECT COUNT(*) FROM topics INNER JOIN chapters ON topics.chapterId = chapters.id WHERE chapters.subjectId = :subjectId")
    suspend fun countTopicsForSubject(subjectId: String): Int

    @Query("UPDATE topics SET state = :state WHERE id = :topicId")
    suspend fun updateTopicState(topicId: String, state: com.studypilot.app.data.model.TopicState)

    @Query("SELECT COUNT(*) FROM topics WHERE state IN ('PASSED', 'COMPLETED')")
    suspend fun countCompletedTopics(): Int

    @Query("SELECT COUNT(*) FROM topics WHERE chapterId = :chapterId AND state IN ('PASSED', 'COMPLETED')")
    suspend fun countCompletedTopicsForChapter(chapterId: String): Int

    @Query("SELECT COUNT(*) FROM topics WHERE chapterId = :chapterId AND state NOT IN ('NOT_STARTED', 'PASSED', 'COMPLETED')")
    suspend fun countInProgressTopicsForChapter(chapterId: String): Int

    @Query("SELECT COUNT(*) FROM topics INNER JOIN chapters ON topics.chapterId = chapters.id WHERE chapters.subjectId = :subjectId AND topics.state IN ('PASSED', 'COMPLETED')")
    suspend fun countCompletedTopicsForSubject(subjectId: String): Int

    @Query("SELECT * FROM topics WHERE state IN ('PASSED', 'COMPLETED')")
    suspend fun getCompletedTopics(): List<Topic>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(topic: Topic)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(topics: List<Topic>)

    @Update
    suspend fun update(topic: Topic)

    @Delete
    suspend fun delete(topic: Topic)

    @Query("DELETE FROM topics WHERE id = :id")
    suspend fun deleteById(id: String)
}
