package com.studypilot.app.data.local

import androidx.room.*
import com.studypilot.app.data.model.StudyMaterial
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyMaterialDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterial(material: StudyMaterial)

    @Update
    suspend fun updateMaterial(material: StudyMaterial)

    @Delete
    suspend fun deleteMaterial(material: StudyMaterial)

    @Query("DELETE FROM study_materials WHERE id = :id")
    suspend fun deleteMaterialById(id: String)

    @Query("SELECT * FROM study_materials WHERE id = :id")
    suspend fun getMaterialById(id: String): StudyMaterial?

    @Query("SELECT * FROM study_materials ORDER BY dateAdded DESC")
    fun getAllMaterialsFlow(): Flow<List<StudyMaterial>>

    @Query("SELECT * FROM study_materials ORDER BY dateAdded DESC")
    suspend fun getAllMaterials(): List<StudyMaterial>

    @Query("SELECT * FROM study_materials WHERE subjectId = :subjectId ORDER BY dateAdded DESC")
    fun getMaterialsForSubjectFlow(subjectId: String): Flow<List<StudyMaterial>>

    @Query("SELECT * FROM study_materials WHERE chapterId = :chapterId ORDER BY dateAdded DESC")
    fun getMaterialsForChapterFlow(chapterId: String): Flow<List<StudyMaterial>>

    @Query("SELECT * FROM study_materials WHERE topicId = :topicId ORDER BY dateAdded DESC")
    fun getMaterialsForTopicFlow(topicId: String): Flow<List<StudyMaterial>>

    @Query("SELECT COUNT(*) FROM study_materials")
    suspend fun countMaterials(): Int

    @Query("SELECT COUNT(*) FROM study_materials")
    fun countMaterialsFlow(): Flow<Int>

    @Query("UPDATE study_materials SET lastOpened = :timestamp WHERE id = :id")
    suspend fun updateLastOpened(id: String, timestamp: Long)

    @Query("DELETE FROM study_materials")
    suspend fun clearAll()
}
