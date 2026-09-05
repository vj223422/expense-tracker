package com.expensetracker.app.data.model

import java.time.LocalDate

data class Expense(
    val id: Long,
    val amountMinor: Long,
    val category: ExpenseCategory,
    val note: String,
    val date: LocalDate,
    val createdAtEpochMillis: Long,
)
