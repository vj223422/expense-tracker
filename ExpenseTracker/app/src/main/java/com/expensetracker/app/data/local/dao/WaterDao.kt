package com.expensetracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import androidx.room.Delete
import com.expensetracker.app.data.entity.WaterIntakeEntity
import com.expensetracker.app.data.entity.WaterSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterDao {
    @Query("SELECT * FROM water_settings WHERE profileId = :profileId LIMIT 1")
    fun observeSettings(profileId: Long): Flow<WaterSettingsEntity?>
    @Query("SELECT * FROM water_settings WHERE profileId = :profileId LIMIT 1")
    suspend fun getSettings(profileId: Long): WaterSettingsEntity?
    @Query("SELECT * FROM water_settings WHERE enabled = 1")
    suspend fun getSettingsForScheduling(): List<WaterSettingsEntity>
    @Upsert
    suspend fun upsertSettings(settings: WaterSettingsEntity)
    @Insert
    suspend fun insertIntake(intake: WaterIntakeEntity): Long
    @Query("SELECT * FROM water_intake WHERE profileId = :profileId ORDER BY drankAtEpochMillis DESC")
    fun observeIntake(profileId: Long): Flow<List<WaterIntakeEntity>>

    @Delete
    suspend fun deleteIntake(intake: WaterIntakeEntity)
}
