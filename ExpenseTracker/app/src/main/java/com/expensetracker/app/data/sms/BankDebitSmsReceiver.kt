package com.expensetracker.app.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SmsMessage
import com.expensetracker.app.data.model.ExpenseCategory
import com.expensetracker.app.data.notification.NotificationHelper
import com.expensetracker.app.data.repository.AddExpenseResult
import com.expensetracker.app.data.repository.ExpenseRepository
import com.expensetracker.app.data.repository.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BankDebitSmsReceiver : BroadcastReceiver(), KoinComponent {
    private val expenseRepository: ExpenseRepository by inject()
    private val profileRepository: ProfileRepository by inject()
    private val notificationHelper: NotificationHelper by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val pending = goAsync()
        val messages = extractMessages(intent.extras)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                messages.forEach { rawMessage -> processMessage(rawMessage) }
            } finally {
                pending.finish()
                scope.coroutineContext[SupervisorJob]?.cancel()
            }
        }
    }

    private suspend fun processMessage(message: String) {
        val parsed = BankDebitSmsParser.parse(message) ?: return
        val reference = parsed.reference ?: buildFallbackKey(parsed, message)
        if (isAlreadyProcessed(reference)) return

        val profileId = profileRepository.observeActiveProfileId().filterNotNull().first()
        val category = inferCategory(parsed.merchant)
        val note = buildString {
            append("To: ").append(parsed.merchant)
            parsed.reference?.let {
                append(" Ref: ").append(it)
            }
            append(" [SMS]")
        }

        when (expenseRepository.addExpense(
            profileId = profileId,
            amountMinor = parsed.amountMinor,
            category = category,
            note = note,
            date = parsed.date,
        )) {
            is AddExpenseResult.Success -> {
                markProcessed(reference)
                notificationHelper.notifyExpenseAdded(
                    amountMinor = parsed.amountMinor,
                    merchant = parsed.merchant,
                    date = parsed.date,
                )
            }
            is AddExpenseResult.Error -> Unit
        }
    }

    private fun extractMessages(extras: Bundle?): List<String> {
        val pdus = extras?.get("pdus") as? Array<*> ?: return emptyList()
        val format = extras.getString("format")
        return pdus.mapNotNull { pdu ->
            runCatching {
                val sms = if (format != null) {
                    SmsMessage.createFromPdu(pdu as ByteArray, format)
                } else {
                    @Suppress("DEPRECATION")
                    SmsMessage.createFromPdu(pdu as ByteArray)
                }
                sms.messageBody
            }.getOrNull()
        }
    }

    private fun inferCategory(merchant: String): ExpenseCategory {
        val value = merchant.uppercase()
        return when {
            listOf("ZEPTO", "SWIGGY", "ZOMATO", "BLINKIT", "BIGBASKET", "DMART", "GROCERY").any(value::contains) -> ExpenseCategory.FOOD
            listOf("UBER", "OLA", "RAPIDO", "METRO", "BMTC", "TRANSPORT").any(value::contains) -> ExpenseCategory.TRANSPORT
            listOf("AMAZON", "FLIPKART", "MYNTRA", "SHOPPING").any(value::contains) -> ExpenseCategory.SHOPPING
            listOf("NETFLIX", "SPOTIFY", "BOOKMYSHOW").any(value::contains) -> ExpenseCategory.ENTERTAINMENT
            listOf("HOSPITAL", "PHARMACY", "MEDICAL", "APOLLO").any(value::contains) -> ExpenseCategory.HEALTH
            else -> ExpenseCategory.OTHER
        }
    }

    private fun buildFallbackKey(parsed: BankDebitSms, rawMessage: String): String =
        "${parsed.amountMinor}|${parsed.date}|${parsed.merchant}|${rawMessage.hashCode()}"

    private fun isAlreadyProcessed(reference: String): Boolean {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(PROCESSED_REFS_KEY, emptySet())?.contains(reference) == true
    }

    private fun markProcessed(reference: String) {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val refs = (prefs.getStringSet(PROCESSED_REFS_KEY, emptySet()) ?: emptySet()).toMutableSet()
        refs.add(reference)
        if (refs.size > MAX_STORED_REFS) {
            refs.take(refs.size - MAX_STORED_REFS).forEach(refs::remove)
        }
        prefs.edit().putStringSet(PROCESSED_REFS_KEY, refs).apply()
    }

    private val applicationContext: Context
        get() = notificationHelperContext

    private val notificationHelperContext: Context
        get() = notificationHelper.contextForSmsDedup

    companion object {
        private const val PREFS_NAME = "bank_sms_import"
        private const val PROCESSED_REFS_KEY = "processed_refs"
        private const val MAX_STORED_REFS = 200
    }
}
