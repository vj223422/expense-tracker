package com.expensetracker.app.data.repository

import com.expensetracker.app.data.entity.BudgetLimitEntity
import com.expensetracker.app.data.entity.ExpenseEntity
import com.expensetracker.app.data.entity.OVERALL_BUDGET_KEY
import com.expensetracker.app.data.model.BudgetLimit
import com.expensetracker.app.data.model.Expense
import com.expensetracker.app.data.model.ExpenseCategory
import java.time.LocalDate

internal fun ExpenseEntity.toDomain(): Expense = Expense(
    id = id,
    amountMinor = amountMinor,
    category = category,
    note = note,
    date = LocalDate.ofEpochDay(epochDay),
    createdAtEpochMillis = createdAtEpochMillis,
)

internal fun Expense.toEntity(): ExpenseEntity = ExpenseEntity(
    id = id,
    amountMinor = amountMinor,
    category = category,
    note = note,
    epochDay = date.toEpochDay(),
    createdAtEpochMillis = createdAtEpochMillis,
)

internal fun BudgetLimitEntity.toDomain(): BudgetLimit = BudgetLimit(
    category = categoryKey.takeIf { it != OVERALL_BUDGET_KEY }?.let { ExpenseCategory.valueOf(it) },
    limitMinor = limitMinor,
)

internal fun BudgetLimit.categoryKey(): String = category?.name ?: OVERALL_BUDGET_KEY
