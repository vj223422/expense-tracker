package com.expensetracker.app.data.notification

import com.expensetracker.app.core.util.formatAsCurrency
import com.expensetracker.app.data.model.ExpenseCategory

/**
 * WARNING (50%+) is a one-shot heads-up; CRITICAL (80%+) is the "keep telling me" zone — every
 * qualifying expense re-notifies while in it, all the way past 100%. See maybeAlert in
 * ExpenseRepositoryImpl for how each tier's repeat behavior is actually applied.
 */
enum class AlertTier { NONE, WARNING, CRITICAL }

data class LimitAlert(
    val profileId: Long,
    /** null = the overall monthly limit rather than a per-category one. */
    val category: ExpenseCategory?,
    val tier: AlertTier,
    val spentMinor: Long,
    val limitMinor: Long,
)

/**
 * In-app fallback wording for a [LimitAlert] — shown as a snackbar by the screen that triggered
 * it. NotificationHelper.notify posts the system notification for the same alert, but that's
 * silently skipped if the user has declined POST_NOTIFICATIONS, so callers of addExpense/
 * updateExpense also surface this directly instead of relying on the system notification alone.
 */
fun LimitAlert.toSnackbarMessage(): String {
    val scopeLabel = category?.displayName ?: "Overall"
    val status = when {
        spentMinor > limitMinor -> "budget exceeded"
        tier == AlertTier.CRITICAL -> "budget almost exceeded"
        else -> "budget past halfway"
    }
    return "$scopeLabel $status — spent ${spentMinor.formatAsCurrency()} of ${limitMinor.formatAsCurrency()}"
}

/** Pure threshold math — kept separate from I/O so it's trivial to unit test. */
object LimitAlertEvaluator {
    private const val WARNING_RATIO = 0.5
    private const val CRITICAL_RATIO = 0.8

    fun tierFor(spentMinor: Long, limitMinor: Long): AlertTier {
        if (limitMinor <= 0) return AlertTier.NONE
        // Double, not Float: Float only represents integers exactly up to ~16.7M, so at limits in
        // the lakhs+ (this app supports up to 100 crore) the rounded ratio can land exactly on a
        // threshold a hair before the true ratio gets there, firing CRITICAL/a notification early.
        val ratio = spentMinor.toDouble() / limitMinor.toDouble()
        return when {
            ratio >= CRITICAL_RATIO -> AlertTier.CRITICAL
            ratio >= WARNING_RATIO -> AlertTier.WARNING
            else -> AlertTier.NONE
        }
    }
}
