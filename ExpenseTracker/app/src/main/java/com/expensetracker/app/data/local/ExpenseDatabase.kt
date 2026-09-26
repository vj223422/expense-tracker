package com.expensetracker.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.expensetracker.app.data.entity.BudgetLimitEntity
import com.expensetracker.app.data.entity.ExpenseEntity
import com.expensetracker.app.data.entity.FuelLogEntity
import com.expensetracker.app.data.entity.WaterIntakeEntity
import com.expensetracker.app.data.entity.WaterSettingsEntity
import com.expensetracker.app.data.entity.NoteEntity
import com.expensetracker.app.data.entity.ProfileEntity
import com.expensetracker.app.data.entity.ReminderEntity
import com.expensetracker.app.data.local.dao.BudgetLimitDao
import com.expensetracker.app.data.local.dao.ExpenseDao
import com.expensetracker.app.data.local.dao.FuelLogDao
import com.expensetracker.app.data.local.dao.NoteDao
import com.expensetracker.app.data.local.dao.ProfileDao
import com.expensetracker.app.data.local.dao.ReminderDao
import com.expensetracker.app.data.local.dao.WaterDao

@Database(entities = [ExpenseEntity::class, BudgetLimitEntity::class, ProfileEntity::class, ReminderEntity::class, NoteEntity::class, FuelLogEntity::class, WaterSettingsEntity::class, WaterIntakeEntity::class], version = 14, exportSchema = true)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun fuelLogDao(): FuelLogDao
    abstract fun budgetLimitDao(): BudgetLimitDao
    abstract fun profileDao(): ProfileDao
    abstract fun reminderDao(): ReminderDao
    abstract fun noteDao(): NoteDao
    abstract fun waterDao(): WaterDao

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
        val MIGRATION_2_3 = object : Migration(2, 3) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("CREATE TABLE `reminders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `profileId` INTEGER NOT NULL, `title` TEXT NOT NULL, `note` TEXT NOT NULL, `triggerAtEpochMillis` INTEGER NOT NULL, `recurrence` TEXT NOT NULL, `customIntervalDays` INTEGER NOT NULL, `enabled` INTEGER NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, FOREIGN KEY(`profileId`) REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"); db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_profileId` ON `reminders` (`profileId`)") } }
        val MIGRATION_3_4 = object : Migration(3, 4) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("CREATE TABLE `notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `profileId` INTEGER NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `updatedAtEpochMillis` INTEGER NOT NULL, FOREIGN KEY(`profileId`) REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"); db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_profileId` ON `notes` (`profileId`)") } }
        val MIGRATION_4_5 = object : Migration(4, 5) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE `reminders` ADD COLUMN `endDateEpochMillis` INTEGER") } }
        val MIGRATION_5_6 = object : Migration(5, 6) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE `expenses` ADD COLUMN `isIncome` INTEGER NOT NULL DEFAULT 0") } }
        val MIGRATION_6_7 = object : Migration(6, 7) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE `expenses` ADD COLUMN `budgetMonth` TEXT NOT NULL DEFAULT ''"); db.execSQL("UPDATE `expenses` SET `budgetMonth` = strftime('%Y-%m', `epochDay` * 86400, 'unixepoch') WHERE `budgetMonth` = ''"); db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_profileId_budgetMonth` ON `expenses` (`profileId`, `budgetMonth`)") } }
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `expenses` ADD COLUMN `budgetCycleStartEpochMillis` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_profileId_budgetCycleStartEpochMillis` ON `expenses` (`profileId`, `budgetCycleStartEpochMillis`)")
            }
        }
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `reminders` ADD COLUMN `sound` TEXT NOT NULL DEFAULT 'DEFAULT'")
            }
        }
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `fuel_logs` ADD COLUMN `expenseId` INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fuel_logs_expenseId` ON `fuel_logs` (`expenseId`)")
                // Link existing fuel rows to their matching PETROL expense so legacy data
                // also stays synchronized when either side is deleted.
                db.execSQL(
                    "UPDATE `fuel_logs` SET `expenseId` = (" +
                        "SELECT e.id FROM expenses e " +
                        "WHERE e.profileId = fuel_logs.profileId " +
                        "AND e.amountMinor = fuel_logs.amountMinor " +
                        "AND e.category = 'PETROL' " +
                        "AND e.note = fuel_logs.note " +
                        "AND e.isIncome = 0 " +
                        "AND e.epochDay = fuel_logs.epochDay " +
                        "ORDER BY e.createdAtEpochMillis DESC, e.id DESC LIMIT 1" +
                        ") WHERE `expenseId` IS NULL"
                )
            }
        }
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `fuel_logs` ADD COLUMN `endEpochDay` INTEGER")
                db.execSQL("ALTER TABLE `fuel_logs` ADD COLUMN `endOdometerKm` REAL")
            }
        }
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `fuel_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `profileId` INTEGER NOT NULL, `amountMinor` INTEGER NOT NULL, `liters` REAL NOT NULL, `odometerKm` REAL NOT NULL, `epochDay` INTEGER NOT NULL, `note` TEXT NOT NULL, FOREIGN KEY(`profileId`) REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fuel_logs_profileId` ON `fuel_logs` (`profileId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fuel_logs_profileId_epochDay` ON `fuel_logs` (`profileId`, `epochDay`)")
            }
        }
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS water_settings (profileId INTEGER NOT NULL, enabled INTEGER NOT NULL, dailyGoalMl INTEGER NOT NULL, intervalHours INTEGER NOT NULL, intakePerReminderMl INTEGER NOT NULL, nextReminderAtEpochMillis INTEGER, PRIMARY KEY(profileId))")
                db.execSQL("CREATE TABLE IF NOT EXISTS water_intake (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, profileId INTEGER NOT NULL, amountMl INTEGER NOT NULL, drankAtEpochMillis INTEGER NOT NULL, source TEXT NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_water_intake_profileId_drankAtEpochMillis ON water_intake (profileId, drankAtEpochMillis)")
            }
        }
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE water_settings ADD COLUMN startTimeMinutes INTEGER NOT NULL DEFAULT 480")
                db.execSQL("ALTER TABLE water_settings ADD COLUMN endTimeMinutes INTEGER NOT NULL DEFAULT 1200")
                db.execSQL("ALTER TABLE water_settings ADD COLUMN reminderCount INTEGER NOT NULL DEFAULT 8")
                db.execSQL("UPDATE water_settings SET reminderCount = CASE WHEN intakePerReminderMl > 0 THEN MAX(1, CAST(dailyGoalMl / intakePerReminderMl AS INTEGER)) ELSE 1 END")
            }
        }

        fun allMigrations(): Array<Migration> = arrayOf(
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
            MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
            MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13,
            MIGRATION_13_14
        )

    }
}
