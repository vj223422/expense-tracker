package com.expensetracker.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** Sentinel [BudgetLimitEntity.categoryKey] for the overall (all-categories) monthly limit. */
const val OVERALL_BUDGET_KEY = "OVERALL"

@Entity(
    tableName = "budget_limits",
    primaryKeys = ["profileId", "categoryKey"],
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("profileId")],
)
data class BudgetLimitEntity(
    val profileId: Long,
    val categoryKey: String,
    val limitMinor: Long,
    val updatedAtEpochMillis: Long,
)
