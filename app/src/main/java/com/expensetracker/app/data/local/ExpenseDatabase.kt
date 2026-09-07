package com.expensetracker.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.expensetracker.app.data.entity.BudgetLimitEntity
import com.expensetracker.app.data.entity.ExpenseEntity
import com.expensetracker.app.data.entity.ProfileEntity
import com.expensetracker.app.data.local.dao.BudgetLimitDao
import com.expensetracker.app.data.local.dao.ExpenseDao
import com.expensetracker.app.data.local.dao.ProfileDao

// exportSchema is off — nothing consumes the schema history yet since there are no migrations to
// test against. version=2 adds ProfileEntity + profileId on the other two entities; the database
// is opened with fallbackToDestructiveMigration() (see di/AppModules.kt), so upgrading from
// version 1 just wipes local data rather than requiring a hand-written Migration.
@Database(
    entities = [ExpenseEntity::class, BudgetLimitEntity::class, ProfileEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetLimitDao(): BudgetLimitDao
    abstract fun profileDao(): ProfileDao

    companion object {
        const val DATABASE_NAME = "expense_tracker.db"
    }
}
