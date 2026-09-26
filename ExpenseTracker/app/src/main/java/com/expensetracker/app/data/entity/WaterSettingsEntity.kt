package com.expensetracker.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "water_settings")
data class WaterSettingsEntity(
    @PrimaryKey val profileId: Long,
    val enabled: Boolean = false,
    val dailyGoalMl: Int = 2000,
    val intervalHours: Int = 2,
    val intakePerReminderMl: Int = 300,
    val nextReminderAtEpochMillis: Long? = null,
    val startTimeMinutes: Int = 8 * 60,
    val endTimeMinutes: Int = 20 * 60,
    val reminderCount: Int = 8,
)
