package com.expensetracker.app.data.model

import androidx.compose.runtime.Immutable

/** @Immutable — see data/model/Expense.kt; this one is passed into ProfileRow / the top-bar switcher. */
@Immutable
data class Profile(
    val id: Long,
    val name: String,
    val createdAtEpochMillis: Long,
)
