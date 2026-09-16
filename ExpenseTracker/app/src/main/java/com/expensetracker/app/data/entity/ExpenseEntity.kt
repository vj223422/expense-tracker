package com.expensetracker.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.expensetracker.app.data.model.ExpenseCategory

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(entity = ProfileEntity::class, parentColumns = ["id"], childColumns = ["profileId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("profileId"), Index("profileId", "budgetMonth"), Index("profileId", "budgetCycleStartEpochMillis")],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val amountMinor: Long,
    val category: ExpenseCategory,
    val note: String,
    val isIncome: Boolean = false,
    /** User-facing cycle month label (YYYY-MM), e.g. 2026-09 for a cycle starting Aug 30. */
    val budgetMonth: String,
    /** Exact transaction timestamp used to assign the transaction to a salary-based cycle. */
    val createdAtEpochMillis: Long,
    /** Exact start timestamp of the budget cycle this transaction belongs to. */
    val budgetCycleStartEpochMillis: Long = 0L,
    /** Transaction date used for history/display. */
    val epochDay: Long,
)
