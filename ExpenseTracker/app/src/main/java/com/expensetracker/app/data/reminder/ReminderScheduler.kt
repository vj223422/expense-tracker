package com.expensetracker.app.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.expensetracker.app.data.entity.ReminderEntity

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(reminder: ReminderEntity) {
        if (!reminder.enabled) {
            cancel(reminder.id)
            return
        }
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderReceiver.EXTRA_TITLE, reminder.title)
            putExtra(ReminderReceiver.EXTRA_NOTE, reminder.note)
            putExtra(ReminderReceiver.EXTRA_TRIGGER_AT, reminder.triggerAtEpochMillis)
            putExtra(ReminderReceiver.EXTRA_RECURRENCE, reminder.recurrence)
            putExtra(ReminderReceiver.EXTRA_INTERVAL_DAYS, reminder.customIntervalDays)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerAtEpochMillis, pendingIntent)
    }

    fun cancel(id: Long) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}
