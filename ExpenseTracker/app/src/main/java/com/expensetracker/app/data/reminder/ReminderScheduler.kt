package com.expensetracker.app.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.expensetracker.app.data.entity.ReminderEntity
import com.expensetracker.app.data.prefs.AppPreferences
import kotlinx.coroutines.runBlocking

class ReminderScheduler(private val context: Context, private val appPreferences: AppPreferences) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(reminder: ReminderEntity) = runBlocking { schedule(reminder, appPreferences.getReminderSound()) }

    fun schedule(reminder: ReminderEntity, sound: String) {
        if (!reminder.enabled || (reminder.endDateEpochMillis != null && reminder.triggerAtEpochMillis > reminder.endDateEpochMillis)) {
            cancel(reminder.id)
            return
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderReceiver.EXTRA_TITLE, reminder.title)
            putExtra(ReminderReceiver.EXTRA_NOTE, reminder.note)
            putExtra(ReminderReceiver.EXTRA_TRIGGER_AT, reminder.triggerAtEpochMillis)
            putExtra(ReminderReceiver.EXTRA_END_DATE, reminder.endDateEpochMillis ?: -1L)
            putExtra(ReminderReceiver.EXTRA_RECURRENCE, reminder.recurrence)
            putExtra(ReminderReceiver.EXTRA_INTERVAL_DAYS, reminder.customIntervalDays)
            putExtra(ReminderReceiver.EXTRA_SOUND, sound)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // setAndAllowWhileIdle() is inexact and can fire minutes late.
        // Use an exact alarm whenever the user has granted Android's exact-alarm
        // special access, while retaining the inexact fallback on devices where
        // that access has not yet been granted.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.triggerAtEpochMillis,
                pendingIntent,
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.triggerAtEpochMillis,
                pendingIntent,
            )
        }
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
