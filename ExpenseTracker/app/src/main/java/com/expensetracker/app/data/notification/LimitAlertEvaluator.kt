package com.expensetracker.app.data.notification

import com.expensetracker.app.data.model.ExpenseCategory

enum class AlertTier { NONE, WARNING, EXCEEDED }

data class LimitAlert(
    /** null = the overall monthly limit rather than a per-category one. */
    val category: ExpenseCategory?,
    val tier: AlertTier,
    val spentMinor: Long,
    val limitMinor: Long,
)

/** Pure threshold math — kept separate from I/O so it's trivial to unit test. */
object LimitAlertEvaluator {
    private const val WARNING_RATIO = 0.8f

    fun tierFor(spentMinor: Long, limitMinor: Long): AlertTier {
        if (limitMinor <= 0) return AlertTier.NONE
        val ratio = spentMinor.toFloat() / limitMinor.toFloat()
        return when {
            ratio >= 1f -> AlertTier.EXCEEDED
            ratio >= WARNING_RATIO -> AlertTier.WARNING
            else -> AlertTier.NONE
        }
    }
}
