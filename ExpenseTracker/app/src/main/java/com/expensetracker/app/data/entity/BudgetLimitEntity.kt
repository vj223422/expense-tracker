package com.expensetracker.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Sentinel [BudgetLimitEntity.categoryKey] for the overall (all-categories) monthly limit. */
const val OVERALL_BUDGET_KEY = "OVERALL"

@Entity(tableName = "budget_limits")
data class BudgetLimitEntity(
    @PrimaryKey val categoryKey: String,
    val limitMinor: Long,
    val updatedAtEpochMillis: Long,
)
