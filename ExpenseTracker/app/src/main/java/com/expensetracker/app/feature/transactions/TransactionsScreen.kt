package com.expensetracker.app.feature.transactions

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.expensetracker.app.data.model.Expense
import com.expensetracker.app.data.model.ExpenseCategory
import kotlinx.coroutines.launch
import java.time.LocalDate
import org.koin.androidx.compose.koinViewModel

@Composable
fun TransactionsScreen(onEditExpenseClick: (Long) -> Unit, viewModel: TransactionsViewModel = koinViewModel()) {
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
    Scaffold(modifier = Modifier.fillMaxSize(), contentWindowInsets = WindowInsets(0, 0, 0, 0), snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { innerPadding ->
        TransactionsContent(uiState, viewModel, onEditExpenseClick, Modifier.fillMaxSize().padding(innerPadding))
    }
}

@Composable
private fun TransactionsContent(uiState: TransactionsUiState, actions: TransactionsActions, onEditExpenseClick: (Long) -> Unit, modifier: Modifier = Modifier) {
    var collapsedDates by remember { mutableStateOf<Set<LocalDate>>(emptySet()) }
    var filterExpanded by remember { mutableStateOf(true) }
    var selectedExpense by remember { mutableStateOf<Expense?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(uiState.expensesByDate) { if (collapsedDates.isEmpty() && uiState.expensesByDate.size > 1) collapsedDates = uiState.expensesByDate.drop(1).map { it.date }.toSet() }
    Column(modifier.fillMaxSize()) {
        SearchAndFilterBar(uiState.searchQuery, actions::onSearchQueryChange, filterExpanded) { filterExpanded = !filterExpanded }
        if (filterExpanded) CategoryFilterRow(uiState.selectedCategoryFilter, actions::onFilterChange)
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(strokeWidth = 2.dp) }
            uiState.isEmpty -> {
                val hasFilter = uiState.selectedCategoryFilter != null || uiState.searchQuery.isNotBlank()
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { EmptyState(Icons.Filled.ReceiptLong, if (hasFilter) "No matching transactions" else "No transactions yet", if (hasFilter) "Try a different search or category filter." else "Add an expense from the Home tab to see it here.") }
            }
            else -> LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                uiState.expensesByDate.forEach { group ->
                    val collapsed = group.date in collapsedDates
                    item(key = "header-${group.date}") { DateGroupHeader(group.date, group.totalMinor, collapsed) { collapsedDates = if (collapsed) collapsedDates - group.date else collapsedDates + group.date } }
                    if (!collapsed) items(group.expenses, key = { it.id }) { expense ->
                        TransactionCard(
                            expense = expense,
                            onDelete = actions::onDeleteExpense,
                            onClick = { onEditExpenseClick(expense.id) },
                            onLongClick = { selectedExpense = expense },
                            onDeleteRequest = { selectedExpense = expense; showDeleteConfirmation = true },
                        )
                    }
                }
            }
        }
    }
    if (selectedExpense != null && !showDeleteConfirmation) {
        selectedExpense?.let { expense ->
            TransactionActionDialog(
                expense = expense,
                onEdit = { selectedExpense = null; onEditExpenseClick(expense.id) },
                onDelete = { showDeleteConfirmation = true },
                onDismiss = { selectedExpense = null },
            )
        }
    }
    if (showDeleteConfirmation) {
        DeleteTransactionConfirmation(
            expense = selectedExpense,
            onConfirm = {
                val expense = selectedExpense ?: return@DeleteTransactionConfirmation
                scope.launch {
                    if (actions.onDeleteExpense(expense)) {
                        showDeleteConfirmation = false
                        selectedExpense = null
                    }
                }
            },
            onDismiss = {
                showDeleteConfirmation = false
                selectedExpense = null
            },
        )
    }
}

@Composable
private fun DeleteTransactionConfirmation(expense: Expense?, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    if (expense == null) return
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete transaction?") },
        text = { Text("Are you sure you want to delete \"${expense.note.ifBlank { if (expense.isIncome) "Income" else expense.category.displayName }}\"? You can undo the deletion afterward.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun TransactionActionDialog(expense: Expense, onEdit: () -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = expense.category.color().copy(alpha = 0.14f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(expense.category.icon(), null, tint = expense.category.color(), modifier = Modifier.size(26.dp))
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Transaction options", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(if (expense.isIncome) "Income" else "Expense", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            expense.note.ifBlank { if (expense.isIncome) "Income" else expense.category.displayName },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(expense.category.displayName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("  •  ${expense.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            (if (expense.isIncome) "+" else "-") + expense.amountMinor.formatAsCurrency(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (expense.isIncome) LocalExtendedColors.current.safe else MaterialTheme.colorScheme.error,
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("Edit", Modifier.padding(start = 8.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Surface(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                    ) {
                        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.DeleteOutline, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                            Text("Delete", Modifier.padding(start = 8.dp), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun SearchAndFilterBar(query: String, onQueryChange: (String) -> Unit, filterExpanded: Boolean, onFilterClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.weight(1f).height(56.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                Box(Modifier.weight(1f).padding(start = 12.dp)) {
                    if (query.isEmpty()) Text("Search transactions, notes or amount...", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    BasicTextField(query, onQueryChange, singleLine = true, textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface), modifier = Modifier.fillMaxWidth())
                }
            }
        }
        Surface(
            onClick = onFilterClick,
            modifier = Modifier.height(56.dp).width(118.dp),
            shape = RoundedCornerShape(18.dp),
            color = if (filterExpanded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.FilterList, null, Modifier.size(22.dp)); Text("Filter", Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun CategoryFilterRow(selected: ExpenseCategory?, onFilterChange: (ExpenseCategory?) -> Unit) {
    LazyRow(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { CategoryChip("All", selected == null, if (selected == null) Icons.Default.Check else null, MaterialTheme.colorScheme.primary) { onFilterChange(null) } }
        items(ExpenseCategory.entries, key = { it.name }) { category -> CategoryChip(category.displayName, selected == category, category.icon(), category.color()) { onFilterChange(category) } }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector?, tint: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(horizontal = 15.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) { icon?.let { Icon(it, null, tint = tint, modifier = Modifier.size(20.dp)) }; if (icon != null) Box(Modifier.width(7.dp)); Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium) }
    }
}

@Composable
private fun DateGroupHeader(date: LocalDate, totalMinor: Long, collapsed: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(date.toRelativeOrFormatted(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            AnimatedAmountText(totalMinor, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Icon(if (collapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess, if (collapsed) "Expand" else "Collapse", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 10.dp).size(22.dp))
        }
    }
}

@Composable
private fun TransactionCard(expense: Expense, onDelete: suspend (Expense) -> Boolean, onClick: () -> Unit, onLongClick: () -> Unit, onDeleteRequest: (Expense) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { SwipeToDeleteExpenseItem(expense, onDelete, onClick = onClick, onLongClick = onLongClick, onDeleteRequest = onDeleteRequest) }
            IconButton(onClick = onClick, Modifier.padding(end = 6.dp)) { Icon(Icons.Default.ChevronRight, "Open transaction", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
