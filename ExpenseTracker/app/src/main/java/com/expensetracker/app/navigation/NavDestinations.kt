package com.expensetracker.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Home
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Destination(val route: String) {
    data object Dashboard : Destination("dashboard")
    data object Transactions : Destination("transactions")
    data object ScanPay : Destination("scan_pay")
    data object Budgets : Destination("budgets")
    data object Settings : Destination("settings")

    /** Doubles as the edit screen: absence of [ARG_EXPENSE_ID] (or [NO_EXPENSE_ID]) means "add". */
    data object AddExpense : Destination("add_expense?expenseId={expenseId}") {
        const val ARG_EXPENSE_ID = "expenseId"
        const val NO_EXPENSE_ID = -1L

        fun routeForAdd(): String = "add_expense"
        fun routeForEdit(expenseId: Long): String = "add_expense?expenseId=$expenseId"
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
    BottomNavItem(Destination.ScanPay, "Scan", Icons.Filled.QrCodeScanner, Icons.Outlined.QrCodeScanner),
    BottomNavItem(Destination.Budgets, "Budgets", Icons.Filled.PieChart, Icons.Outlined.PieChart),
    BottomNavItem(Destination.Settings, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
)
