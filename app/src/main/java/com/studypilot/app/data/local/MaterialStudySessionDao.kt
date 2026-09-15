package com.studypilot.app.data.local

import androidx.room.*
import com.studypilot.app.data.model.MaterialStudySession
import kotlinx.coroutines.flow.Flow

@Dao
interface MaterialStudySessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: MaterialStudySession)

    @Query("SELECT * FROM material_study_sessions WHERE materialId = :materialId ORDER BY startTime DESC")
    fun getSessionsForMaterial(materialId: String): Flow<List<MaterialStudySession>>

    @Query("SELECT * FROM material_study_sessions ORDER BY startTime DESC")
    fun getAllSessionsFlow(): Flow<List<MaterialStudySession>>

    @Query("SELECT SUM(activeDurationSeconds) FROM material_study_sessions")
    fun getTotalMaterialStudySecondsFlow(): Flow<Long?>

    @Query("SELECT SUM(activeDurationSeconds) FROM material_study_sessions")
    suspend fun getTotalMaterialStudySeconds(): Long?

    @Query("SELECT SUM(activeDurationSeconds) FROM material_study_sessions WHERE dateString = :dateString")
    fun getTodayMaterialStudySecondsFlow(dateString: String): Flow<Long?>

    @Query("SELECT SUM(activeDurationSeconds) FROM material_study_sessions WHERE dateString = :dateString")
    suspend fun getTodayMaterialStudySeconds(dateString: String): Long?

    @Query("SELECT SUM(activeDurationSeconds) FROM material_study_sessions WHERE subjectId = :subjectId")
    suspend fun getSubjectMaterialStudySeconds(subjectId: String): Long?

    @Query("SELECT SUM(activeDurationSeconds) FROM material_study_sessions WHERE chapterId = :chapterId")
    suspend fun getChapterMaterialStudySeconds(chapterId: String): Long?

    @Query("DELETE FROM material_study_sessions")
    suspend fun clearAll()
}
