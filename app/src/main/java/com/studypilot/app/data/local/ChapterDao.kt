package com.studypilot.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studypilot.app.data.model.Chapter
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId ORDER BY orderIndex ASC, createdAt ASC")
    fun getChaptersForSubjectFlow(subjectId: String): Flow<List<Chapter>>

    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId ORDER BY orderIndex ASC, createdAt ASC")
    suspend fun getChaptersForSubject(subjectId: String): List<Chapter>

    @Query("SELECT * FROM chapters ORDER BY orderIndex ASC")
    suspend fun getAllChapters(): List<Chapter>

    @Query("SELECT * FROM chapters WHERE id = :id LIMIT 1")
    suspend fun getChapterById(id: String): Chapter?

    @Query("SELECT COUNT(*) FROM chapters WHERE subjectId = :subjectId")
    suspend fun countChaptersForSubject(subjectId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chapter: Chapter)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chapters: List<Chapter>)

    @Update
    suspend fun update(chapter: Chapter)

    @Delete
    suspend fun delete(chapter: Chapter)

    @Query("DELETE FROM chapters WHERE id = :id")
    suspend fun deleteById(id: String)
}
