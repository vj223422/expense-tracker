package com.expensetracker.app.data.crash

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.expensetracker.app.R
import java.io.PrintWriter
import java.io.StringWriter

object CrashReporter {
    private const val PREFS = "crash_reporter"
    private const val KEY_CRASH = "last_crash"
    private const val CHANNEL_ID = "crash_reports"
    private const val NOTIFICATION_ID = 9001

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            saveCrash(appContext, thread, throwable)
            notifyCrash(appContext)
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun consumeLastCrash(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val crash = prefs.getString(KEY_CRASH, null)
        if (crash != null) prefs.edit().remove(KEY_CRASH).apply()
        return crash
    }

    private fun saveCrash(context: Context, thread: Thread, throwable: Throwable) {
        val stack = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
        val report = "Thread: ${thread.name}\n\n$stack"
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CRASH, report.take(12000))
            .apply()
    }

    private fun notifyCrash(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Crash reports",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "Notifications when Kanakku crashes" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Kanakku crashed")
            .setContentText("Crash details were saved. Open Kanakku to view them.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Crash details were saved. Open Kanakku to view them."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }
}
