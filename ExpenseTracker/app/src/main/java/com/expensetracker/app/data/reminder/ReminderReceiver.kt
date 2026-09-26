package com.expensetracker.app.data.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.media.AudioManager
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
import com.expensetracker.app.data.prefs.AppPreferences
import com.expensetracker.app.data.sound.UISfxSoundPlayer
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
        val sound = intent.getStringExtra(EXTRA_SOUND).orEmpty().ifBlank { "DEFAULT" }
        showNotification(context, id, title, note, sound)

        val recurrence = intent.getStringExtra(EXTRA_RECURRENCE).orEmpty()
        val current = intent.getLongExtra(EXTRA_TRIGGER_AT, System.currentTimeMillis())
        val endDate = intent.getLongExtra(EXTRA_END_DATE, -1L).takeIf { it >= 0L }
        val intervalDays = intent.getIntExtra(EXTRA_INTERVAL_DAYS, 1).coerceAtLeast(1)
        val next = nextOccurrence(current, recurrence, intervalDays) ?: return

        if (endDate != null && next > endDate) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val db = Room.databaseBuilder(context.applicationContext, ExpenseDatabase::class.java, ExpenseDatabase.DATABASE_NAME)
                .addMigrations(*ExpenseDatabase.allMigrations())
                .build()
            try {
                val existing = db.reminderDao().getById(id)
                if (existing?.enabled == true && (existing.endDateEpochMillis == null || next <= existing.endDateEpochMillis)) {
                    val updated = existing.copy(triggerAtEpochMillis = next, sound = sound)
                    db.reminderDao().update(updated)
                    ReminderScheduler(context.applicationContext, AppPreferences(context.applicationContext)).schedule(updated)
                }
            } finally {
                db.close()
                pending.finish()
            }
        }
    }

    private fun showNotification(context: Context, id: Long, title: String, note: String, sound: String) {
        val channelId = ensureChannel(context, sound)
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!allowed) return
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(note.ifBlank { "Reminder" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(note.ifBlank { "Reminder" }))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id.toInt(), notification)
        playCustomSound(context, sound)
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

    private fun ensureChannel(context: Context, sound: String): String {
        val channelId = CHANNEL_ID + "_" + sound.lowercase()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audio = when (sound) {
                "ALARM" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                "RINGTONE" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            val channel = NotificationChannel(channelId, soundLabel(sound), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.notification_channel_reminders_description)
                if (sound == "DEFAULT" || sound == "ALARM" || sound == "RINGTONE") setSound(audio, android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return channelId
    }

    private fun playCustomSound(context: Context, sound: String) {
        if (sound == "DEFAULT" || sound == "ALARM" || sound == "RINGTONE" || sound == "SILENT") return

        if (sound.startsWith("UISFX_")) {
            UISfxSoundPlayer.play(context, sound.removePrefix("UISFX_").lowercase())
            return
        }
        val tone = when (sound) {
            "DOUBLE_BEEP" -> ToneGenerator.TONE_PROP_ACK
            "CHIME" -> ToneGenerator.TONE_PROP_ACK
            "SOFT" -> ToneGenerator.TONE_PROP_BEEP
            "URGENT" -> ToneGenerator.TONE_PROP_NACK
            else -> ToneGenerator.TONE_PROP_BEEP
        }
        val duration = when (sound) {
            "DOUBLE_BEEP" -> 140
            "CHIME" -> 220
            "SOFT" -> 120
            "URGENT" -> 300
            else -> 180
        }
        val generator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
        generator.startTone(tone, duration)
        if (sound == "DOUBLE_BEEP") {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ generator.startTone(tone, duration) }, 220L)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ generator.release() }, 500L)
        } else {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ generator.release() }, (duration + 50).toLong())
        }
    }

    private fun soundLabel(sound: String): String = when (sound) {
        "ALARM" -> "Reminders · Alarm"
        "RINGTONE" -> "Reminders · Ringtone"
        "SILENT" -> "Reminders · Silent"
        "BEEP" -> "Reminders · Single beep"
        "DOUBLE_BEEP" -> "Reminders · Double beep"
        "CHIME" -> "Reminders · Chime"
        "SOFT" -> "Reminders · Soft alert"
        "URGENT" -> "Reminders · Urgent alert"
        "UISFX_NOTIFICATION" -> "Reminders · UISFX Notification"
        "UISFX_SUCCESS" -> "Reminders · UISFX Success"
        "UISFX_WARNING" -> "Reminders · UISFX Warning"
        "UISFX_ERROR" -> "Reminders · UISFX Error"
        "UISFX_COMPLETE" -> "Reminders · UISFX Complete"
        "UISFX_REWARD" -> "Reminders · UISFX Reward"
        "UISFX_CHECK" -> "Reminders · UISFX Check"
        "UISFX_SELECT" -> "Reminders · UISFX Select"
        else -> "Reminders · Default"
    }

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_NOTE = "note"
        const val EXTRA_TRIGGER_AT = "trigger_at"
        const val EXTRA_END_DATE = "end_date"
        const val EXTRA_RECURRENCE = "recurrence"
        const val EXTRA_INTERVAL_DAYS = "interval_days"
        const val EXTRA_SOUND = "sound"
        const val CHANNEL_ID = "reminders"
    }
}
