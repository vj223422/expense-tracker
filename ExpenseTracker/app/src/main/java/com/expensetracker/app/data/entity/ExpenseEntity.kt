package com.expensetracker.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.expensetracker.app.data.model.ExpenseCategory

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("profileId"), Index("profileId", "budgetMonth")],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val amountMinor: Long,
    val category: ExpenseCategory,
    val note: String,
    /** True when money was received; false for an expense/debit. */
    val isIncome: Boolean = false,
    /** Budget period displayed as YYYY-MM. This can differ from the transaction date month. */
    val budgetMonth: String,
    /** [java.time.LocalDate.toEpochDay] — a plain Long sorts/filters without a TypeConverter. */
    val epochDay: Long,
    val createdAtEpochMillis: Long,
)
