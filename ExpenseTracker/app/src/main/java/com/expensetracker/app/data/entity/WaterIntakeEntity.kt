package com.expensetracker.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "water_intake", indices = [Index(value = ["profileId", "drankAtEpochMillis"])])
data class WaterIntakeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val amountMl: Int,
    val drankAtEpochMillis: Long,
    val source: String = "REMINDER",
)
