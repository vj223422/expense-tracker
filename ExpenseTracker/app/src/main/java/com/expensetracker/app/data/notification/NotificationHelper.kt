package com.expensetracker.app.data.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.expensetracker.app.MainActivity
import com.expensetracker.app.R
import com.expensetracker.app.core.util.formatAsCurrency
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val CHANNEL_ID = "budget_alerts"

class NotificationHelper(private val context: Context) {
    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(context.getString(R.string.notification_channel_budget_alerts), context.getString(R.string.notification_channel_budget_alerts), NotificationManager.IMPORTANCE_DEFAULT).apply {
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
            .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT).setAutoCancel(true).build()
        val manager = NotificationManagerCompat.from(context)
        val warningNotificationId = "${alert.profileId}_$scopeLabel".hashCode()
        val notificationId = if (alert.tier == AlertTier.CRITICAL) { manager.cancel(warningNotificationId); "${alert.profileId}_${scopeLabel}_${alert.spentMinor}".hashCode() } else warningNotificationId
        manager.notify(notificationId, notification)
    }

    @SuppressLint("MissingPermission")
    fun notifyExpenseAdded(amountMinor: Long, merchant: String, date: LocalDate, expenseId: Long) {
        if (!hasNotificationPermission()) return
        notifyTransactionAdded("Expense added automatically", "${amountMinor.formatAsCurrency()} paid to $merchant on ${formatDate(date)} was added to your expenses. Tap to add a note.", "${amountMinor.formatAsCurrency()} • $merchant • ${formatDate(date)}", createEditExpensePendingIntent(expenseId))
    }

    @SuppressLint("MissingPermission")
    fun notifyIncomeAdded(amountMinor: Long, source: String, date: LocalDate, expenseId: Long, salaryDetected: Boolean) {
        if (!hasNotificationPermission()) return
        if (salaryDetected) {
            notifyTransactionAdded("Salary detected", "${amountMinor.formatAsCurrency()} received from $source on ${formatDate(date)}. This starts your next budget period automatically.", "+${amountMinor.formatAsCurrency()} • $source • Next budget", null)
        } else {
            val actionIntent = Intent(context, BudgetCycleActionReceiver::class.java).apply {
                action = BudgetCycleActionReceiver.ACTION_START_BUDGET
                putExtra(BudgetCycleActionReceiver.EXTRA_EXPENSE_ID, expenseId)
            }
            val actionPendingIntent = PendingIntent.getBroadcast(context, expenseId.hashCode(), actionIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Income received")
                .setContentText("${amountMinor.formatAsCurrency()} received. Use it to start the next budget cycle?")
                .setStyle(NotificationCompat.BigTextStyle().bigText("${amountMinor.formatAsCurrency()} received from $source on ${formatDate(date)}. If this is your salary, start the next budget cycle from this income."))
                .addAction(android.R.drawable.ic_menu_agenda, "Start next budget", actionPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT).setAutoCancel(true).build()
            NotificationManagerCompat.from(context).notify("budget_cycle_$expenseId".hashCode(), notification)
        }
    }

    @SuppressLint("MissingPermission")
    fun notifySmsImportMissed() {
        if (!hasNotificationPermission()) return
        val openAddExpenseIntent = Intent(context, MainActivity::class.java).apply { action = MainActivity.ACTION_OPEN_ADD_EXPENSE; flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP }
        val pendingIntent = PendingIntent.getActivity(context, SMS_IMPORT_MISSED_REQUEST_CODE, openAddExpenseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Possible transaction missed").setContentText("A bank SMS was received but couldn't be added automatically.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("A bank transaction SMS was received, but Expense Tracker couldn't process it automatically. Tap to add the transaction manually."))
            .setPriority(NotificationCompat.PRIORITY_HIGH).setContentIntent(pendingIntent).setAutoCancel(true).build()
        NotificationManagerCompat.from(context).notify("sms_import_missed".hashCode(), notification)
    }

    @SuppressLint("MissingPermission")
    private fun notifyTransactionAdded(title: String, body: String, summary: String, contentIntent: PendingIntent?) {
        if (!hasNotificationPermission()) return
        val builder = NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body)).setPriority(NotificationCompat.PRIORITY_DEFAULT).setAutoCancel(true)
        contentIntent?.let(builder::setContentIntent)
        NotificationManagerCompat.from(context).notify("sms_transaction_${System.currentTimeMillis()}".hashCode(), builder.build())
    }

    private fun createEditExpensePendingIntent(expenseId: Long): PendingIntent = PendingIntent.getActivity(context, expenseId.hashCode(), Intent(context, MainActivity::class.java).apply {
        action = MainActivity.ACTION_OPEN_EDIT_EXPENSE; flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP; putExtra(MainActivity.EXTRA_EXPENSE_ID, expenseId)
    }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun formatDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.US))
    private fun hasNotificationPermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    companion object { private const val SMS_IMPORT_MISSED_REQUEST_CODE = 2201 }
}
