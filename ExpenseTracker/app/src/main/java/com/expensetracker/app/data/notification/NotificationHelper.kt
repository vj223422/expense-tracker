package com.expensetracker.app.data.notification

import android.Manifest
import android.annotation.SuppressLint
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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

    @SuppressLint("MissingPermission")
    fun notify(alert: LimitAlert) {
        if (!hasNotificationPermission()) return

        val scopeLabel = alert.category?.displayName ?: "Overall"
        val overLimit = alert.spentMinor > alert.limitMinor
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

        val manager = NotificationManagerCompat.from(context)
        val warningNotificationId = "${alert.profileId}_$scopeLabel".hashCode()
        val notificationId = if (alert.tier == AlertTier.CRITICAL) {
            manager.cancel(warningNotificationId)
            "${alert.profileId}_${scopeLabel}_${alert.spentMinor}".hashCode()
        } else {
            warningNotificationId
        }
        manager.notify(notificationId, notification)
    }

    @SuppressLint("MissingPermission")
    fun notifyExpenseAdded(amountMinor: Long, merchant: String, date: LocalDate) {
        if (!hasNotificationPermission()) return
        notifyTransactionAdded(
            title = "Expense added automatically",
            body = "${amountMinor.formatAsCurrency()} paid to $merchant on ${formatDate(date)} was added to your expenses.",
            summary = "${amountMinor.formatAsCurrency()} • $merchant • ${formatDate(date)}",
        )
    }

    @SuppressLint("MissingPermission")
    fun notifyIncomeAdded(amountMinor: Long, source: String, date: LocalDate) {
        if (!hasNotificationPermission()) return
        notifyTransactionAdded(
            title = "Money received automatically",
            body = "${amountMinor.formatAsCurrency()} received from $source on ${formatDate(date)} was added to your income.",
            summary = "+${amountMinor.formatAsCurrency()} • $source • ${formatDate(date)}",
        )
    }

    @SuppressLint("MissingPermission")
    fun notifySmsImportMissed() {
        if (!hasNotificationPermission()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Possible transaction missed")
            .setContentText("A bank SMS was received but couldn't be added automatically.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "A bank transaction SMS was received, but Expense Tracker couldn't process it automatically. Please check the SMS and add the transaction manually if needed.",
                ),
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(
            "sms_import_missed".hashCode(),
            notification,
        )
    }

    @SuppressLint("MissingPermission")
    private fun notifyTransactionAdded(title: String, body: String, summary: String) {
        if (!hasNotificationPermission()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(
            "sms_transaction_${System.currentTimeMillis()}".hashCode(),
            notification,
        )
    }

    private fun formatDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.US))

    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
}
