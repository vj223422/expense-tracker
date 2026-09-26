package com.expensetracker.app.data.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.expensetracker.app.data.local.ExpenseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

class WaterReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val profileId = intent.getLongExtra(EXTRA_PROFILE_ID, -1L)
        if (profileId < 0) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val db = Room.databaseBuilder(context.applicationContext, ExpenseDatabase::class.java, ExpenseDatabase.DATABASE_NAME)
                .addMigrations(*ExpenseDatabase.allMigrations()).build()
            try {
                val settings = db.waterDao().getSettings(profileId) ?: return@launch
                if (!settings.enabled) return@launch
                val allowed = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                if (allowed) {
                    val action = Intent(context, WaterReminderActionReceiver::class.java).apply {
                        putExtra(WaterReminderActionReceiver.EXTRA_PROFILE_ID, profileId)
                        putExtra(WaterReminderActionReceiver.EXTRA_AMOUNT_ML, settings.intakePerReminderMl)
                    }
                    val actionPi = PendingIntent.getBroadcast(context, actionCode(profileId), action, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_popup_reminder)
                        .setContentTitle("Time to hydrate")
                        .setContentText("Drink " + settings.intakePerReminderMl + " ml of water")
                        .setStyle(NotificationCompat.BigTextStyle().bigText("Daily goal: " + settings.dailyGoalMl + " ml. Tap Done after drinking " + settings.intakePerReminderMl + " ml."))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .addAction(android.R.drawable.ic_menu_add, "Mark as done", actionPi)
                        .build()
                    NotificationManagerCompat.from(context).notify(notificationId(profileId), notification)
                    playDropSound()
                }
                val next = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneId.systemDefault())
                    .plusHours(settings.intervalHours.coerceAtLeast(1).toLong()).toInstant().toEpochMilli()
                val updated = settings.copy(nextReminderAtEpochMillis = next)
                db.waterDao().upsertSettings(updated)
                WaterReminderScheduler(context.applicationContext).schedule(updated)
            } finally {
                db.close()
                pending.finish()
            }
        }
    }

    private fun playDropSound() {
        val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 120)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 180)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ tone.release() }, 220)
        }, 100)
    }

    companion object {
        const val EXTRA_PROFILE_ID = "water_profile_id"
        const val CHANNEL_ID = "water_reminders"
        private fun notificationId(id: Long) = (910000 + id % 100000).toInt()
        private fun actionCode(id: Long) = (920000 + id % 100000).toInt()
    }
}
