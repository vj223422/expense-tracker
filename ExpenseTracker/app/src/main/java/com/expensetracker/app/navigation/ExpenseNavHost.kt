package com.expensetracker.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.expensetracker.app.core.theme.ExpenseTrackerTheme
import com.expensetracker.app.data.prefs.AppPreferences
import com.expensetracker.app.data.prefs.ThemeMode
import com.expensetracker.app.feature.addexpense.AddExpenseScreen
import com.expensetracker.app.feature.budgets.BudgetsScreen
import com.expensetracker.app.feature.dashboard.DashboardScreen
import com.expensetracker.app.feature.settings.SettingsScreen
import com.expensetracker.app.feature.transactions.TransactionsScreen
import org.koin.compose.koinInject

@Composable
fun ExpenseTrackerApp() {
    val appPreferences: AppPreferences = koinInject()
    val themeMode by appPreferences.themeMode.collectAsStateWithLifecycle(ThemeMode.SYSTEM)
    val dynamicColorEnabled by appPreferences.dynamicColorEnabled.collectAsStateWithLifecycle(true)
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    ExpenseTrackerTheme(darkTheme = darkTheme, dynamicColor = dynamicColorEnabled) {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        val showChrome = currentRoute != Destination.AddExpense.route
        val fabRoutes = setOf(Destination.Dashboard.route, Destination.Transactions.route)

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (showChrome) {
                    NavigationBar {
                        bottomNavItems.forEach { item ->
                            val selected = backStackEntry?.destination?.hierarchy
                                ?.any { it.route == item.destination.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label,
                                    )
                                },
                                label = { Text(item.label) },
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (showChrome && currentRoute in fabRoutes) {
                    FloatingActionButton(onClick = { navController.navigate(Destination.AddExpense.route) }) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = "Add expense")
                    }
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Destination.Dashboard.route,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(Destination.Dashboard.route) {
                    DashboardScreen(
                        onAddExpenseClick = { navController.navigate(Destination.AddExpense.route) },
                        onSeeAllTransactionsClick = {
                            navController.navigate(Destination.Transactions.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
                composable(Destination.Transactions.route) {
                    TransactionsScreen(onAddExpenseClick = { navController.navigate(Destination.AddExpense.route) })
                }
                composable(Destination.Budgets.route) {
                    BudgetsScreen()
                }
                composable(Destination.Settings.route) {
                    SettingsScreen()
                }
                composable(
                    route = Destination.AddExpense.route,
                    enterTransition = {
                        slideInVertically(animationSpec = tween(350)) { height -> height / 4 } + fadeIn(tween(250))
                    },
                    exitTransition = {
                        slideOutVertically(animationSpec = tween(300)) { height -> height / 4 } + fadeOut(tween(200))
                    },
                ) {
                    AddExpenseScreen(onNavigateBack = { navController.popBackStack() })
                }
            }
        }
    }
}
