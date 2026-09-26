package com.expensetracker.app.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.expensetracker.app.data.entity.WaterSettingsEntity
import java.time.Instant
import java.time.ZoneId

class WaterReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(settings: WaterSettingsEntity) {
        cancel(settings.profileId)
        if (!settings.enabled) return
        val now = System.currentTimeMillis()
        val next = settings.nextReminderAtEpochMillis?.takeIf { it > now }
            ?: Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault())
                .plusHours(settings.intervalHours.coerceAtLeast(1).toLong()).toInstant().toEpochMilli()
        val intent = Intent(context, WaterReminderReceiver::class.java).apply {
            putExtra(WaterReminderReceiver.EXTRA_PROFILE_ID, settings.profileId)
        }
        val pi = PendingIntent.getBroadcast(context, code(settings.profileId), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi)
        }
    }

    fun cancel(profileId: Long) {
        val pi = PendingIntent.getBroadcast(context, code(profileId), Intent(context, WaterReminderReceiver::class.java), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        pi?.let { alarmManager.cancel(it); it.cancel() }
    }

    private fun code(profileId: Long) = (900000 + profileId % 100000).toInt()
}
