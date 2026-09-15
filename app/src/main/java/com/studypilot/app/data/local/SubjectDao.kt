package com.studypilot.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studypilot.app.data.model.Subject
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY orderIndex ASC, createdAt ASC")
    fun getAllSubjectsFlow(): Flow<List<Subject>>

    @Query("SELECT * FROM subjects ORDER BY orderIndex ASC, createdAt ASC")
    suspend fun getAllSubjects(): List<Subject>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectById(id: String): Subject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subject: Subject)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(subjects: List<Subject>)

    @Update
    suspend fun update(subject: Subject)

    @Delete
    suspend fun delete(subject: Subject)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM subjects")
    suspend fun clearAll()
}
