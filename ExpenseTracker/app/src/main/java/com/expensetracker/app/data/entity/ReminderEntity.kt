package com.expensetracker.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val title: String,
    val note: String,
    val triggerAtEpochMillis: Long,
    val recurrence: String,
    val customIntervalDays: Int = 1,
    val enabled: Boolean = true,
    val createdAtEpochMillis: Long,
)
