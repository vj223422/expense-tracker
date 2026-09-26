package com.expensetracker.app.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.room.Room
import com.expensetracker.app.data.entity.WaterIntakeEntity
import com.expensetracker.app.data.local.ExpenseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WaterReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val profileId = intent.getLongExtra(EXTRA_PROFILE_ID, -1L)
        val amount = intent.getIntExtra(EXTRA_AMOUNT_ML, 0)
        if (profileId < 0 || amount <= 0) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val db = Room.databaseBuilder(context.applicationContext, ExpenseDatabase::class.java, ExpenseDatabase.DATABASE_NAME)
                .addMigrations(*ExpenseDatabase.allMigrations()).build()
            try {
                db.waterDao().insertIntake(WaterIntakeEntity(profileId = profileId, amountMl = amount, drankAtEpochMillis = System.currentTimeMillis()))
                NotificationManagerCompat.from(context).cancel((910000 + profileId % 100000).toInt())
            } finally {
                db.close()
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_PROFILE_ID = "water_profile_id"
        const val EXTRA_AMOUNT_ML = "water_amount_ml"
    }
}
