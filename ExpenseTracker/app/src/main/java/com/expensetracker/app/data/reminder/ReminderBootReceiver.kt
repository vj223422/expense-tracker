package com.expensetracker.app.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.expensetracker.app.data.local.ExpenseDatabase
import com.expensetracker.app.data.prefs.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val db = Room.databaseBuilder(context.applicationContext, ExpenseDatabase::class.java, ExpenseDatabase.DATABASE_NAME)
                .addMigrations(*ExpenseDatabase.allMigrations())
                .build()
            try {
                val scheduler = ReminderScheduler(context.applicationContext, AppPreferences(context.applicationContext))
                db.reminderDao().getEnabled().forEach { scheduler.schedule(it) }
            } finally {
                db.close()
                pending.finish()
            }
        }
    }
}
