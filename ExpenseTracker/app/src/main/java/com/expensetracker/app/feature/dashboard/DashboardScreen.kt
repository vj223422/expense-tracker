package com.expensetracker.app.feature.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardScreen(onAddExpenseClick: () -> Unit, onAddIncomeClick: () -> Unit, onEditExpenseClick: (Long) -> Unit, onSeeAllTransactionsClick: () -> Unit, viewModel: DashboardViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showBudgetEditor by viewModel.showBudgetEditor.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { viewModel.effects.collect { effect -> when (effect) {
        is DashboardEffect.ShowUndoDelete -> { val label = effect.expense.note.ifBlank { effect.expense.category.displayName }; val result = snackbarHostState.showSnackbar(message = "Deleted \"$label\"", actionLabel = "Undo", duration = SnackbarDuration.Short); if (result == SnackbarResult.ActionPerformed) viewModel.onUndoDelete(effect.expense) }
        is DashboardEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message, duration = SnackbarDuration.Short)
        is DashboardEffect.ShowError -> snackbarHostState.showSnackbar(effect.message, duration = SnackbarDuration.Short)
    } } }
    Scaffold(modifier = Modifier.fillMaxSize(), contentWindowInsets = WindowInsets(0, 0, 0, 0), snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(Color(0xFFFAF6EF)).padding(innerPadding),
            contentPadding = PaddingValues(start = 18.dp, top = 10.dp, end = 18.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item { DashboardGreeting() }
            item { MonthSelector(uiState.yearMonth, viewModel::onPreviousMonth, viewModel::onNextMonth) }
            item { CalendarWeekStrip(uiState.selectedDate, uiState.daysWithTransactions, viewModel::onSelectDate) }
            item { DailyExpenseCard(uiState.selectedDate, uiState.selectedDaySpentMinor) }
            item { LifetimeSummaryCard(uiState, onRemainingClick = viewModel::onRemainingClick) }
            item { TransactionActionButtons(onAddExpenseClick, onAddIncomeClick) }
            val topCategories = uiState.categorySpends.take(5)
            if (topCategories.isNotEmpty()) {
                item { SectionTitle("Spending categories", "See all", onSeeAllTransactionsClick) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
                        items(topCategories, key = { it.category.name }) { spend ->
                            CategoryCard(spend.category, spend.spentMinor, uiState.totalSpentMinor)
                        }
                    }
                }
            }
            item { SectionTitle("Transactions · " + uiState.selectedDate.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())), "View all", onSeeAllTransactionsClick) }
            if (uiState.selectedDayExpenses.isEmpty() && !uiState.isLoading) {
                item { EmptyState(icon = Icons.Outlined.ReceiptLong, title = "No transactions for this day", message = "Choose another date or add an expense or income.") }
            } else {
                items(uiState.selectedDayExpenses, key = { it.id }) { expense ->
                    SwipeToDeleteExpenseItem(expense = expense, onDelete = viewModel::onDeleteExpense, onClick = { onEditExpenseClick(expense.id) })
                }
            }
        }
    }
    if (showBudgetEditor) BudgetEditorDialog(
        currentEffectiveBudgetMinor = uiState.effectiveBudgetMinor,
        baseBudgetMinor = uiState.overallLimitMinor,
        incomeMinor = uiState.totalIncomeMinor,
        carryForwardMinor = uiState.carryForwardMinor,
        onSave = viewModel::onSaveBudget,
        onClear = viewModel::onClearBudget,
        onDismiss = viewModel::onDismissBudgetEditor,
    )
}

@Composable
private fun DashboardGreeting() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Good evening", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Track  •  Split  •  Save", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(
            shape = CircleShape,
            color = Color(0xFFFFF0CF),
            modifier = Modifier.size(46.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = "Calendar", tint = Color(0xFF9A7439), modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun MonthSelector(yearMonth: YearMonth, onPreviousMonth: () -> Unit, onNextMonth: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF2EDE4),
        modifier = Modifier.border(1.dp, Color(0xFFE7DED0), RoundedCornerShape(18.dp)),
    ) {
        Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onPreviousMonth) { Icon(Icons.Default.ChevronLeft, "Previous month") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CalendarMonth, null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(9.dp))
                Text(yearMonth.toDisplayString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onNextMonth) { Icon(Icons.Default.ChevronRight, "Next month") }
        }
    }
}

@Composable
private fun CalendarWeekStrip(
    selectedDate: LocalDate,
    daysWithTransactions: Set<LocalDate>,
    onSelectDate: (LocalDate) -> Unit,
) {
    val weekStart = selectedDate.minusDays((selectedDate.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
    val days = (0L..6L).map { weekStart.plusDays(it) }
    Row(
        Modifier.fillMaxWidth().pointerInput(selectedDate) {
            var dragDistance = 0f
            detectHorizontalDragGestures(
                onHorizontalDrag = { _, delta -> dragDistance += delta },
                onDragEnd = {
                    when {
                        dragDistance < -48f -> onSelectDate(selectedDate.plusDays(7))
                        dragDistance > 48f -> onSelectDate(selectedDate.minusDays(7))
                    }
                    dragDistance = 0f
                },
                onDragCancel = { dragDistance = 0f },
            )
        },
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        days.forEach { date ->
            val selected = date == selectedDate
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(22.dp))
                    .background(if (selected) Color(0xFFF2C968) else Color(0xFFFFFDFA))
                    .clickable { onSelectDate(date) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()), style = MaterialTheme.typography.labelSmall, color = if (selected) Color(0xFF4A3210) else MaterialTheme.colorScheme.onSurfaceVariant)
                Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium, color = if (selected) Color(0xFF39264F) else MaterialTheme.colorScheme.onSurface)
                Box(Modifier.size(5.dp).clip(CircleShape).background(if (date in daysWithTransactions) { if (selected) Color(0xFF39264F) else Color(0xFFFFA62B) } else Color.Transparent))
            }
        }
    }
}

@Composable
private fun DailyExpenseCard(date: LocalDate, spentMinor: Long) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = Color.Transparent,
        shadowElevation = 2.dp,
    ) {
        Row(
            Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFFFFF0D1), Color(0xFFF7E5BE))))
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CalendarMonth, null, tint = Color(0xFF6E5735), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault())), style = MaterialTheme.typography.titleSmall, color = Color(0xFF715B3A), fontWeight = FontWeight.SemiBold)
                }
                Text(spentMinor.formatAsCurrency(), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = Color(0xFF30291F))
                Text("Total expense", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF715B3A))
            }
            Surface(shape = CircleShape, color = Color.White.copy(alpha = .55f), modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ReceiptLong, null, tint = Color(0xFF70451D), modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun LifetimeSummaryCard(uiState: DashboardUiState, onRemainingClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFFFFDFA),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(17.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = Color(0xFFF4E9D4), modifier = Modifier.size(30.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ReceiptLong, null, tint = Color(0xFF9A7439), modifier = Modifier.size(17.dp))
                            }
                        }
                        Spacer(Modifier.width(7.dp))
                        Text("Till date", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    AnimatedAmountText(
                        amountMinor = uiState.allTimeSpentMinor,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    )
                    Text("All recorded expenses", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(Modifier.width(1.dp).height(104.dp).background(Color(0xFFE9E0D3)))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = Color(0xFFFFF0CF), modifier = Modifier.size(30.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.CalendarMonth, null, tint = Color(0xFFB18128), modifier = Modifier.size(17.dp))
                            }
                        }
                        Spacer(Modifier.width(7.dp))
                        Text("This month", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    AnimatedAmountText(
                        amountMinor = uiState.totalSpentMinor,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    )
                    Text(uiState.yearMonth.toDisplayString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LinearProgressIndicator(
                        progress = { uiState.overallProgress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = Color(0xFFE5B94F),
                        trackColor = Color(0xFFF2EBDD),
                    )
                }
            }
            Spacer(Modifier.height(15.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE9E0D3)))
            Spacer(Modifier.height(13.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LifetimeMetric("Income", uiState.allTimeIncomeMinor, Color(0xFF65A66C), Modifier.weight(1f))
                LifetimeMetric("Budget", uiState.effectiveBudgetMinor ?: 0L, Color(0xFFC18A35), Modifier.weight(1f), onClick = onRemainingClick, showSetWhenZero = uiState.effectiveBudgetMinor == null)
            }
        }
    }
}

@Composable
private fun LifetimeMetric(
    label: String,
    amount: Long,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    showSetWhenZero: Boolean = false,
) {
    Row(
        modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Surface(shape = CircleShape, color = tint.copy(alpha = .13f), modifier = Modifier.size(34.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (label == "Income") Icons.Default.ArrowUpward else Icons.Default.Wallet,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(
                if (showSetWhenZero) "Set budget" else amount.formatAsCurrency(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BudgetEditorDialog(
    currentEffectiveBudgetMinor: Long?,
    baseBudgetMinor: Long?,
    incomeMinor: Long,
    carryForwardMinor: Long,
    onSave: (Long) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    var amountText by rememberSaveable(currentEffectiveBudgetMinor) {
        mutableStateOf(currentEffectiveBudgetMinor?.let { String.format(Locale.US, "%.2f", it / 100.0) } ?: "")
    }
    val parsedMinor = amountText.parseAmountToMinorUnits()
    val valid = parsedMinor != null && parsedMinor > 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Monthly budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "This is the amount currently available for the cycle. Received amounts and carry-forward are included.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Available budget") },
                    singleLine = true,
                    isError = amountText.isNotBlank() && !valid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Base budget: ${baseBudgetMinor?.formatAsCurrency() ?: "No limit"}", style = MaterialTheme.typography.labelMedium)
                    if (incomeMinor > 0L) Text("Received: ${incomeMinor.formatAsCurrency()}", style = MaterialTheme.typography.labelMedium, color = LocalExtendedColors.current.safe)
                    if (carryForwardMinor > 0L) Text("Carry forward: ${carryForwardMinor.formatAsCurrency()}", style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        confirmButton = { TextButton(onClick = { parsedMinor?.let(onSave) }, enabled = valid) { Text("Save") } },
        dismissButton = {
            Row {
                if (baseBudgetMinor != null) TextButton(onClick = onClear) { Text("Clear") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
@Composable
private fun SummaryMetric(label: String, amountMinor: Long?, icon: ImageVector, iconColor: Color) { Row(verticalAlignment = Alignment.CenterVertically) { Surface(Modifier.size(36.dp), shape = CircleShape, color = iconColor.copy(alpha = .10f)) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = iconColor, modifier = Modifier.size(19.dp)) } }; Spacer(Modifier.width(9.dp)); Column { Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(amountMinor?.formatAsCurrency() ?: "No limit", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) } } }

@Composable
private fun TransactionActionButtons(
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TransactionActionButton("Add expense", Icons.Default.ArrowDownward, onAddExpense, Modifier.weight(1f), Color(0xFFFFE4D8), Color(0xFF9D482C))
        TransactionActionButton("Add income", Icons.Default.ArrowUpward, onAddIncome, Modifier.weight(1f), Color(0xFFDDF2E8), Color(0xFF246A53))
    }
}

@Composable
private fun TransactionActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier,
    containerColor: Color,
    contentColor: Color,
) {
    Surface(modifier = modifier.height(58.dp).clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), color = containerColor, shadowElevation = 1.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Surface(shape = CircleShape, color = Color.White.copy(alpha = .65f), modifier = Modifier.size(34.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = contentColor, modifier = Modifier.size(19.dp)) }
            }
            Spacer(Modifier.width(9.dp))
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = contentColor)
        }
    }
}

@Composable
private fun SectionTitle(title: String, action: String, onClick: () -> Unit) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 4.dp)) { Text(action, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold) } } }

@Composable
private fun CategoryCard(category: ExpenseCategory, amountMinor: Long, totalSpentMinor: Long) { val categoryColor = category.color(); val share = if (totalSpentMinor > 0) (amountMinor.toDouble() / totalSpentMinor).coerceIn(0.0, 1.0) else 0.0; Surface(modifier = Modifier.width(138.dp), shape = RoundedCornerShape(20.dp), color = categoryColor.copy(alpha = .08f)) { Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { Surface(Modifier.size(36.dp), shape = CircleShape, color = categoryColor.copy(alpha = .12f)) { Box(contentAlignment = Alignment.Center) { Text(category.displayName.take(1), color = categoryColor, fontWeight = FontWeight.Bold) } }; Text(category.displayName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(amountMinor.formatAsCurrency(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold); Text("${(share * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); LinearProgressIndicator(progress = { share.toFloat() }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape), color = categoryColor, trackColor = categoryColor.copy(alpha = .12f)) } } }
