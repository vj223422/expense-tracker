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
            try {
                saveCrash(appContext, thread, throwable)
                notifyCrash(appContext)
            } catch (_: Throwable) {
                // Diagnostics must never interfere with the original crash.
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun consumeLastCrash(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CRASH, null)
    }

    fun clearLastCrash(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_CRASH)
            .apply()
    }

    private fun saveCrash(context: Context, thread: Thread, throwable: Throwable) {
        val stack = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
        val report = buildString {
            appendLine("Kanakku crash report")
            appendLine("Thread: ${thread.name}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine()
            append(stack)
        }.take(12000)

        // commit() is intentional: the process may be killed immediately after an uncaught exception.
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CRASH, report)
            .commit()
    }

    private fun notifyCrash(context: Context) {
        try {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "Crash reports",
                        NotificationManager.IMPORTANCE_HIGH,
                    ).apply { description = "Notifications when Kanakku crashes" },
                )
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Kanakku crashed")
                .setContentText("Crash details were saved. Open Kanakku to view them.")
                .setStyle(NotificationCompat.BigTextStyle().bigText("Crash details were saved. Open Kanakku to view them."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            manager.notify(NOTIFICATION_ID, notification)
        } catch (_: Throwable) {
            // Notification permission can be denied; the saved report remains available in-app.
        }
    }
}
