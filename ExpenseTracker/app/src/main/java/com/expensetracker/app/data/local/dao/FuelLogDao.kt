package com.expensetracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.expensetracker.app.data.entity.FuelLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelLogDao {
    @Insert suspend fun insert(log: FuelLogEntity): Long
    @Delete suspend fun delete(log: FuelLogEntity)

    @Query("DELETE FROM fuel_logs WHERE expenseId = :expenseId")
    suspend fun deleteByExpenseId(expenseId: Long)

    @Query("SELECT * FROM fuel_logs WHERE profileId = :profileId AND endEpochDay IS NULL ORDER BY epochDay DESC, id DESC LIMIT 1")
    suspend fun getOpenCycle(profileId: Long): FuelLogEntity?

    @Query("UPDATE fuel_logs SET endEpochDay = :endEpochDay, endOdometerKm = :endOdometerKm WHERE id = :id")
    suspend fun closeCycle(id: Long, endEpochDay: Long, endOdometerKm: Double)

    @Query("SELECT * FROM fuel_logs WHERE profileId = :profileId ORDER BY epochDay DESC, id DESC")
    fun observeAll(profileId: Long): Flow<List<FuelLogEntity>>
}
