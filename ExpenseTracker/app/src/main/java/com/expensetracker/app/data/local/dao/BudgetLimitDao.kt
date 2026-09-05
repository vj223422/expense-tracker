package com.expensetracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.expensetracker.app.data.entity.BudgetLimitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetLimitDao {

    @Query("SELECT * FROM budget_limits")
    fun observeAll(): Flow<List<BudgetLimitEntity>>

    @Query("SELECT * FROM budget_limits WHERE categoryKey = :categoryKey")
    suspend fun getByKey(categoryKey: String): BudgetLimitEntity?

    @Upsert
    suspend fun upsert(limit: BudgetLimitEntity)

    @Query("DELETE FROM budget_limits WHERE categoryKey = :categoryKey")
    suspend fun delete(categoryKey: String)
}
