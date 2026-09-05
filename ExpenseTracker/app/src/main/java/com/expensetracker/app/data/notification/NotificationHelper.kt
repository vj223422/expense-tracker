package com.expensetracker.app.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.expensetracker.app.R
import com.expensetracker.app.core.util.formatAsCurrency

private const val CHANNEL_ID = "budget_alerts"

class NotificationHelper(private val context: Context) {

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_budget_alerts),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notification_channel_budget_alerts_description)
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun notify(alert: LimitAlert) {
        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return

        val scopeLabel = alert.category?.displayName ?: "Overall"
        val overLimit = alert.spentMinor >= alert.limitMinor
        val title = when {
            overLimit -> "$scopeLabel budget exceeded"
            alert.tier == AlertTier.CRITICAL -> "$scopeLabel budget almost exceeded"
            else -> "$scopeLabel budget past halfway"
        }
        val body = "Spent ${alert.spentMinor.formatAsCurrency()} of ${alert.limitMinor.formatAsCurrency()}"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        // CRITICAL repeats on every qualifying expense (see maybeAlert) — key it by the running
        // total so each one lands as its own notification instead of silently overwriting the
        // last. WARNING only ever fires once, so a stable per-scope id is fine there.
        val notificationId = if (alert.tier == AlertTier.CRITICAL) {
            (scopeLabel + alert.spentMinor).hashCode()
        } else {
            scopeLabel.hashCode()
        }
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
