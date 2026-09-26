package com.expensetracker.app.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.expensetracker.app.data.entity.WaterSettingsEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class WaterReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(settings: WaterSettingsEntity) {
        cancel(settings.profileId)
        if (!settings.enabled) return
        val now = System.currentTimeMillis()
        val next = nextReminderAt(settings, now)
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

    private fun nextReminderAt(settings: WaterSettingsEntity, now: Long): Long {
        val zone = ZoneId.systemDefault()
        val current = Instant.ofEpochMilli(now).atZone(zone)
        val start = settings.startTimeMinutes.coerceIn(0, 1439)
        val end = settings.endTimeMinutes.coerceIn(start, 1439)
        val count = settings.reminderCount.coerceAtLeast(1)

        for (dayOffset in 0..1) {
            val date = current.toLocalDate().plusDays(dayOffset.toLong())
            if (count == 1) {
                val candidate = date.atStartOfDay(zone).plusMinutes(start.toLong()).toInstant().toEpochMilli()
                if (candidate > now + 1000L) return candidate
                continue
            }

            val span = end - start
            for (index in 0 until count) {
                val minute = start + kotlin.math.round(span.toDouble() * index / (count - 1)).toInt()
                val candidate = date.atStartOfDay(zone).plusMinutes(minute.toLong()).toInstant().toEpochMilli()
                if (candidate > now + 1000L) return candidate
            }
        }

        return LocalDate.now(zone).plusDays(1)
            .atStartOfDay(zone)
            .plusMinutes(start.toLong())
            .toInstant()
            .toEpochMilli()
    }

    fun cancel(profileId: Long) {
        val pi = PendingIntent.getBroadcast(context, code(profileId), Intent(context, WaterReminderReceiver::class.java), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        pi?.let { alarmManager.cancel(it); it.cancel() }
    }

    private fun code(profileId: Long) = (900000 + profileId % 100000).toInt()
}
