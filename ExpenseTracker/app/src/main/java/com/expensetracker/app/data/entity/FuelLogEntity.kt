package com.expensetracker.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "fuel_logs",
    foreignKeys = [ForeignKey(entity = ProfileEntity::class, parentColumns = ["id"], childColumns = ["profileId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("profileId"), Index("profileId", "epochDay"), Index("expenseId")],
)
data class FuelLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val amountMinor: Long,
    val liters: Double,
    val odometerKm: Double,
    val epochDay: Long,
    val endEpochDay: Long? = null,
    val endOdometerKm: Double? = null,
    val note: String = "",
    /** The home-screen expense created for this fuel fill. */
    val expenseId: Long? = null,
)
