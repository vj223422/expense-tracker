package com.expensetracker.app.feature.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.core.designsystem.AnimatedAmountText
import com.expensetracker.app.core.designsystem.CategoryDonutChart
import com.expensetracker.app.core.designsystem.DonutSegment
import com.expensetracker.app.core.designsystem.EmptyState
import com.expensetracker.app.core.designsystem.SwipeToDeleteExpenseItem
import com.expensetracker.app.core.designsystem.color
import com.expensetracker.app.core.theme.LocalExtendedColors
import com.expensetracker.app.core.theme.LocalReducedMotion
import com.expensetracker.app.core.theme.MotionDurations
import com.expensetracker.app.core.theme.MotionEasing
import com.expensetracker.app.core.util.formatAsCurrency
import com.expensetracker.app.core.util.parseAmountToMinorUnits
import com.expensetracker.app.core.util.toDisplayString
import com.expensetracker.app.data.model.Expense
import com.expensetracker.app.data.model.ExpenseCategory
import org.koin.androidx.compose.koinViewModel
import java.time.YearMonth
import java.util.Locale

@Composable
fun DashboardScreen(
    onAddExpenseClick: () -> Unit,
    onEditExpenseClick: (Long) -> Unit,
    onSeeAllTransactionsClick: () -> Unit,
    viewModel: DashboardViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showBudgetEditor by viewModel.showBudgetEditor.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DashboardEffect.ShowUndoDelete -> {
                    val label = effect.expense.note.ifBlank { effect.expense.category.displayName }
                    val result = snackbarHostState.showSnackbar(message = "Deleted \"$label\"", actionLabel = "Undo", duration = SnackbarDuration.Short)
                    if (result == SnackbarResult.ActionPerformed) viewModel.onUndoDelete(effect.expense)
                }
                is DashboardEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message, duration = SnackbarDuration.Short)
                is DashboardEffect.ShowError -> snackbarHostState.showSnackbar(effect.message, duration = SnackbarDuration.Short)
            }
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize(), snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item { MonthSelector(uiState.yearMonth, viewModel::onPreviousMonth, viewModel::onNextMonth) }
            item { SummaryCard(uiState, onRemainingClick = viewModel::onRemainingClick) }
            item { AddExpenseButton(onAddExpenseClick) }
            val topCategories = uiState.categorySpends.take(5)
            if (topCategories.isNotEmpty()) {
                item { SectionTitle("Top categories", "See all", onSeeAllTransactionsClick) }
                item { LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
                    items(topCategories, key = { it.category.name }) { spend -> CategoryCard(spend.category, spend.spentMinor, uiState.totalSpentMinor) }
                } }
            }
            item { SectionTitle("Recent transactions", "See all", onSeeAllTransactionsClick) }
            if (uiState.recentExpenses.isEmpty() && !uiState.isLoading) {
                item { EmptyState(icon = Icons.Outlined.ReceiptLong, title = "No transactions yet", message = "Your latest expenses and received amounts will appear here.") }
            } else {
                items(uiState.recentExpenses, key = { it.id }) { expense ->
                    SwipeToDeleteExpenseItem(expense = expense, onDelete = viewModel::onDeleteExpense, onClick = { onEditExpenseClick(expense.id) })
                }
            }
        }
    }

    if (showBudgetEditor) BudgetEditorDialog(
        currentLimitMinor = uiState.overallLimitMinor,
        onSave = viewModel::onSaveBudget,
        onClear = viewModel::onClearBudget,
        onDismiss = viewModel::onDismissBudgetEditor,
    )
}

@Composable
private fun MonthSelector(yearMonth: YearMonth, onPreviousMonth: () -> Unit, onNextMonth: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onPreviousMonth) {
                Icon(Icons.Default.ChevronLeft, "Previous month", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CalendarMonth, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(9.dp))
                Text(yearMonth.toDisplayString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            IconButton(onClick = onNextMonth) {
                Icon(Icons.Default.ChevronRight, "Next month", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SummaryCard(uiState: DashboardUiState, onRemainingClick: () -> Unit) {
    val extended = LocalExtendedColors.current
    val reduceMotion = LocalReducedMotion.current
    val progress = uiState.overallProgress
    val animatedProgress by animateFloatAsState(targetValue = progress.coerceIn(0f, 1f), animationSpec = tween(if (reduceMotion) 0 else MotionDurations.MEDIUM, easing = MotionEasing.Standard), label = "dashboardBudgetProgress")
    val remaining = uiState.remainingMinor
    val remainingColor = when {
        remaining == null -> MaterialTheme.colorScheme.onSurface
        remaining < 0L -> MaterialTheme.colorScheme.error
        progress >= .8f -> extended.warning
        else -> extended.safe
    }
    val segments = uiState.categorySpends.map { DonutSegment(it.spentMinor.toFloat(), it.category.color(), it.category.displayName) }

    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest, tonalElevation = 2.dp, shadowElevation = 2.dp) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(176.dp), contentAlignment = Alignment.Center) {
                    CategoryDonutChart(segments = segments, modifier = Modifier.fillMaxSize(), centerContent = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AnimatedAmountText(uiState.totalSpentMinor, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            Text("spent this month", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                        }
                    })
                }
                Spacer(Modifier.width(18.dp))
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SummaryMetric(Icons.Default.Wallet, "Budget", uiState.overallLimitMinor?.formatAsCurrency() ?: "Not set", MaterialTheme.colorScheme.primary)
                    SummaryMetric(Icons.Default.ArrowDownward, "Spent", uiState.totalSpentMinor.formatAsCurrency(), MaterialTheme.colorScheme.error)
                    SummaryMetric(Icons.Default.ArrowUpward, "Received", uiState.totalIncomeMinor.formatAsCurrency(), extended.safe)
                }
            }
            Spacer(Modifier.height(10.dp))
            Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onRemainingClick), shape = RoundedCornerShape(24.dp), color = extended.safeContainer) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(44.dp), CircleShape, color = extended.safeContainerStrong) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Wallet, null, tint = extended.safe, modifier = Modifier.size(24.dp)) } }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) { Text("Remaining", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold); Text(remaining?.formatAsCurrency() ?: "Not set", style = MaterialTheme.typography.headlineSmall, color = remainingColor, fontWeight = FontWeight.Bold) }
                    if (uiState.overallLimitMinor != null) Text("${((1f - progress).coerceIn(0f, 1f) * 100).toInt()}% left", style = MaterialTheme.typography.titleSmall, color = extended.safe, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)), trackColor = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

@Composable
private fun SummaryMetric(icon: ImageVector, label: String, value: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(Modifier.size(44.dp), CircleShape, color = tint.copy(alpha = .1f)) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp)) } }
        Spacer(Modifier.width(12.dp))
        Column { Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold); Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }
    }
}
