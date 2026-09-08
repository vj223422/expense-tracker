package com.expensetracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.expensetracker.app.data.entity.ExpenseEntity
import com.expensetracker.app.data.model.ExpenseCategory
import kotlinx.coroutines.flow.Flow

data class CategoryTotal(val category: ExpenseCategory, val totalMinor: Long)

@Dao
interface ExpenseDao {

    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE id = :id AND profileId = :profileId")
    suspend fun getById(id: Long, profileId: Long): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE profileId = :profileId ORDER BY epochDay DESC, createdAtEpochMillis DESC")
    fun observeAll(profileId: Long): Flow<List<ExpenseEntity>>

    @Query(
        "SELECT * FROM expenses WHERE profileId = :profileId AND epochDay BETWEEN :startEpochDay AND :endEpochDay " +
            "ORDER BY epochDay DESC, createdAtEpochMillis DESC",
    )
    fun observeBetween(profileId: Long, startEpochDay: Long, endEpochDay: Long): Flow<List<ExpenseEntity>>

    @Query(
        "SELECT category, SUM(amountMinor) AS totalMinor FROM expenses " +
            "WHERE profileId = :profileId AND epochDay BETWEEN :startEpochDay AND :endEpochDay GROUP BY category",
    )
    fun observeCategoryTotals(profileId: Long, startEpochDay: Long, endEpochDay: Long): Flow<List<CategoryTotal>>

    @Query(
        "SELECT COALESCE(SUM(amountMinor), 0) FROM expenses " +
            "WHERE profileId = :profileId AND category = :category AND epochDay BETWEEN :startEpochDay AND :endEpochDay",
    )
    suspend fun getCategoryTotal(profileId: Long, category: ExpenseCategory, startEpochDay: Long, endEpochDay: Long): Long

    @Query(
        "SELECT COALESCE(SUM(amountMinor), 0) FROM expenses " +
            "WHERE profileId = :profileId AND epochDay BETWEEN :startEpochDay AND :endEpochDay",
    )
    suspend fun getOverallTotal(profileId: Long, startEpochDay: Long, endEpochDay: Long): Long
}
