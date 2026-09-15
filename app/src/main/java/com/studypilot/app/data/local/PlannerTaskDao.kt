package com.studypilot.app.data.local

import androidx.room.*
import com.studypilot.app.data.model.PlannerTask
import com.studypilot.app.data.model.PlannerTaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannerTaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: PlannerTask)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<PlannerTask>)

    @Update
    suspend fun updateTask(task: PlannerTask)

    @Query("UPDATE planner_tasks SET status = :status WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: String, status: PlannerTaskStatus)

    @Query("SELECT * FROM planner_tasks WHERE plannedDate = :dateString ORDER BY priority ASC, createdAt ASC")
    fun getTasksForDateFlow(dateString: String): Flow<List<PlannerTask>>

    @Query("SELECT * FROM planner_tasks WHERE plannedDate = :dateString ORDER BY priority ASC, createdAt ASC")
    suspend fun getTasksForDate(dateString: String): List<PlannerTask>

    @Query("SELECT * FROM planner_tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): PlannerTask?

    @Query("DELETE FROM planner_tasks WHERE plannedDate = :dateString")
    suspend fun deleteTasksForDate(dateString: String)

    @Query("DELETE FROM planner_tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Query("DELETE FROM planner_tasks")
    suspend fun clearAll()
}
