package com.studypilot.app.data.local

import androidx.room.TypeConverter
import com.studypilot.app.data.model.MaterialType
import com.studypilot.app.data.model.PlannerTaskStatus
import com.studypilot.app.data.model.TopicState

class Converters {
    @TypeConverter
    fun fromTopicState(value: TopicState): String = value.name

    @TypeConverter
    fun toTopicState(value: String): TopicState {
        return try {
            TopicState.valueOf(value)
        } catch (e: Exception) {
            TopicState.NOT_STARTED
        }
    }

    @TypeConverter
    fun fromMaterialType(value: MaterialType): String = value.name

    @TypeConverter
    fun toMaterialType(value: String): MaterialType {
        return try {
            MaterialType.valueOf(value)
        } catch (e: Exception) {
            MaterialType.DOCUMENT
        }
    }

    @TypeConverter
    fun fromPlannerTaskStatus(value: PlannerTaskStatus): String = value.name

    @TypeConverter
    fun toPlannerTaskStatus(value: String): PlannerTaskStatus {
        return try {
            PlannerTaskStatus.valueOf(value)
        } catch (e: Exception) {
            PlannerTaskStatus.PLANNED
        }
    }
}
