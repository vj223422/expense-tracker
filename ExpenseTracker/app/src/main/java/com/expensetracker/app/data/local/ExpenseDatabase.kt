package com.expensetracker.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.expensetracker.app.data.entity.BudgetLimitEntity
import com.expensetracker.app.data.entity.ExpenseEntity
import com.expensetracker.app.data.local.dao.BudgetLimitDao
import com.expensetracker.app.data.local.dao.ExpenseDao

@Database(
    entities = [ExpenseEntity::class, BudgetLimitEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetLimitDao(): BudgetLimitDao

    companion object {
        const val DATABASE_NAME = "expense_tracker.db"
    }
}
