package com.expensetracker.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.expensetracker.app.core.designsystem.CreateProfileDialog
import com.expensetracker.app.core.theme.ExpenseTrackerTheme
import com.expensetracker.app.data.prefs.AppPreferences
import com.expensetracker.app.data.prefs.ThemeMode
import com.expensetracker.app.feature.addexpense.AddExpenseScreen
import com.expensetracker.app.feature.budgets.BudgetsScreen
import com.expensetracker.app.feature.dashboard.DashboardScreen
import com.expensetracker.app.feature.profileswitcher.ProfileSwitcherViewModel
import com.expensetracker.app.feature.reminders.RemindersScreen
import com.expensetracker.app.feature.settings.SettingsScreen
import com.expensetracker.app.feature.transactions.TransactionsScreen
import com.expensetracker.app.feature.upipay.UpiScanPayScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun ExpenseTrackerApp() {
    val appPreferences: AppPreferences = koinInject()
    val themeMode by appPreferences.themeMode.collectAsStateWithLifecycle(ThemeMode.SYSTEM)
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    ExpenseTrackerTheme(darkTheme = darkTheme) {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        val showChrome = currentRoute != Destination.AddExpense.route && currentRoute != Destination.ScanPay.route
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = { if (showChrome) AppTopBar(onScanPayClick = { navController.navigate(Destination.ScanPay.route) }) },
            bottomBar = {
                if (showChrome) NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = backStackEntry?.destination?.hierarchy?.any { it.route == item.destination.route } == true
                        NavigationBarItem(selected = selected, onClick = {
                            navController.navigate(item.destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }, icon = { Icon(if (selected) item.selectedIcon else item.unselectedIcon, item.label) })
                    }
                }
            },
        ) { innerPadding ->
            NavHost(navController, Destination.Dashboard.route, Modifier.padding(innerPadding)) {
                composable(Destination.Dashboard.route) {
                    DashboardScreen(
                        onAddExpenseClick = { navController.navigate(Destination.AddExpense.routeForAdd()) },
                        onEditExpenseClick = { navController.navigate(Destination.AddExpense.routeForEdit(it)) },
                        onSeeAllTransactionsClick = { navController.navigate(Destination.Transactions.route) { launchSingleTop = true; restoreState = true } },
                    )
                }
                composable(Destination.Transactions.route) { TransactionsScreen(onEditExpenseClick = { navController.navigate(Destination.AddExpense.routeForEdit(it)) }) }
                composable(Destination.Reminders.route) { RemindersScreen() }
                composable(Destination.Budgets.route) { BudgetsScreen() }
                composable(Destination.Settings.route) { SettingsScreen() }
                composable(
                    route = Destination.AddExpense.route,
                    arguments = listOf(navArgument(Destination.AddExpense.ARG_EXPENSE_ID) { type = NavType.LongType; defaultValue = Destination.AddExpense.NO_EXPENSE_ID }),
                    enterTransition = { slideInVertically(tween(350)) { it / 4 } + fadeIn(tween(250)) },
                    exitTransition = { slideOutVertically(tween(300)) { it / 4 } + fadeOut(tween(200)) },
                ) { entry ->
                    val expenseId = entry.arguments?.getLong(Destination.AddExpense.ARG_EXPENSE_ID)?.takeIf { it != Destination.AddExpense.NO_EXPENSE_ID }
                    AddExpenseScreen(expenseId = expenseId, onNavigateBack = { navController.popBackStack() })
                }
                composable(Destination.ScanPay.route) {
                    UpiScanPayScreen(onNavigateBack = { navController.popBackStack() })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    onScanPayClick: () -> Unit,
    profileSwitcherViewModel: ProfileSwitcherViewModel = koinViewModel(),
) {
    val uiState by profileSwitcherViewModel.uiState.collectAsStateWithLifecycle()
    val profiles = uiState.profiles
    val activeProfileId = uiState.activeProfileId
    var expanded by remember { mutableStateOf(false) }
    var showCreateProfile by remember { mutableStateOf(false) }
    val activeProfile = uiState.activeProfile

    TopAppBar(
        title = { Text(activeProfile?.name ?: "Expense Tracker") },
        actions = {
            TextButton(onClick = onScanPayClick) { Text("Scan & Pay") }
            Box {
                TextButton(onClick = { expanded = true }) { Text("Profile") }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    profiles.forEach { profile ->
                        DropdownMenuItem(
                            text = { Text(profile.name) },
                            onClick = {
                                profileSwitcherViewModel.onSwitchProfile(profile.id)
                                expanded = false
                            },
                            leadingIcon = {
                                if (profile.id == activeProfileId) Icon(Icons.Default.Check, null)
                            },
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Create profile") },
                        onClick = {
                            expanded = false
                            showCreateProfile = true
                        },
                        leadingIcon = { Icon(Icons.Default.Add, null) },
                    )
                }
            }
        },
    )
    if (showCreateProfile) {
        CreateProfileDialog(
            existingNames = profiles.map { it.name },
            onDismiss = { showCreateProfile = false },
            onConfirm = { name ->
                profileSwitcherViewModel.onCreateProfile(name)
                showCreateProfile = false
            },
        )
    }
}
