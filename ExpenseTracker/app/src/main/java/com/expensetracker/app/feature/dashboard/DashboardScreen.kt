package com.expensetracker.app.feature.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.expensetracker.app.core.designsystem.AnimatedAmountText
import com.expensetracker.app.core.designsystem.CategoryDonutChart
import com.expensetracker.app.core.designsystem.CategoryProgressRow
import com.expensetracker.app.core.designsystem.DonutSegment
import com.expensetracker.app.core.designsystem.EmptyState
import com.expensetracker.app.core.designsystem.SectionHeader
import com.expensetracker.app.core.designsystem.StatCard
import com.expensetracker.app.core.designsystem.SwipeToDeleteExpenseItem
import com.expensetracker.app.core.designsystem.color
import com.expensetracker.app.core.theme.LocalExtendedColors
import com.expensetracker.app.core.theme.LocalReducedMotion
import com.expensetracker.app.core.theme.MotionDurations
import com.expensetracker.app.core.theme.MotionEasing
import com.expensetracker.app.core.util.formatAsCurrency
import com.expensetracker.app.core.util.toDisplayString
import com.expensetracker.app.data.model.Expense
import org.koin.androidx.compose.koinViewModel

@Composable
fun DashboardScreen(
    onAddExpenseClick: () -> Unit,
    onEditExpenseClick: (Long) -> Unit,
    onSeeAllTransactionsClick: () -> Unit,
    viewModel: DashboardViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is DashboardEffect.ShowUndoDelete -> {
                        val label = effect.expense.note.ifBlank { effect.expense.category.displayName }
                        val result = snackbarHostState.showSnackbar(
                            message = "Deleted \"$label\"",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.onUndoDelete(effect.expense)
                        }
                    }

                    is DashboardEffect.ShowMessage -> {
                        snackbarHostState.showSnackbar(message = effect.message, duration = SnackbarDuration.Short)
                    }

                    is DashboardEffect.ShowError -> {
                        snackbarHostState.showSnackbar(message = effect.message, duration = SnackbarDuration.Short)
                    }
                }
            }
        }
    }

    // A nested Scaffold here only hosts the undo-delete snackbar; see TransactionsScreen for the
    // same pattern/reasoning.
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        DashboardContent(
            uiState = uiState,
            onAddExpenseClick = onAddExpenseClick,
            onEditExpenseClick = onEditExpenseClick,
            onDeleteExpense = viewModel::onDeleteExpense,
            onSeeAllTransactionsClick = onSeeAllTransactionsClick,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}

@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onAddExpenseClick: () -> Unit,
    onEditExpenseClick: (Long) -> Unit,
    onDeleteExpense: suspend (Expense) -> Boolean,
    onSeeAllTransactionsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val topCategories = uiState.categorySpends.take(5)
    val showEmptyRecent = uiState.recentExpenses.isEmpty() && !uiState.isLoading

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item(key = "month_header") {
            Column {
                Text(
                    text = "Overview",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = uiState.yearMonth.toDisplayString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        item(key = "hero") {
            DashboardHero(uiState = uiState, onAddExpenseClick = onAddExpenseClick)
        }

        if (topCategories.isNotEmpty()) {
            item(key = "top_categories_header") {
                SectionHeader(title = "Top categories")
            }
            items(topCategories, key = { it.category.name }, contentType = { "category_row" }) { categorySpend ->
                CategoryProgressRow(
                    categorySpend = categorySpend,
                    modifier = Modifier.animateItem(),
                )
            }
        }

        item(key = "recent_header") {
            SectionHeader(
                title = "Recent",
                actionLabel = "See all",
                onActionClick = onSeeAllTransactionsClick,
            )
        }

        if (showEmptyRecent) {
            item(key = "recent_empty") {
                EmptyState(
                    icon = Icons.Filled.ReceiptLong,
                    title = "No expenses yet",
                    message = "Tap the Spent card above to add your first expense.",
                )
            }
        } else {
            items(uiState.recentExpenses, key = { it.id }, contentType = { "expense_item" }) { expense ->
                SwipeToDeleteExpenseItem(
                    expense = expense,
                    onDelete = onDeleteExpense,
                    onClick = { onEditExpenseClick(expense.id) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun DashboardHero(
    uiState: DashboardUiState,
    onAddExpenseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val segments = uiState.categorySpends.map { spend ->
        DonutSegment(
            value = spend.spentMinor.toFloat(),
            color = spend.category.color(),
            label = spend.category.displayName,
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
            CategoryDonutChart(
                segments = segments,
                modifier = Modifier.fillMaxSize(),
                centerContent = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AnimatedAmountText(
                            amountMinor = uiState.totalSpentMinor,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "spent this month",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            // height(IntrinsicSize.Max) + fillMaxHeight() on each child: Remaining has an
            // extra progress-bar line Spent doesn't, so without this the two cards' Surfaces
            // would size to their own (different) content heights instead of matching. Always
            // shown (no limit/spend gate) so a fresh, empty profile still reads as "₹0 spent /
            // ₹0 remaining" rather than the row just vanishing.
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Tapping Spent opens Add Expense — this replaces the FAB that used to float over
            // the Recent list; it covered the last row or two of it, especially on short screens.
            StatCard(
                label = "Spent",
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = onAddExpenseClick,
            ) {
                AnimatedAmountText(
                    amountMinor = uiState.totalSpentMinor,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            RemainingStatCard(uiState = uiState, modifier = Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun RemainingStatCard(uiState: DashboardUiState, modifier: Modifier = Modifier) {
    val extended = LocalExtendedColors.current
    val reduceMotion = LocalReducedMotion.current
    val remainingMinor = uiState.remainingMinor
    val progress = uiState.overallProgress
    val remainingColor = when {
        remainingMinor == null -> MaterialTheme.colorScheme.onSurface
        remainingMinor < 0L || progress >= 1f -> extended.danger
        progress >= 0.8f -> extended.warning
        else -> MaterialTheme.colorScheme.onSurface
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(
            durationMillis = if (reduceMotion) 0 else MotionDurations.MEDIUM,
            easing = MotionEasing.Standard,
        ),
        label = "overallProgress",
    )

    StatCard(label = "Remaining", modifier = modifier) {
        Column {
            Text(
                // null means no overall limit is configured at all — distinct from "₹0.00 left",
                // which would wrongly read as an exhausted budget the user never actually set.
                text = remainingMinor?.formatAsCurrency() ?: "No limit set",
                style = MaterialTheme.typography.titleLarge,
                color = remainingColor,
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (remainingMinor != null) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = remainingColor,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
            }
        }
    }
}
