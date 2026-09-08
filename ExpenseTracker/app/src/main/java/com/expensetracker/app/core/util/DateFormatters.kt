package com.expensetracker.app.core.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val dayMonthFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
private val dayMonthYearFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

fun LocalDate.toRelativeOrFormatted(today: LocalDate = LocalDate.now()): String = when (this) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    // Without the year, an expense from any prior year renders identically to one from this
    // year on the same day/month — indistinguishable once history spans more than ~12 months.
    else -> format(if (year == today.year) dayMonthFormatter else dayMonthYearFormatter)
}

fun YearMonth.toDisplayString(): String = format(monthYearFormatter)

fun YearMonth.shortLabel(): String =
    month.getDisplayName(TextStyle.SHORT, Locale.getDefault())

val YearMonth.startEpochDay: Long get() = atDay(1).toEpochDay()
val YearMonth.endEpochDay: Long get() = atEndOfMonth().toEpochDay()
