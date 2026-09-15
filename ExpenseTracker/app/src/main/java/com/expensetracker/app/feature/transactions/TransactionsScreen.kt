package com.expensetracker.app.feature.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.expensetracker.app.core.designsystem.AnimatedAmountText
import com.expensetracker.app.core.designsystem.EmptyState
import com.expensetracker.app.core.designsystem.SwipeToDeleteExpenseItem
import com.expensetracker.app.core.designsystem.color
import com.expensetracker.app.core.designsystem.icon
import com.expensetracker.app.core.theme.LocalExtendedColors
import com.expensetracker.app.core.util.toRelativeOrFormatted
import com.expensetracker.app.data.model.ExpenseCategory
import java.time.LocalDate
import org.koin.androidx.compose.koinViewModel

@Composable
fun TransactionsScreen(
    onEditExpenseClick: (Long) -> Unit,
    viewModel: TransactionsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is TransactionsEffect.ShowUndoDelete -> {
                        val label = effect.expense.note.ifBlank { effect.expense.category.displayName }
                        val result = snackbarHostState.showSnackbar(
                            message = "Deleted \"$label\"",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short,
                        )
                        if (result == SnackbarResult.ActionPerformed) viewModel.onUndoDelete(effect.expense)
                    }
                    is TransactionsEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message, duration = SnackbarDuration.Short)
                    is TransactionsEffect.ShowError -> snackbarHostState.showSnackbar(effect.message, duration = SnackbarDuration.Short)
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        TransactionsContent(
            uiState = uiState,
            actions = viewModel,
            onEditExpenseClick = onEditExpenseClick,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        )
    }
}

@Composable
private fun TransactionsContent(
    uiState: TransactionsUiState,
    actions: TransactionsActions,
    onEditExpenseClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        CategoryFilterRow(
            selected = uiState.selectedCategoryFilter,
            onFilterChange = actions::onFilterChange,
            modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
        )

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 2.dp)
            }
            uiState.isEmpty -> {
                val hasFilter = uiState.selectedCategoryFilter != null
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Filled.ReceiptLong,
                        title = if (hasFilter) "No matching expenses" else "No transactions yet",
                        message = if (hasFilter) "Try a different category filter." else "Add an expense from the Home tab to see it here.",
                    )
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                uiState.expensesByDate.forEach { group ->
                    item(key = "header-${group.date}", contentType = "date_header") {
                        DateGroupHeader(
                            date = group.date,
                            totalMinor = group.totalMinor,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    items(group.expenses, key = { it.id }, contentType = { "expense_row" }) { expense ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        ) {
                            SwipeToDeleteExpenseItem(
                                expense = expense,
                                onDelete = actions::onDeleteExpense,
                                onClick = { onEditExpenseClick(expense.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(
    selected: ExpenseCategory?,
    onFilterChange: (ExpenseCategory?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "filter-all") {
            FilterChip(
                selected = selected == null,
                onClick = { onFilterChange(null) },
                label = { Text("All", style = MaterialTheme.typography.labelLarge) },
                leadingIcon = if (selected == null) {
                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                } else null,
                shape = RoundedCornerShape(14.dp),
                border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected == null),
            )
        }
        items(ExpenseCategory.entries, key = { it.name }) { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onFilterChange(category) },
                label = { Text(category.displayName, style = MaterialTheme.typography.labelLarge) },
                leadingIcon = {
                    Icon(
                        imageVector = category.icon(),
                        contentDescription = null,
                        tint = category.color(),
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                },
                shape = RoundedCornerShape(14.dp),
                border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected == category),
            )
        }
    }
}

@Composable
private fun DateGroupHeader(
    date: LocalDate,
    totalMinor: Long,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = date.toRelativeOrFormatted(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (date != LocalDate.now()) {
                Text(
                    text = date.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy")),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        AnimatedAmountText(
            amountMinor = totalMinor,
            style = MaterialTheme.typography.titleSmall,
            color = LocalExtendedColors.current.safe,
        )
    }
}
