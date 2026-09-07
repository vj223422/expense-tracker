package com.expensetracker.app.data.model

import androidx.compose.runtime.Immutable
import java.time.LocalDate

/**
 * @Immutable — a firm promise the compiler can't verify itself: this class holds [LocalDate] (an
 * external JDK type Compose can't inspect) and is passed straight into list-item composables
 * (ExpenseListItem, SwipeToDeleteExpenseItem), so without this every one of those is unskippable
 * on every recomposition (see core/designsystem — the same reasoning applies to every UiState
 * holding a List<T> or a java.time type across the app).
 */
@Immutable
data class Expense(
    val id: Long,
    val profileId: Long,
    val amountMinor: Long,
    val category: ExpenseCategory,
    val note: String,
    val date: LocalDate,
    val createdAtEpochMillis: Long,
)
