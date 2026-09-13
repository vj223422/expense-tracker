package com.expensetracker.app.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.expensetracker.app.data.local.ExpenseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val db = Room.databaseBuilder(context.applicationContext, ExpenseDatabase::class.java, ExpenseDatabase.DATABASE_NAME)
                .addMigrations(
                    ExpenseDatabase.MIGRATION_1_2,
                    ExpenseDatabase.MIGRATION_2_3,
                    ExpenseDatabase.MIGRATION_3_4,
                    ExpenseDatabase.MIGRATION_4_5,
                )
                .build()
            try {
                val scheduler = ReminderScheduler(context.applicationContext)
                db.reminderDao().getEnabled().forEach { scheduler.schedule(it) }
            } finally {
                db.close()
                pending.finish()
            }
        }
    }
}
