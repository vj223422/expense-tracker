package com.expensetracker.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.expensetracker.app.data.entity.BudgetLimitEntity
import com.expensetracker.app.data.entity.ExpenseEntity
import com.expensetracker.app.data.entity.ProfileEntity
import com.expensetracker.app.data.local.dao.BudgetLimitDao
import com.expensetracker.app.data.local.dao.ExpenseDao
import com.expensetracker.app.data.local.dao.ProfileDao

// exportSchema is on so future migrations can be tested against real schema history (androidx
// room-testing) instead of only trusted by inspection — see app/schemas and the
// room.schemaLocation ksp arg in app/build.gradle.kts. version=2 added ProfileEntity + profileId
// on the other two entities; MIGRATION_1_2 below is the real upgrade path for that (see
// di/AppModules.kt, which now only falls back to a destructive rebuild on a *downgrade*, not on
// any unhandled upgrade).
@Database(
    entities = [ExpenseEntity::class, BudgetLimitEntity::class, ProfileEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetLimitDao(): BudgetLimitDao
    abstract fun profileDao(): ProfileDao

    companion object {
        const val DATABASE_NAME = "expense_tracker.db"

        /**
         * v1 had no profiles table and no profileId column — this was a single-profile app.
         * Existing expenses/budget limits are attached to a new default profile ("Me") so no
         * data is lost on upgrade. Both tables are fully recreated (not just ALTER TABLE ADD
         * COLUMN) because SQLite can't add a foreign key or change a primary key via ALTER TABLE,
         * and Room's migration validation checks the resulting foreign keys/indices, not just
         * column names.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `profiles` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`createdAtEpochMillis` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "INSERT INTO `profiles` (`id`, `name`, `createdAtEpochMillis`) " +
                        "VALUES (1, 'Me', ${System.currentTimeMillis()})",
                )

                db.execSQL(
                    "CREATE TABLE `expenses_new` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`profileId` INTEGER NOT NULL, " +
                        "`amountMinor` INTEGER NOT NULL, " +
                        "`category` TEXT NOT NULL, " +
                        "`note` TEXT NOT NULL, " +
                        "`epochDay` INTEGER NOT NULL, " +
                        "`createdAtEpochMillis` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`profileId`) REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL(
                    "INSERT INTO `expenses_new` " +
                        "(`id`, `profileId`, `amountMinor`, `category`, `note`, `epochDay`, `createdAtEpochMillis`) " +
                        "SELECT `id`, 1, `amountMinor`, `category`, `note`, `epochDay`, `createdAtEpochMillis` FROM `expenses`",
                )
                db.execSQL("DROP TABLE `expenses`")
                db.execSQL("ALTER TABLE `expenses_new` RENAME TO `expenses`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_profileId` ON `expenses` (`profileId`)")

                db.execSQL(
                    "CREATE TABLE `budget_limits_new` (" +
                        "`profileId` INTEGER NOT NULL, " +
                        "`categoryKey` TEXT NOT NULL, " +
                        "`limitMinor` INTEGER NOT NULL, " +
                        "`updatedAtEpochMillis` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`profileId`, `categoryKey`), " +
                        "FOREIGN KEY(`profileId`) REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL(
                    "INSERT INTO `budget_limits_new` (`profileId`, `categoryKey`, `limitMinor`, `updatedAtEpochMillis`) " +
                        "SELECT 1, `categoryKey`, `limitMinor`, `updatedAtEpochMillis` FROM `budget_limits`",
                )
                db.execSQL("DROP TABLE `budget_limits`")
                db.execSQL("ALTER TABLE `budget_limits_new` RENAME TO `budget_limits`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_budget_limits_profileId` ON `budget_limits` (`profileId`)")
            }
        }
    }
}
