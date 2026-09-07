package com.expensetracker.app.data.notification

import com.expensetracker.app.data.model.ExpenseCategory

/**
 * WARNING (50%+) is a one-shot heads-up; CRITICAL (80%+) is the "keep telling me" zone — every
 * qualifying expense re-notifies while in it, all the way past 100%. See maybeAlert in
 * ExpenseRepositoryImpl for how each tier's repeat behavior is actually applied.
 */
enum class AlertTier { NONE, WARNING, CRITICAL }

data class LimitAlert(
    /** null = the overall monthly limit rather than a per-category one. */
    val category: ExpenseCategory?,
    val tier: AlertTier,
    val spentMinor: Long,
    val limitMinor: Long,
)

/** Pure threshold math — kept separate from I/O so it's trivial to unit test. */
object LimitAlertEvaluator {
    private const val WARNING_RATIO = 0.5f
    private const val CRITICAL_RATIO = 0.8f

    fun tierFor(spentMinor: Long, limitMinor: Long): AlertTier {
        if (limitMinor <= 0) return AlertTier.NONE
        val ratio = spentMinor.toFloat() / limitMinor.toFloat()
        return when {
            ratio >= CRITICAL_RATIO -> AlertTier.CRITICAL
            ratio >= WARNING_RATIO -> AlertTier.WARNING
            else -> AlertTier.NONE
        }
    }
}
