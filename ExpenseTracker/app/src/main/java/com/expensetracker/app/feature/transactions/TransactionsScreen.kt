package com.expensetracker.app.feature.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.core.designsystem.AnimatedAmountText
import com.expensetracker.app.core.designsystem.EmptyState
import com.expensetracker.app.core.designsystem.SwipeToDeleteExpenseItem
import com.expensetracker.app.core.designsystem.color
import com.expensetracker.app.core.designsystem.icon
import com.expensetracker.app.core.theme.LocalExtendedColors
import com.expensetracker.app.core.util.formatAsCurrency
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

    LaunchedEffect(Unit) {
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
    var collapsedDates by remember { mutableStateOf<Set<LocalDate>>(emptySet()) }
    var filterExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(uiState.expensesByDate) {
        if (collapsedDates.isEmpty() && uiState.expensesByDate.size > 1) {
            collapsedDates = uiState.expensesByDate.drop(1).map { it.date }.toSet()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        SearchAndFilterBar(
            query = uiState.searchQuery,
            onQueryChange = actions::onSearchQueryChange,
            filterExpanded = filterExpanded,
            onFilterClick = { filterExpanded = !filterExpanded },
        )
        if (filterExpanded) {
            CategoryFilterRow(
                selected = uiState.selectedCategoryFilter,
                onFilterChange = actions::onFilterChange,
            )
        }

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 2.dp)
            }
            uiState.isEmpty -> {
                val hasFilter = uiState.selectedCategoryFilter != null || uiState.searchQuery.isNotBlank()
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Filled.ReceiptLong,
                        title = if (hasFilter) "No matching transactions" else "No transactions yet",
                        message = if (hasFilter) "Try a different search or category filter." else "Add an expense from the Home tab to see it here.",
                    )
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                uiState.expensesByDate.forEach { group ->
                    val collapsed = group.date in collapsedDates
                    item(key = "header-${group.date}") {
                        DateGroupHeader(
                            date = group.date,
                            totalMinor = group.totalMinor,
                            collapsed = collapsed,
                            onClick = {
                                collapsedDates = if (collapsed) collapsedDates - group.date else collapsedDates + group.date
                            },
                        )
                    }
                    if (!collapsed) {
                        items(group.expenses, key = { it.id }) { expense ->
                            TransactionCard(expense = expense, onDelete = actions::onDeleteExpense, onClick = { onEditExpenseClick(expense.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchAndFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    filterExpanded: Boolean,
    onFilterClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.weight(1f).height(56.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                Box(Modifier.weight(1f).padding(start = 12.dp)) {
                    if (query.isEmpty()) Text("Search transactions, notes or amount...", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        Surface(
            modifier = Modifier.height(56.dp).width(118.dp),
            shape = RoundedCornerShape(18.dp),
            color = if (filterExpanded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
            onClick = onFilterClick,
        ) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FilterList, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
                Text("Filter", modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(selected: ExpenseCategory?, onFilterChange: (ExpenseCategory?) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "filter-all") {
            CategoryChip(
                label = "All",
                selected = selected == null,
                icon = if (selected == null) Icons.Default.Check else null,
                tint = MaterialTheme.colorScheme.primary,
                onClick = { onFilterChange(null) },
            )
        }
        items(ExpenseCategory.entries, key = { it.name }) { category ->
            CategoryChip(
                label = category.displayName,
                selected = selected == category,
                icon = category.icon(),
                tint = category.color(),
                onClick = { onFilterChange(category) },
            )
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector?, tint: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(horizontal = 15.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            icon?.let { Icon(it, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp)) }
            if (icon != null) Box(Modifier.width(7.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
        }
    }
}

@Composable
private fun DateGroupHeader(
    date: LocalDate,
    totalMinor: Long,
    collapsed: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(date.toRelativeOrFormatted(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            AnimatedAmountText(
                amountMinor = totalMinor,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Icon(if (collapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess, contentDescription = if (collapsed) "Expand" else "Collapse", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 10.dp).size(22.dp))
        }
    }
}

@Composable
private fun TransactionCard(
    expense: com.expensetracker.app.data.model.Expense,
    onDelete: suspend (com.expensetracker.app.data.model.Expense) -> Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                SwipeToDeleteExpenseItem(expense = expense, onDelete = onDelete, onClick = onClick)
            }
            IconButton(onClick = onClick, modifier = Modifier.padding(end = 6.dp)) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Open transaction", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
