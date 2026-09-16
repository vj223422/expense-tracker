package com.expensetracker.app.data.model

import androidx.compose.runtime.Immutable
import java.time.LocalDate

/** @Immutable — transaction model used directly by Compose lists. */
@Immutable
data class Expense(
    val id: Long,
    val profileId: Long,
    val amountMinor: Long,
    val category: ExpenseCategory,
    val note: String,
    val date: LocalDate,
    val createdAtEpochMillis: Long,
    val isIncome: Boolean = false,
    val budgetMonth: String = "",
)
