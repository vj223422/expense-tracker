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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
import com.expensetracker.app.core.util.toDisplayString
import com.expensetracker.app.data.model.Expense
import com.expensetracker.app.data.model.ExpenseCategory
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

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DashboardEffect.ShowUndoDelete -> {
                    val label = effect.expense.note.ifBlank { effect.expense.category.displayName }
                    val result = snackbarHostState.showSnackbar("Deleted \"$label\"", "Undo", SnackbarDuration.Short)
                    if (result == SnackbarResult.ActionPerformed) viewModel.onUndoDelete(effect.expense)
                }
                is DashboardEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message, duration = SnackbarDuration.Short)
                is DashboardEffect.ShowError -> snackbarHostState.showSnackbar(effect.message, duration = SnackbarDuration.Short)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item { MonthSelector(uiState.yearMonth) }
            item { SummaryCard(uiState) }
            item { AddExpenseButton(onAddExpenseClick) }

            val topCategories = uiState.categorySpends.take(5)
            if (topCategories.isNotEmpty()) {
                item { SectionTitle("Top categories", "See all", onSeeAllTransactionsClick) }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp),
                    ) {
                        items(topCategories, key = { it.category.name }) { spend ->
                            CategoryCard(spend.category, spend.spentMinor, uiState.totalSpentMinor)
                        }
                    }
                }
            }

            item { SectionTitle("Recent transactions", "See all", onSeeAllTransactionsClick) }
            if (uiState.recentExpenses.isEmpty() && !uiState.isLoading) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.ReceiptLong,
                        title = "No transactions yet",
                        message = "Your latest expenses and received amounts will appear here.",
                    )
                }
            } else {
                items(uiState.recentExpenses, key = { it.id }) { expense ->
                    SwipeToDeleteExpenseItem(
                        expense = expense,
                        onDelete = viewModel::onDeleteExpense,
                        onClick = { onEditExpenseClick(expense.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthSelector(yearMonth: java.time.YearMonth) {
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(
            Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(Icons.Default.ChevronLeft, "Previous month", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CalendarMonth, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(9.dp))
                Text(yearMonth.toDisplayString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Icon(Icons.Default.ChevronRight, "Next month", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SummaryCard(uiState: DashboardUiState) {
    val extended = LocalExtendedColors.current
    val reduceMotion = LocalReducedMotion.current
    val progress = uiState.overallProgress
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(if (reduceMotion) 0 else MotionDurations.MEDIUM, easing = MotionEasing.Standard),
        label = "dashboardBudgetProgress",
    )
    val remaining = uiState.remainingMinor
    val remainingColor = when {
        remaining == null -> MaterialTheme.colorScheme.onSurface
        remaining < 0L -> MaterialTheme.colorScheme.error
        progress >= .8f -> extended.warning
        else -> extended.safe
    }
    val segments = uiState.categorySpends.map { DonutSegment(it.spentMinor.toFloat(), it.category.color(), it.category.displayName) }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(176.dp), contentAlignment = Alignment.Center) {
                    CategoryDonutChart(
                        segments = segments,
                        modifier = Modifier.fillMaxSize(),
                        centerContent = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                AnimatedAmountText(
                                    amountMinor = uiState.totalSpentMinor,
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                )
                                Text("spent this month", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryMetric("Budget", uiState.overallLimitMinor, Icons.Default.Wallet, MaterialTheme.colorScheme.primary)
                    SummaryMetric("Spent", uiState.totalSpentMinor, Icons.Default.ArrowDownward, MaterialTheme.colorScheme.error)
                    SummaryMetric("Received", uiState.totalIncomeMinor, Icons.Default.ArrowUpward, extended.safe)
                }
            }
            Spacer(Modifier.height(14.dp))
            Surface(shape = RoundedCornerShape(20.dp), color = extended.safe.copy(alpha = .10f)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(40.dp), shape = CircleShape, color = extended.safe.copy(alpha = .12f)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Wallet, null, tint = extended.safe, modifier = Modifier.size(21.dp)) }
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Remaining", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(remaining?.formatAsCurrency() ?: "No limit set", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = remainingColor)
                    }
                    if (remaining != null && uiState.overallLimitMinor != null && uiState.overallLimitMinor > 0) {
                        val left = ((remaining.toDouble() / uiState.overallLimitMinor.toDouble()) * 100).toInt().coerceIn(0, 100)
                        Text("$left% left", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = extended.safe)
                    }
                }
            }
            if (uiState.overallLimitMinor != null) {
                Spacer(Modifier.height(9.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(label: String, amountMinor: Long?, icon: ImageVector, iconColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(Modifier.size(36.dp), shape = CircleShape, color = iconColor.copy(alpha = .10f)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = iconColor, modifier = Modifier.size(19.dp)) }
        }
        Spacer(Modifier.width(9.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(amountMinor?.formatAsCurrency() ?: "No limit", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AddExpenseButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(50.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Surface(Modifier.size(29.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                Box(contentAlignment = Alignment.Center) {
                    Text("+", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(9.dp))
            Text("Add Expense", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SectionTitle(title: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 4.dp)) {
            Text(action, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CategoryCard(category: ExpenseCategory, amountMinor: Long, totalSpentMinor: Long) {
    val categoryColor = category.color()
    val share = if (totalSpentMinor > 0) (amountMinor.toDouble() / totalSpentMinor).coerceIn(0.0, 1.0) else 0.0
    Surface(modifier = Modifier.width(138.dp), shape = RoundedCornerShape(20.dp), color = categoryColor.copy(alpha = .08f)) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Surface(Modifier.size(36.dp), shape = CircleShape, color = categoryColor.copy(alpha = .12f)) {
                Box(contentAlignment = Alignment.Center) { Text(category.displayName.take(1), color = categoryColor, fontWeight = FontWeight.Bold) }
            }
            Text(category.displayName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(amountMinor.formatAsCurrency(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("${(share * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinearProgressIndicator(
                progress = { share.toFloat() },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = categoryColor,
                trackColor = categoryColor.copy(alpha = .12f),
            )
        }
    }
}
