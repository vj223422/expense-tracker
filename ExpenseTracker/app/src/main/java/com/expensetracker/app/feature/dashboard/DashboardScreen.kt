package com.expensetracker.app.feature.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
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
import com.expensetracker.app.core.designsystem.icon
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
fun DashboardScreen(
    onAddExpenseClick: () -> Unit,
    onAddIncomeClick: () -> Unit,
    onEditExpenseClick: (Long) -> Unit,
    onSeeAllTransactionsClick: () -> Unit,
    onFuelClick: () -> Unit = {},
    onRemindersClick: () -> Unit = {},
    onInsightsClick: () -> Unit = onSeeAllTransactionsClick,
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
                    val result = snackbarHostState.showSnackbar("Deleted \"$label\"", "Undo", duration = SnackbarDuration.Short)
                    if (result == SnackbarResult.ActionPerformed) viewModel.onUndoDelete(effect.expense)
                }
                is DashboardEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
                is DashboardEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFF7F8FF),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            androidx.compose.material3.FloatingActionButton(
                onClick = onAddExpenseClick,
                containerColor = Color(0xFF2852B8),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(66.dp),
            ) { Icon(Icons.Default.Add, "Add expense", modifier = Modifier.size(32.dp)) }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(Color(0xFFF7F8FF)).padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { HomeHeader(onSeeAllTransactionsClick, onRemindersClick) }
            item { GreetingBlock() }
            item { MonthCalendar(uiState.yearMonth, uiState.selectedDate, uiState.daysWithTransactions, viewModel::onPreviousMonth, viewModel::onNextMonth, viewModel::onSelectDate) }
            item { SpendingOverviewCard(uiState, viewModel::onRemainingClick) }
            item { QuickActions(onAddExpenseClick, onFuelClick, onRemindersClick, onInsightsClick) }
            item { CategoryOverview(uiState, onInsightsClick) }
            item { SectionTitle("Recent Transactions", "See All", onSeeAllTransactionsClick) }
            if (uiState.recentExpenses.isEmpty() && !uiState.isLoading) {
                item { EmptyState(icon = Icons.Outlined.ReceiptLong, title = "No transactions yet", message = "Your recent expenses and income will appear here.") }
            } else {
                items(uiState.recentExpenses.take(5), key = { it.id }) { expense ->
                    SwipeToDeleteExpenseItem(expense = expense, onDelete = viewModel::onDeleteExpense, onClick = { onEditExpenseClick(expense.id) })
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onAddIncomeClick, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.ArrowDownward, null); Spacer(Modifier.width(6.dp)); Text("Add income")
                    }
                    OutlinedButton(onClick = onSeeAllTransactionsClick, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.ReceiptLong, null); Spacer(Modifier.width(6.dp)); Text("Transactions")
                    }
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

private val RefBlue = Color(0xFF2852B8)
private val RefPaleBlue = Color(0xFFE8EEFF)
private val RefCard = Color(0xFFFCFCFF)

@Composable
private fun HomeHeader(onSearch: () -> Unit, onNotifications: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(13.dp), color = RefBlue, modifier = Modifier.size(46.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.BarChart, null, tint = Color.White, modifier = Modifier.size(29.dp)) }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("FinTrack", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color(0xFF12182A))
            Text("Expense Tracker", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4E5668))
        }
        IconButton(onClick = onSearch) { Icon(Icons.Default.Search, "Search", tint = Color(0xFF151B2A), modifier = Modifier.size(26.dp)) }
        IconButton(onClick = onNotifications) { Icon(Icons.Outlined.NotificationsNone, "Notifications", tint = Color(0xFF151B2A), modifier = Modifier.size(26.dp)) }
        Surface(onClick = onNotifications, shape = CircleShape, color = RefPaleBlue, modifier = Modifier.size(42.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, "Profile", tint = RefBlue, modifier = Modifier.size(24.dp)) }
        }
    }
}

@Composable
private fun GreetingBlock() {
    Column(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("Good Afternoon, Vijay 👋", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Color(0xFF111827))
        Text("Here's your spending overview", style = MaterialTheme.typography.bodyLarge, color = Color(0xFF4D5669))
    }
}

@Composable
private fun MonthCalendar(month: YearMonth, selectedDate: LocalDate, transactionDates: Set<LocalDate>, onPrevious: () -> Unit, onNext: () -> Unit, onSelect: (LocalDate) -> Unit) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedDate.dayOfMonth - 1)
    LaunchedEffect(month) { listState.animateScrollToItem(selectedDate.dayOfMonth - 1) }
    Surface(shape = RoundedCornerShape(25.dp), color = RefCard, shadowElevation = 2.dp) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onPrevious) { Icon(Icons.Default.ChevronLeft, "Previous month") }
                Surface(shape = CircleShape, color = RefPaleBlue) {
                    Row(Modifier.padding(horizontal = 18.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CalendarMonth, null, tint = RefBlue)
                        Spacer(Modifier.width(8.dp))
                        Text(month.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())), fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ExpandMore, null)
                    }
                }
                IconButton(onClick = onNext) { Icon(Icons.Default.ChevronRight, "Next month") }
            }
            LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
                items(month.lengthOfMonth(), key = { month.atDay(it + 1).toEpochDay() }) { index ->
                    val date = month.atDay(index + 1)
                    val selected = date == selectedDate
                    Surface(onClick = { onSelect(date) }, shape = RoundedCornerShape(18.dp), color = if (selected) RefBlue else Color(0xFFF7F8FD), shadowElevation = 1.dp, modifier = Modifier.width(61.dp).height(86.dp)) {
                        Column(Modifier.fillMaxSize().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()), style = MaterialTheme.typography.labelMedium, color = if (selected) Color.White else Color(0xFF343B4B))
                            Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (selected) Color.White else Color(0xFF111827))
                            Box(Modifier.size(7.dp).clip(CircleShape).background(if (selected) Color.White else if (date in transactionDates) Color(0xFF27833F) else Color.Transparent))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpendingOverviewCard(state: DashboardUiState, onBudgetClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(27.dp), color = Color(0xFFEAF0FF), shadowElevation = 1.dp) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("This Month  ⌄", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF17213A))
                    Text(state.totalSpentMinor.formatAsCurrency(), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = Color(0xFF111827))
                    Text(if (state.effectiveBudgetMinor != null) "${(state.overallProgress.coerceIn(0f, 1f) * 100).toInt()}% of budget used" else "Set a monthly budget", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4E5668))
                }
                SpendingSparkline(Modifier.width(125.dp).height(75.dp))
            }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp)).background(Color(0xFFF9FAFF)).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                SummaryColumn("Income", state.totalIncomeMinor, Icons.Default.ArrowDownward, Color(0xFF27833F), Modifier.weight(1f))
                Box(Modifier.width(1.dp).height(42.dp).background(Color(0xFFB9C2D6)))
                SummaryColumn("Expense", state.totalSpentMinor, Icons.Default.ArrowUpward, Color(0xFFD94B55), Modifier.weight(1f))
                Box(Modifier.width(1.dp).height(42.dp).background(Color(0xFFB9C2D6)))
                Column(Modifier.weight(1f).clickable(onClick = onBudgetClick), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.TrackChanges, null, tint = RefBlue)
                    Text("Budget", style = MaterialTheme.typography.labelMedium)
                    Text(state.effectiveBudgetMinor?.formatAsCurrency() ?: "Set budget", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun SpendingSparkline(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val pts = listOf(
            androidx.compose.ui.geometry.Offset(0f, size.height * .78f),
            androidx.compose.ui.geometry.Offset(size.width * .18f, size.height * .48f),
            androidx.compose.ui.geometry.Offset(size.width * .36f, size.height * .55f),
            androidx.compose.ui.geometry.Offset(size.width * .54f, size.height * .30f),
            androidx.compose.ui.geometry.Offset(size.width * .72f, size.height * .38f),
            androidx.compose.ui.geometry.Offset(size.width * .88f, size.height * .12f),
            androidx.compose.ui.geometry.Offset(size.width, size.height * .08f),
        )
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(pts.first().x, pts.first().y)
            for (i in 1 until pts.size) {
                val p = pts[i]; val q = pts[i - 1]; val mid = (p.x + q.x) / 2f
                cubicTo(mid, q.y, mid, p.y, p.x, p.y)
            }
        }
        drawPath(path, RefBlue, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(RefBlue, 5.dp.toPx(), pts.last())
        drawCircle(Color.White, 2.5.dp.toPx(), pts.last())
    }
}

@Composable
private fun SummaryColumn(label: String, amount: Long, icon: ImageVector, tint: Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Surface(shape = CircleShape, color = tint.copy(alpha = .13f)) { Icon(icon, null, tint = tint, modifier = Modifier.padding(6.dp).size(20.dp)) }
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color(0xFF30384A))
        Text(amount.formatAsCurrency(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF111827), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun QuickActions(onAddExpense: () -> Unit, onFuel: () -> Unit, onReminders: () -> Unit, onInsights: () -> Unit) {
    val actions = listOf(
        Triple("Add Expense", Icons.Default.Add, onAddExpense),
        Triple("Fuel Tracker", Icons.Default.LocalGasStation, onFuel),
        Triple("Water Reminder", Icons.Outlined.WaterDrop, onReminders),
        Triple("Reminders", Icons.Outlined.NotificationsNone, onReminders),
        Triple("Reports", Icons.Default.BarChart, onInsights),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        actions.forEachIndexed { index, item ->
            Surface(onClick = item.third, modifier = Modifier.weight(1f).height(104.dp), shape = RoundedCornerShape(19.dp), color = if (index == 0) Color(0xFFDCE7FF) else Color(0xFFEFF1FC)) {
                Column(Modifier.fillMaxSize().padding(horizontal = 2.dp, vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Icon(item.second, null, tint = RefBlue, modifier = Modifier.size(27.dp))
                    Text(item.first, style = MaterialTheme.typography.labelMedium, color = Color(0xFF161D2D), textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun CategoryOverview(state: DashboardUiState, onSeeAll: () -> Unit) {
    Surface(shape = RoundedCornerShape(25.dp), color = RefCard, shadowElevation = 2.dp) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            SectionTitle("Category Overview", "See All", onSeeAll)
            val top = state.categorySpends.take(5)
            if (top.isEmpty()) {
                Text("Your category breakdown will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(.9f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        CategoryDonutChart(
                            segments = top.map { DonutSegment(it.spentMinor.toFloat(), it.category.color(), it.category.displayName) },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 18.dp,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(state.totalSpentMinor.formatAsCurrency(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                                Text("Spent", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1.1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        top.forEach { spend ->
                            val percent = if (state.totalSpentMinor > 0L) spend.spentMinor * 100 / state.totalSpentMinor else 0L
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(9.dp), color = spend.category.color().copy(alpha = .13f)) {
                                    Icon(spend.category.icon(), null, tint = spend.category.color(), modifier = Modifier.padding(6.dp).size(17.dp))
                                }
                                Spacer(Modifier.width(6.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(spend.category.displayName, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(spend.spentMinor.formatAsCurrency(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                                Text("${percent}%", style = MaterialTheme.typography.labelMedium, color = Color(0xFF4E5668))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardGreeting() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Good evening", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Track  •  Split  •  Save", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(46.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = "Calendar", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun MonthSelector(yearMonth: YearMonth, onPreviousMonth: () -> Unit, onNextMonth: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp)),
    ) {
        Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onPreviousMonth) { Icon(Icons.Default.ChevronLeft, "Previous month") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CalendarMonth, null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(9.dp))
                Text(yearMonth.toDisplayString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    val month = YearMonth.from(selectedDate)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedDate.dayOfMonth - 1,
    )

    // Preserve the scroll position while browsing; only reposition when month changes.
    LaunchedEffect(month) {
        listState.animateScrollToItem(selectedDate.dayOfMonth - 1)
    }


    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            count = month.lengthOfMonth(),
            key = { index -> month.atDay(index + 1).toEpochDay() },
        ) { index ->
            val date = month.atDay(index + 1)
            val selected = date == selectedDate
            Column(
                Modifier
                    .width(46.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow)
                    .clickable { onSelectDate(date) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                )
                Box(
                    Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(
                            if (date in daysWithTransactions) {
                                if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.tertiary
                            } else {
                                Color.Transparent
                            },
                        ),
                )
            }
        }
    }
}

@Composable
private fun DailyExpenseCard(date: LocalDate, spentMinor: Long) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
    ) {
        Row(
            Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CalendarMonth, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault())), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(spentMinor.formatAsCurrency(), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("Total expense", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ReceiptLong, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun LifetimeSummaryCard(uiState: DashboardUiState, onRemainingClick: () -> Unit) {
    val remaining = uiState.remainingMinor
    val budget = uiState.effectiveBudgetMinor
    val progress = uiState.overallProgress.coerceIn(0f, 1f)
    val progressPercent = (progress * 100).toInt()

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClick = onRemainingClick)
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(58.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Wallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(29.dp),
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        "Remaining purse",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (remaining != null) {
                        AnimatedAmountText(
                            amountMinor = remaining,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (remaining >= 0L) LocalExtendedColors.current.safe else MaterialTheme.colorScheme.error,
                            ),
                        )
                    } else {
                        Text(
                            "Set monthly budget",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Wallet,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                        }
                        Text(
                            "Budget",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        budget?.formatAsCurrency() ?: "No limit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Box(
                    Modifier
                        .width(1.dp)
                        .height(62.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                        }
                        Text(
                            "Expense",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    AnimatedAmountText(
                        amountMinor = uiState.totalSpentMinor,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        uiState.yearMonth.toDisplayString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (budget != null) "$progressPercent% used" else "No budget set",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
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
        TransactionActionButton("Add expense", Icons.Default.ArrowDownward, onAddExpense, Modifier.weight(1f), MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        TransactionActionButton("Add income", Icons.Default.ArrowUpward, onAddIncome, Modifier.weight(1f), MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
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
private fun CategoryCard(category: ExpenseCategory, amountMinor: Long, totalSpentMinor: Long) { val categoryColor = category.color(); val share = if (totalSpentMinor > 0) (amountMinor.toDouble() / totalSpentMinor).coerceIn(0.0, 1.0) else 0.0; Surface(modifier = Modifier.width(138.dp), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) { Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { Surface(Modifier.size(36.dp), shape = CircleShape, color = categoryColor.copy(alpha = .12f)) { Box(contentAlignment = Alignment.Center) { Text(category.displayName.take(1), color = categoryColor, fontWeight = FontWeight.Bold) } }; Text(category.displayName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(amountMinor.formatAsCurrency(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold); Text("${(share * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); LinearProgressIndicator(progress = { share.toFloat() }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape), color = categoryColor, trackColor = categoryColor.copy(alpha = .12f)) } } }
