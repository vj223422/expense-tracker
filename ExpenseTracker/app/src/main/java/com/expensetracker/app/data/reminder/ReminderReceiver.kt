package com.expensetracker.app.data.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.expensetracker.app.R
import com.expensetracker.app.data.local.ExpenseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (id < 0) return
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Reminder" }
        val note = intent.getStringExtra(EXTRA_NOTE).orEmpty()
        showNotification(context, id, title, note)

        val recurrence = intent.getStringExtra(EXTRA_RECURRENCE).orEmpty()
        val current = intent.getLongExtra(EXTRA_TRIGGER_AT, System.currentTimeMillis())
        val endDate = intent.getLongExtra(EXTRA_END_DATE, -1L).takeIf { it >= 0L }
        val intervalDays = intent.getIntExtra(EXTRA_INTERVAL_DAYS, 1).coerceAtLeast(1)
        val next = nextOccurrence(current, recurrence, intervalDays) ?: return

        if (endDate != null && next > endDate) return

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
                val existing = db.reminderDao().getById(id)
                if (existing?.enabled == true && (existing.endDateEpochMillis == null || next <= existing.endDateEpochMillis)) {
                    val updated = existing.copy(triggerAtEpochMillis = next)
                    db.reminderDao().update(updated)
                    ReminderScheduler(context.applicationContext).schedule(updated)
                }
            } finally {
                db.close()
                pending.finish()
            }
        }
    }

    private fun showNotification(context: Context, id: Long, title: String, note: String) {
        ensureChannel(context)
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!allowed) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(note.ifBlank { "Reminder" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(note.ifBlank { "Reminder" }))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id.toInt(), notification)
    }

    private fun nextOccurrence(triggerAt: Long, recurrence: String, intervalDays: Int): Long? {
        val dateTime = Instant.ofEpochMilli(triggerAt).atZone(ZoneId.systemDefault())
        val next = when (recurrence) {
            "DAILY" -> dateTime.plusDays(1)
            "WEEKLY" -> dateTime.plusWeeks(1)
            "MONTHLY" -> dateTime.plusMonths(1)
            "YEARLY" -> dateTime.plusYears(1)
            "CUSTOM" -> dateTime.plusDays(intervalDays.toLong())
            else -> return null
        }
        return next.toInstant().toEpochMilli()
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel_reminders), NotificationManager.IMPORTANCE_HIGH).apply {
                    description = context.getString(R.string.notification_channel_reminders_description)
                },
            )
        }
    }

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_NOTE = "note"
        const val EXTRA_TRIGGER_AT = "trigger_at"
        const val EXTRA_END_DATE = "end_date"
        const val EXTRA_RECURRENCE = "recurrence"
        const val EXTRA_INTERVAL_DAYS = "interval_days"
        const val CHANNEL_ID = "reminders"
    }
}
