package com.expensetracker.app.data.reminder

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.expensetracker.app.data.local.ExpenseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WaterReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            intent.action != AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        ) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            var db: ExpenseDatabase? = null
            try {
                val database = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    ExpenseDatabase.DATABASE_NAME,
                ).addMigrations(*ExpenseDatabase.allMigrations()).build()
                db = database
                val scheduler = WaterReminderScheduler(context.applicationContext)
                database.waterDao().getSettingsForScheduling().forEach { scheduler.schedule(it) }
            } catch (_: Exception) {
                // A later boot or exact-alarm permission event can retry scheduling.
            } finally {
                db?.close()
                pending.finish()
            }
        }
    }
}
