package com.studypilot.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studypilot.app.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE userId = 'default_student' LIMIT 1")
    fun getProfileFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE userId = 'default_student' LIMIT 1")
    suspend fun getProfile(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: UserProfile)

    @Update
    suspend fun update(profile: UserProfile)

    @Query("DELETE FROM user_profile")
    suspend fun clearProfile()
}
