package com.expensetracker.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Destination(val route: String) {
    data object Dashboard : Destination("dashboard")
    data object Transactions : Destination("transactions")
    data object Insights : Destination("insights")
    data object Budgets : Destination("budgets")
    data object More : Destination("more")
    data object Reminders : Destination("reminders")
    data object Settings : Destination("settings")
    data object FuelTracker : Destination("fuel_tracker")

    data object AddExpense : Destination("add_expense?expenseId={expenseId}&income={income}") {
        const val ARG_EXPENSE_ID = "expenseId"
        const val ARG_INCOME = "income"
        const val NO_EXPENSE_ID = -1L
        fun routeForAdd(): String = "add_expense?income=false"
        fun routeForIncome(): String = "add_expense?income=true"
        fun routeForEdit(expenseId: Long): String = "add_expense?expenseId=$expenseId&income=false"
    }
}

data class BottomNavItem(
    val destination: Destination,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

val bottomNavItems = listOf(
    BottomNavItem(Destination.Dashboard, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Destination.Transactions, "Transactions", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
    BottomNavItem(Destination.Insights, "Insights", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    BottomNavItem(Destination.Budgets, "Goals", Icons.Filled.TrackChanges, Icons.Outlined.TrackChanges),
    BottomNavItem(Destination.More, "More", Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz),
)
