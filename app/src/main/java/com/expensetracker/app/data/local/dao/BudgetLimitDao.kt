package com.expensetracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.expensetracker.app.data.entity.BudgetLimitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetLimitDao {

    @Query("SELECT * FROM budget_limits WHERE profileId = :profileId")
    fun observeAll(profileId: Long): Flow<List<BudgetLimitEntity>>

    @Query("SELECT * FROM budget_limits WHERE profileId = :profileId AND categoryKey = :categoryKey")
    suspend fun getByKey(profileId: Long, categoryKey: String): BudgetLimitEntity?

    @Upsert
    suspend fun upsert(limit: BudgetLimitEntity)

    @Query("DELETE FROM budget_limits WHERE profileId = :profileId AND categoryKey = :categoryKey")
    suspend fun delete(profileId: Long, categoryKey: String)
}
