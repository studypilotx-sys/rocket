package com.studypilot.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.studypilot.app.data.model.Chapter
import com.studypilot.app.data.model.Evidence
import com.studypilot.app.data.model.MaterialStudySession
import com.studypilot.app.data.model.PlannerTask
import com.studypilot.app.data.model.StudyMaterial
import com.studypilot.app.data.model.StudySession
import com.studypilot.app.data.model.Subject
import com.studypilot.app.data.model.TestAttempt
import com.studypilot.app.data.model.TestQuestion
import com.studypilot.app.data.model.Topic
import com.studypilot.app.data.model.UserProfile

@Database(
    entities = [
        UserProfile::class,
        Subject::class,
        Chapter::class,
        Topic::class,
        StudySession::class,
        Evidence::class,
        TestQuestion::class,
        TestAttempt::class,
        StudyMaterial::class,
        MaterialStudySession::class,
        PlannerTask::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class StudyPilotDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun subjectDao(): SubjectDao
    abstract fun chapterDao(): ChapterDao
    abstract fun topicDao(): TopicDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun evidenceDao(): EvidenceDao
    abstract fun testDao(): TestDao
    abstract fun studyMaterialDao(): StudyMaterialDao
    abstract fun materialStudySessionDao(): MaterialStudySessionDao
    abstract fun plannerTaskDao(): PlannerTaskDao

    companion object {
        @Volatile
        private var INSTANCE: StudyPilotDatabase? = null

        fun getDatabase(context: Context): StudyPilotDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudyPilotDatabase::class.java,
                    "studypilot_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
