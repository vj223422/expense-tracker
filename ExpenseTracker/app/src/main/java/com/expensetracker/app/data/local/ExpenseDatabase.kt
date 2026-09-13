package com.expensetracker.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.expensetracker.app.data.entity.BudgetLimitEntity
import com.expensetracker.app.data.entity.ExpenseEntity
import com.expensetracker.app.data.entity.NoteEntity
import com.expensetracker.app.data.entity.ProfileEntity
import com.expensetracker.app.data.entity.ReminderEntity
import com.expensetracker.app.data.local.dao.BudgetLimitDao
import com.expensetracker.app.data.local.dao.ExpenseDao
import com.expensetracker.app.data.local.dao.NoteDao
import com.expensetracker.app.data.local.dao.ProfileDao
import com.expensetracker.app.data.local.dao.ReminderDao

@Database(entities = [ExpenseEntity::class, BudgetLimitEntity::class, ProfileEntity::class, ReminderEntity::class, NoteEntity::class], version = 5, exportSchema = true)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetLimitDao(): BudgetLimitDao
    abstract fun profileDao(): ProfileDao
    abstract fun reminderDao(): ReminderDao
    abstract fun noteDao(): NoteDao

    companion object {
        const val DATABASE_NAME = "expense_tracker.db"
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `profiles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL)")
                db.execSQL("INSERT INTO `profiles` (`id`, `name`, `createdAtEpochMillis`) VALUES (1, 'Me', ${System.currentTimeMillis()})")
                db.execSQL("CREATE TABLE `expenses_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `profileId` INTEGER NOT NULL, `amountMinor` INTEGER NOT NULL, `category` TEXT NOT NULL, `note` TEXT NOT NULL, `epochDay` INTEGER NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, FOREIGN KEY(`profileId`) REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("INSERT INTO `expenses_new` (`id`, `profileId`, `amountMinor`, `category`, `note`, `epochDay`, `createdAtEpochMillis`) SELECT `id`, 1, `amountMinor`, `category`, `note`, `epochDay`, `createdAtEpochMillis` FROM `expenses`")
                db.execSQL("DROP TABLE `expenses`")
                db.execSQL("ALTER TABLE `expenses_new` RENAME TO `expenses`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_profileId` ON `expenses` (`profileId`)")
                db.execSQL("CREATE TABLE `budget_limits_new` (`profileId` INTEGER NOT NULL, `categoryKey` TEXT NOT NULL, `limitMinor` INTEGER NOT NULL, `updatedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`profileId`, `categoryKey`), FOREIGN KEY(`profileId`) REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("INSERT INTO `budget_limits_new` (`profileId`, `categoryKey`, `limitMinor`, `updatedAtEpochMillis`) SELECT 1, `categoryKey`, `limitMinor`, `updatedAtEpochMillis` FROM `budget_limits`")
                db.execSQL("DROP TABLE `budget_limits`")
                db.execSQL("ALTER TABLE `budget_limits_new` RENAME TO `budget_limits`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_budget_limits_profileId` ON `budget_limits` (`profileId`)")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE `reminders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `profileId` INTEGER NOT NULL, `title` TEXT NOT NULL, `note` TEXT NOT NULL, `triggerAtEpochMillis` INTEGER NOT NULL, `recurrence` TEXT NOT NULL, `customIntervalDays` INTEGER NOT NULL, `enabled` INTEGER NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, FOREIGN KEY(`profileId`) REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_profileId` ON `reminders` (`profileId`)")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE `notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `profileId` INTEGER NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `updatedAtEpochMillis` INTEGER NOT NULL, FOREIGN KEY(`profileId`) REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_profileId` ON `notes` (`profileId`)")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `reminders` ADD COLUMN `endDateEpochMillis` INTEGER")
            }
        }
    }
}
