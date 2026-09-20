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
    @Insert suspend fun insert(expense: ExpenseEntity): Long
    @Update suspend fun update(expense: ExpenseEntity)
    @Delete suspend fun delete(expense: ExpenseEntity)
    @Query("SELECT * FROM expenses WHERE id = :id AND profileId = :profileId") suspend fun getById(id: Long, profileId: Long): ExpenseEntity?
    @Query("SELECT * FROM expenses WHERE profileId = :profileId ORDER BY epochDay DESC, createdAtEpochMillis DESC") fun observeAll(profileId: Long): Flow<List<ExpenseEntity>>
    @Query("SELECT * FROM expenses WHERE profileId = :profileId AND epochDay BETWEEN :startEpochDay AND :endEpochDay ORDER BY epochDay DESC, createdAtEpochMillis DESC") fun observeBetween(profileId: Long, startEpochDay: Long, endEpochDay: Long): Flow<List<ExpenseEntity>>
    @Query("SELECT * FROM expenses WHERE profileId = :profileId AND budgetMonth = :budgetMonth ORDER BY epochDay DESC, createdAtEpochMillis DESC") fun observeBudgetPeriod(profileId: Long, budgetMonth: String): Flow<List<ExpenseEntity>>
    @Query("SELECT category, SUM(amountMinor) AS totalMinor FROM expenses WHERE profileId = :profileId AND isIncome = 0 AND budgetMonth = :budgetMonth GROUP BY category") fun observeCategoryTotalsForBudget(profileId: Long, budgetMonth: String): Flow<List<CategoryTotal>>
    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM expenses WHERE profileId = :profileId AND isIncome = 0 AND budgetMonth = :budgetMonth AND category = :category") suspend fun getCategoryTotalForBudget(profileId: Long, category: ExpenseCategory, budgetMonth: String): Long
    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM expenses WHERE profileId = :profileId AND isIncome = 0 AND budgetMonth = :budgetMonth") suspend fun getOverallTotalForBudget(profileId: Long, budgetMonth: String): Long
    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM expenses WHERE profileId = :profileId AND isIncome = 1 AND budgetMonth = :budgetMonth") fun observeIncomeTotalForBudget(profileId: Long, budgetMonth: String): Flow<Long>
    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM expenses WHERE profileId = :profileId AND isIncome = 1 AND budgetMonth = :budgetMonth") suspend fun getIncomeTotalForBudget(profileId: Long, budgetMonth: String): Long

    /** Used only to link legacy fuel logs created before fuel/expense IDs were linked. */
    @Query("SELECT * FROM expenses WHERE profileId = :profileId AND amountMinor = :amountMinor AND category = :category AND note = :note AND isIncome = 0 AND epochDay = :epochDay ORDER BY createdAtEpochMillis DESC, id DESC LIMIT 1")
    suspend fun findMatchingFuelExpense(profileId: Long, amountMinor: Long, category: ExpenseCategory, note: String, epochDay: Long): ExpenseEntity?

    @Query("UPDATE expenses SET budgetMonth = :budgetMonth, budgetCycleStartEpochMillis = :cycleStartEpochMillis WHERE profileId = :profileId AND createdAtEpochMillis >= :fromEpochMillis")
    suspend fun assignExpensesToBudgetFrom(profileId: Long, fromEpochMillis: Long, budgetMonth: String, cycleStartEpochMillis: Long)
}
