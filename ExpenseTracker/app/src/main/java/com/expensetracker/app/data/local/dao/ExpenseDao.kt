package com.expensetracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.expensetracker.app.data.entity.ExpenseEntity
import com.expensetracker.app.data.model.ExpenseCategory
import kotlinx.coroutines.flow.Flow

data class CategoryTotal(val category: ExpenseCategory, val totalMinor: Long)

@Dao
interface ExpenseDao {

    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses ORDER BY epochDay DESC, createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query(
        "SELECT * FROM expenses WHERE epochDay BETWEEN :startEpochDay AND :endEpochDay " +
            "ORDER BY epochDay DESC, createdAtEpochMillis DESC",
    )
    fun observeBetween(startEpochDay: Long, endEpochDay: Long): Flow<List<ExpenseEntity>>

    @Query(
        "SELECT category, SUM(amountMinor) AS totalMinor FROM expenses " +
            "WHERE epochDay BETWEEN :startEpochDay AND :endEpochDay GROUP BY category",
    )
    fun observeCategoryTotals(startEpochDay: Long, endEpochDay: Long): Flow<List<CategoryTotal>>

    @Query(
        "SELECT COALESCE(SUM(amountMinor), 0) FROM expenses " +
            "WHERE category = :category AND epochDay BETWEEN :startEpochDay AND :endEpochDay",
    )
    suspend fun getCategoryTotal(category: ExpenseCategory, startEpochDay: Long, endEpochDay: Long): Long

    @Query(
        "SELECT COALESCE(SUM(amountMinor), 0) FROM expenses " +
            "WHERE epochDay BETWEEN :startEpochDay AND :endEpochDay",
    )
    suspend fun getOverallTotal(startEpochDay: Long, endEpochDay: Long): Long
}
