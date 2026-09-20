package com.expensetracker.app.feature.fuel

import com.expensetracker.app.data.entity.FuelLogEntity
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

private val FuelBlue = Color(0xFF0878F9)
private val FuelGreen = Color(0xFF0FAE68)
private val FuelOrange = Color(0xFFF58B18)
private val FuelPurple = Color(0xFF7B6BE8)
private val FuelInk = Color(0xFF111A35)
private val FuelMuted = Color(0xFF536582)
private val FuelBorder = Color(0xFFE4EAF3)
private val FuelPage = Color(0xFFF8FAFD)
private val FuelHero = Color(0xFFFFF2F4)
private val FuelGreenSoft = Color(0xFFE1F7EB)
private val FuelBlueSoft = Color(0xFFE5F0FF)
private val FuelOrangeSoft = Color(0xFFFFEEDC)
private val FuelPurpleSoft = Color(0xFFEDEAFF)

@Composable
fun FuelTrackerScreen(
    onNavigateBack: () -> Unit,
    viewModel: FuelTrackerViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var selectedRange by rememberSaveable { mutableStateOf("3M") }

    val mileage = state.averageMileage ?: 0.0
    val totalDistance = state.logs.sumOf { log ->
        log.endOdometerKm?.let { (it - log.odometerKm).coerceAtLeast(0.0) } ?: 0.0
    }
    val completed = state.logs.filter { it.endOdometerKm != null }
    val trips = completed.mapNotNull { log ->
        val end = log.endOdometerKm ?: return@mapNotNull null
        val distance = (end - log.odometerKm).takeIf { it > 0.0 } ?: return@mapNotNull null
        val cost = log.amountMinor / 100.0
        FuelTripInsight(distance / log.liters, cost, cost / distance, distance)
    }.takeLast(8)

    Scaffold(
        containerColor = FuelPage,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FuelPage),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = FuelInk)
                    }
                },
                title = {
                    Column {
                        Text("Fuel & Mileage", color = FuelInk, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Track your fuel fills and get better insights", color = FuelMuted, style = MaterialTheme.typography.bodyMedium)
                    }
                },
                actions = {
                    Button(
                        onClick = { showAdd = true },
                        modifier = Modifier.padding(end = 12.dp),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FuelBlue),
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Add Fuel", fontWeight = FontWeight.SemiBold)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { FuelVehicleHero(activeDistance = 0.0, active = state.logs.firstOrNull()?.endOdometerKm == null) }
            item { FuelKpiGrid(state.totalSpentMinor / 100.0, state.totalLiters, mileage, totalDistance, state.logs.size) }
            item { FuelRangeSelector(selectedRange) { selectedRange = it } }
            item { FuelCharts(trips) }
            item { FuelHighlights(trips) }
            item { FuelHistoryHeader(state.logs.size) { showAdd = true } }

            if (state.logs.isEmpty()) {
                item { FuelEmptyState { showAdd = true } }
            } else {
                items(state.logs, key = { it.id }) { log ->
                    FuelHistoryRow(log) { viewModel.deleteFuel(log) }
                }
            }
        }
    }

    if (showAdd) {
        AddFuelDialog(
            onDismiss = { showAdd = false },
            onSave = { date, liters, price, odometer, note ->
                viewModel.addFuel(date, liters, price, odometer, note)
                showAdd = false
            },
        )
    }
}

@Composable
private fun FuelVehicleHero(activeDistance: Double, active: Boolean) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(FuelHero),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0DCE0)),
    ) {
        Box(Modifier.fillMaxWidth().height(178.dp)) {
            Column(Modifier.padding(start = 28.dp, top = 22.dp)) {
                Text("TVS Raider", color = FuelInk, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Deadpool Edition", color = FuelMuted, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(4.dp))
                Text("“More journeys, better stories”", color = FuelInk, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalGasStation, null, tint = FuelMuted, modifier = Modifier.size(21.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Track · Analyse · Save", color = FuelMuted, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Icon(
                Icons.Default.TwoWheeler,
                contentDescription = "TVS Raider",
                tint = FuelInk,
                modifier = Modifier.align(Alignment.Center).offset(y = 12.dp).size(112.dp),
            )
            Column(Modifier.align(Alignment.TopEnd).width(220.dp).padding(end = 22.dp, top = 23.dp)) {
                Text("Current Cycle", color = FuelMuted, style = MaterialTheme.typography.bodyLarge)
                Text(if (active && activeDistance > 0) "%.0f km".format(activeDistance) else "0 km", color = FuelInk, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Since last fill", color = FuelMuted, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(50)).background(Color(0xFFE7EDF4))) {
                    Box(Modifier.fillMaxWidth((activeDistance / 450.0).coerceIn(0.0, 1.0).toFloat()).fillMaxHeight().clip(RoundedCornerShape(50)).background(FuelGreen))
                }
                Spacer(Modifier.height(6.dp))
                Text("%.0f / 450 km".format(activeDistance), color = FuelMuted, style = MaterialTheme.typography.bodyMedium)
            }
            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                repeat(3) { i ->
                    Box(Modifier.size(if (i == 0) 8.dp else 7.dp).clip(CircleShape).background(if (i == 0) Color(0xFFE53935) else Color(0xFF9DA9B9)))
                }
            }
        }
    }
}

@Composable
private fun FuelKpiGrid(totalSpent: Double, totalLiters: Double, mileage: Double, distance: Double, fills: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FuelKpi("Total Spent", currency(totalSpent), "$fills fills", Icons.Default.LocalGasStation, FuelGreenSoft, FuelGreen, Modifier.weight(1f))
            FuelKpi("Total Fuel", "%.1f L".format(totalLiters), "$fills fills", Icons.Default.WaterDrop, FuelBlueSoft, FuelBlue, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FuelKpi("Avg. Mileage", if (mileage > 0) "%.1f km/L".format(mileage) else "—", if (mileage > 0) "↑ 12%" else "No data", Icons.Default.Speed, FuelOrangeSoft, FuelOrange, Modifier.weight(1f))
            FuelKpi("Total Distance", if (distance > 0) "%.0f km".format(distance) else "—", "", Icons.Default.Route, FuelPurpleSoft, FuelPurple, Modifier.weight(1f))
        }
    }
}

@Composable
private fun FuelKpi(title: String, value: String, subtitle: String, icon: ImageVector, iconBg: Color, iconColor: Color, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(15.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, FuelBorder)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(iconBg), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(25.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = FuelMuted, style = MaterialTheme.typography.bodySmall)
                Text(value, color = FuelInk, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, color = if (subtitle.startsWith("↑")) FuelGreen else FuelMuted, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun FuelRangeSelector(selected: String, onSelected: (String) -> Unit) {
    val options = listOf("1M", "3M", "6M", "1Y", "All")
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = Color(0xFFEDF2F8)) {
        Row(Modifier.padding(3.dp)) {
            options.forEach { option ->
                Surface(
                    Modifier.weight(1f).height(40.dp),
                    onClick = { onSelected(option) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (option == selected) FuelBlue else Color.Transparent,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(option, color = if (option == selected) Color.White else FuelInk, fontWeight = if (option == selected) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun FuelCharts(trips: List<FuelTripInsight>) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FuelChartCard("Mileage Trend", Icons.Default.ShowChart, FuelGreen, if (trips.isNotEmpty()) "Avg. %.1f km/L".format(trips.map { it.mileage }.average()) else "No data", Modifier.weight(1f)) {
            FuelLineChart(trips.map { it.mileage }, Modifier.fillMaxWidth().height(145.dp))
            ChartLabels()
        }
        FuelChartCard("Fuel Cost per Fill", Icons.Default.BarChart, FuelBlue, if (trips.isNotEmpty()) "Avg. ${currency(trips.map { it.cost }.average()).replace(".00", "")}" else "No data", Modifier.weight(1f)) {
            FuelBarChart(trips.map { it.cost }, Modifier.fillMaxWidth().height(145.dp))
            ChartLabels()
        }
    }
}

@Composable
private fun FuelChartCard(title: String, icon: ImageVector, accent: Color, badge: String, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, FuelBorder)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(25.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, color = FuelInk, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Surface(shape = RoundedCornerShape(9.dp), color = accent.copy(alpha = .12f), modifier = Modifier.align(Alignment.End)) {
                Text(badge, color = accent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
            }
            content()
        }
    }
}

@Composable
private fun ChartLabels() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("Jun", "Jul", "Aug", "Sep").forEach { Text(it, color = FuelMuted, style = MaterialTheme.typography.labelSmall) }
    }
}

@Composable
private fun FuelHighlights(trips: List<FuelTripInsight>) {
    val costKm = trips.map { it.costPerKm }.averageOrNull() ?: 0.0
    val longest = trips.maxOfOrNull { it.distanceKm } ?: 0.0
    val best = trips.maxOfOrNull { it.mileage } ?: 0.0
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FuelHighlight("Cost per km", if (costKm > 0) currency(costKm) else "—", "↓ -8%\nvs previous 3 months", Icons.Default.Payments, FuelOrange, Modifier.weight(1f))
        FuelHighlight("Longest Run", if (longest > 0) "%.0f km".format(longest) else "—", "Best distance in a cycle", Icons.Default.Route, FuelBlue, Modifier.weight(1f))
        FuelHighlight("Best Mileage", if (best > 0) "%.1f km/L".format(best) else "—", "Your best so far", Icons.Default.Eco, FuelGreen, Modifier.weight(1f))
    }
}

@Composable
private fun FuelHighlight(title: String, value: String, subtitle: String, icon: ImageVector, accent: Color, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(15.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, FuelBorder)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(accent.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(25.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, color = FuelMuted, style = MaterialTheme.typography.bodySmall)
                Text(value, color = FuelInk, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(subtitle, color = if (subtitle.startsWith("↓")) FuelGreen else FuelMuted, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun FuelHistoryHeader(count: Int, onAdd: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.History, null, tint = FuelMuted, modifier = Modifier.size(31.dp))
        Spacer(Modifier.width(9.dp))
        Text("Recent Fuel History", color = FuelInk, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        TextButton(onClick = onAdd) { Text("View All", color = FuelBlue, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun FuelHistoryRow(log: FuelLogEntity, onDelete: () -> Unit) {
    val distance = log.endOdometerKm?.let { (it - log.odometerKm).coerceAtLeast(0.0) }
    val mileage = if (distance != null && log.liters > 0) distance / log.liters else null
    val cost = log.amountMinor / 100.0
    val costPerKm = if (distance != null && distance > 0) cost / distance else null
    var menuOpen by remember { mutableStateOf(false) }
    val date = java.time.LocalDate.ofEpochDay(log.epochDay)
    val dateText = date.format(DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH))
    val yearText = date.format(DateTimeFormatter.ofPattern("yyyy", Locale.ENGLISH))
    Surface(shape = RoundedCornerShape(15.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, FuelBorder)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                HistoryColumn(dateText, yearText, Modifier.width(70.dp))
                HistoryColumn(distance?.let { "%.0f km".format(it) } ?: "—", "Distance", Modifier.weight(1f))
                HistoryColumn("%.1f L".format(log.liters), "Fuel", Modifier.weight(.8f))
                HistoryColumn(currency(cost).replace(".00", ""), "Total cost", Modifier.weight(1f))
                if (mileage != null) {
                    Surface(shape = RoundedCornerShape(18.dp), color = FuelGreenSoft) {
                        Column(Modifier.padding(horizontal = 9.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("%.1f km/L".format(mileage), color = FuelGreen, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Text("Mileage", color = FuelGreen, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                } else {
                    Text("In progress", color = FuelOrange, style = MaterialTheme.typography.labelSmall)
                }
                Box {
                    IconButton(onClick = { menuOpen = true }, Modifier.size(32.dp)) { Icon(Icons.Default.MoreVert, null, tint = FuelMuted) }
                    DropdownMenu(menuOpen, { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; onDelete() }, leadingIcon = { Icon(Icons.Default.DeleteOutline, null) })
                    }
                }
            }
            Spacer(Modifier.height(9.dp))
            HorizontalDivider(color = FuelBorder)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ReceiptLong, null, tint = FuelMuted, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (log.note.isBlank()) "Fuel fill" else log.note, color = FuelMuted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                if (costPerKm != null) Text("₹%.1f/km".format(costPerKm), color = FuelMuted, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun FuelEmptyState(onAdd: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, FuelBorder)) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.LocalGasStation, null, tint = FuelBlue, modifier = Modifier.size(42.dp))
            Spacer(Modifier.height(10.dp))
            Text("Start tracking your fuel", color = FuelInk, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Add your first fuel fill to unlock mileage and cost insights.", color = FuelMuted, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = FuelBlue)) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("Add Fuel")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFuelDialog(
    onDismiss: () -> Unit,
    onSave: (java.time.LocalDate, Double, Double, Double, String) -> Unit,
) {
    var date by rememberSaveable { mutableStateOf(java.time.LocalDate.now()) }
    var liters by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var odometer by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    val valid = (liters.toDoubleOrNull() ?: 0.0) > 0 &&
        (price.toDoubleOrNull() ?: 0.0) > 0 &&
        (odometer.toDoubleOrNull() ?: -1.0) >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add petrol fill") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("This starts a new fuel cycle. Saving it closes the previous open cycle using this date and odometer.", style = MaterialTheme.typography.bodySmall)
                Text("Start date: $date", style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = { showDatePicker = true }) { Text("Choose start date") }
                OutlinedTextField(liters, { liters = it }, label = { Text("Fuel liters") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                OutlinedTextField(price, { price = it }, label = { Text("Price per liter (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                OutlinedTextField(odometer, { odometer = it }, label = { Text("Odometer start (km)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                OutlinedTextField(note, { note = it }, label = { Text("Note (optional)") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(date, liters.toDouble(), price.toDouble(), odometer.toDouble(), note) }, enabled = valid) {
                Text("Save ₹%.2f".format((liters.toDoubleOrNull() ?: 0.0) * (price.toDoubleOrNull() ?: 0.0)))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    if (showDatePicker) {
        val today = java.time.LocalDate.now()
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = picker) }
    }
}


@Composable
private fun FuelInsightsCard(logs: List<FuelLogEntity>) {
    val trips = logs.mapNotNull { log ->
        val end = log.endOdometerKm ?: return@mapNotNull null
        val distance = (end - log.odometerKm).takeIf { it > 0.0 } ?: return@mapNotNull null
        val cost = log.amountMinor / 100.0
        FuelTripInsight(
            mileage = distance / log.liters,
            cost = cost,
            costPerKm = cost / distance,
            distanceKm = distance,
        )
    }
    val recent = trips.takeLast(6)
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Fuel insights", style = MaterialTheme.typography.titleMedium)
                    Text("Mileage and fuel-cost trends from completed trips", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (recent.isEmpty()) {
                Text("Complete a fuel cycle to unlock mileage and spending trends.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InsightMiniStat("Avg mileage", "%.2f km/L".format(recent.map { it.mileage }.averageOrNull() ?: 0.0), Modifier.weight(1f))
                    InsightMiniStat("Cost / km", currency(recent.map { it.costPerKm }.averageOrNull() ?: 0.0), Modifier.weight(1f))
                    InsightMiniStat("Distance", "%.0f km".format(recent.sumOf { it.distanceKm }), Modifier.weight(1f))
                }
                Text("Mileage trend", style = MaterialTheme.typography.labelLarge)
                FuelLineChart(recent.map { it.mileage }, Modifier.fillMaxWidth().height(150.dp))
                Text("Fuel cost per trip", style = MaterialTheme.typography.labelLarge)
                FuelBarChart(recent.map { it.cost }, Modifier.fillMaxWidth().height(150.dp))
            }
        }
    }
}

private data class FuelTripInsight(
    val mileage: Double,
    val cost: Double,
    val costPerKm: Double,
    val distanceKm: Double,
)
private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else sum() / size

@Composable
private fun InsightMiniStat(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)) {
        Column(Modifier.padding(10.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun FuelLineChart(values: List<Double>, modifier: Modifier = Modifier) {
    if (values.isEmpty()) return

    val chartColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier) {
        val left = 28f
        val right = size.width - 18f
        val top = 18f
        val bottom = size.height - 24f
        val min = values.minOrNull() ?: 0.0
        val max = values.maxOrNull() ?: min
        val range = (max - min).takeIf { it > 0.001 } ?: max.coerceAtLeast(1.0) * 0.15
        val paddedMin = min - range * 0.15
        val paddedMax = max + range * 0.15
        val paddedRange = (paddedMax - paddedMin).coerceAtLeast(1.0)

        // Light baseline/grid so a single completed trip is still visually meaningful.
        for (i in 0..3) {
            val y = top + (bottom - top) * i / 3f
            drawLine(gridColor, Offset(left, y), Offset(right, y), strokeWidth = 1f)
        }

        val step = if (values.size == 1) 0f else (right - left) / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, value ->
            val x = if (values.size == 1) (left + right) / 2f else left + step * i
            val y = bottom - (((value - paddedMin) / paddedRange).toFloat() * (bottom - top))
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(chartColor, 7f, Offset(x, y))
        }

        if (values.size > 1) {
            drawPath(path, chartColor, style = Stroke(5f))
        }
    }
}

@Composable
private fun FuelBarChart(values: List<Double>, modifier: Modifier = Modifier) {
    if (values.isEmpty()) return

    val chartColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier) {
        val left = 28f
        val right = size.width - 18f
        val top = 18f
        val bottom = size.height - 24f
        val maxValue = (values.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
        val chartMax = maxValue * 1.2
        val slot = (right - left) / values.size
        val width = if (values.size == 1) {
            (slot * 0.45f).coerceAtLeast(36f)
        } else {
            (slot - 10f).coerceAtLeast(8f)
        }

        drawLine(gridColor, Offset(left, bottom), Offset(right, bottom), strokeWidth = 2f)

        values.forEachIndexed { i, value ->
            val x = left + slot * i + (slot - width) / 2f
            val h = ((value / chartMax).toFloat() * (bottom - top)).coerceAtLeast(8f)
            drawRoundRect(
                chartColor,
                Offset(x, bottom - h),
                Size(width, h),
                CornerRadius(10f, 10f),
            )
        }
    }
}
private fun formatDate(epochDay: Long): String = java.time.LocalDate.ofEpochDay(epochDay).toString()

private fun currency(value: Double): String = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(max(0.0, value))
