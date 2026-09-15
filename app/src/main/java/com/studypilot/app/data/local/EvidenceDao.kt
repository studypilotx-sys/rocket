package com.studypilot.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.studypilot.app.data.model.Evidence
import kotlinx.coroutines.flow.Flow

@Dao
interface EvidenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(evidence: Evidence)

    @Query("SELECT * FROM evidence_records WHERE topicId = :topicId ORDER BY timestamp DESC")
    fun getEvidenceForTopicFlow(topicId: String): Flow<List<Evidence>>

    @Query("SELECT * FROM evidence_records WHERE topicId = :topicId ORDER BY timestamp DESC")
    suspend fun getEvidenceForTopic(topicId: String): List<Evidence>

    @Query("SELECT * FROM evidence_records ORDER BY timestamp DESC")
    fun getAllEvidenceFlow(): Flow<List<Evidence>>

    @Query("SELECT COUNT(*) FROM evidence_records WHERE topicId = :topicId")
    suspend fun countEvidenceForTopic(topicId: String): Int
}
