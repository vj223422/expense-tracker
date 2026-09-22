package com.expensetracker.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
import com.expensetracker.app.MainActivity
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
import com.expensetracker.app.feature.fuel.FuelTrackerScreen
import com.expensetracker.app.feature.transactions.TransactionsScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun ExpenseTrackerApp(
    notificationNavigationRequest: Long = 0L,
    notificationNavigationAction: String? = null,
    notificationExpenseId: Long? = null,
) {
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
        val showChrome = currentRoute != Destination.AddExpense.route
        val showAppTopBar = showChrome && currentRoute != Destination.FuelTracker.route

        LaunchedEffect(notificationNavigationRequest) {
            if (notificationNavigationRequest <= 0L) return@LaunchedEffect
            when (notificationNavigationAction) {
                MainActivity.ACTION_OPEN_ADD_EXPENSE -> navController.navigate(Destination.AddExpense.routeForAdd()) { launchSingleTop = true }
                MainActivity.ACTION_OPEN_EDIT_EXPENSE -> notificationExpenseId?.let { navController.navigate(Destination.AddExpense.routeForEdit(it)) }
            }
        }

        Scaffold(
            modifier = Modifier,
            topBar = { if (showAppTopBar) AppTopBar(onSettingsClick = { navController.navigate(Destination.Settings.route) { launchSingleTop = true } }) },
            bottomBar = {
                if (showChrome) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                    ) {
                        bottomNavItems.forEach { item ->
                            val selected = backStackEntry?.destination?.hierarchy?.any { it.route == item.destination.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(if (selected) item.selectedIcon else item.unselectedIcon, contentDescription = item.label) },
                                label = { Text(item.label, style = MaterialTheme.typography.labelMedium) },
                                alwaysShowLabel = true,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            NavHost(navController, Destination.Dashboard.route, Modifier.padding(innerPadding)) {
                composable(Destination.Dashboard.route) {
                    DashboardScreen(
                        onAddExpenseClick = { navController.navigate(Destination.AddExpense.routeForAdd()) },
                        onAddIncomeClick = { navController.navigate(Destination.AddExpense.routeForIncome()) },
                        onEditExpenseClick = { navController.navigate(Destination.AddExpense.routeForEdit(it)) },
                        onSeeAllTransactionsClick = { navController.navigate(Destination.Transactions.route) { launchSingleTop = true; restoreState = true } },
                    )
                }
                composable(Destination.Transactions.route) { TransactionsScreen(onEditExpenseClick = { navController.navigate(Destination.AddExpense.routeForEdit(it)) }) }
                composable(Destination.Reminders.route) { RemindersScreen() }
                composable(Destination.Budgets.route) { BudgetsScreen() }
                composable(Destination.Settings.route) { SettingsScreen(onFuelTrackerClick = { navController.navigate(Destination.FuelTracker.route) }) }
                composable(Destination.FuelTracker.route) {
                    FuelTrackerScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onSettingsClick = {
                            navController.navigate(Destination.Settings.route) { launchSingleTop = true }
                        },
                    )
                }
                composable(
                    route = Destination.AddExpense.route,
                    arguments = listOf(
                        navArgument(Destination.AddExpense.ARG_EXPENSE_ID) { type = NavType.LongType; defaultValue = Destination.AddExpense.NO_EXPENSE_ID },
                        navArgument(Destination.AddExpense.ARG_INCOME) { type = NavType.BoolType; defaultValue = false },
                    ),
                    enterTransition = { slideInVertically(tween(350)) { it / 4 } + fadeIn(tween(250)) },
                    exitTransition = { slideOutVertically(tween(300)) { it / 4 } + fadeOut(tween(200)) },
                ) { entry ->
                    val expenseId = entry.arguments?.getLong(Destination.AddExpense.ARG_EXPENSE_ID)?.takeIf { it != Destination.AddExpense.NO_EXPENSE_ID }
                    val initialIncome = entry.arguments?.getBoolean(Destination.AddExpense.ARG_INCOME) ?: false
                    AddExpenseScreen(
                        expenseId = expenseId,
                        initialIncome = initialIncome,
                        onNavigateBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(onSettingsClick: () -> Unit, profileSwitcherViewModel: ProfileSwitcherViewModel = koinViewModel()) {
    val uiState by profileSwitcherViewModel.uiState.collectAsStateWithLifecycle()
    val profiles = uiState.profiles
    val activeProfileId = uiState.activeProfileId
    var expanded by remember { mutableStateOf(false) }
    var showCreateProfile by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Column {
                Text(
                    "My Expenses",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "All your transactions in one place",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        actions = {
            Box {
                TextButton(
                    onClick = { expanded = true },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    profiles.forEach { profile ->
                        DropdownMenuItem(
                            text = { Text(profile.name) },
                            onClick = { profileSwitcherViewModel.onSwitchProfile(profile.id); expanded = false },
                            leadingIcon = { if (profile.id == activeProfileId) Icon(Icons.Default.Check, null) },
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Create profile") },
                        onClick = { expanded = false; showCreateProfile = true },
                        leadingIcon = { Icon(Icons.Default.Add, null) },
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        onClick = {
                            expanded = false
                            onSettingsClick()
                        },
                        leadingIcon = { Icon(Icons.Default.Settings, null) },
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
    if (showCreateProfile) {
        CreateProfileDialog(
            existingNames = profiles.map { it.name },
            onDismiss = { showCreateProfile = false },
            onConfirm = { name -> profileSwitcherViewModel.onCreateProfile(name); showCreateProfile = false },
        )
    }
}
