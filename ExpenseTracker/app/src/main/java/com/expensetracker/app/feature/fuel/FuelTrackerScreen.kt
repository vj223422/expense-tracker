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
private fun FuelHeroCard(
    totalSpent: Double,
    totalLiters: Double,
    mileage: Double,
    completedTrips: Int,
    activeTrip: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.LocalGasStation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Fuel dashboard",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (activeTrip) "Current fuel cycle is active"
                        else "Ready for your next fuel fill",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                    )
                }
                if (activeTrip) {
                    FuelActivePill()
                }
            }

            Spacer(Modifier.height(22.dp))

            Text(
                "Total fuel spend",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.68f),
            )
            Text(
                currency(totalSpent),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
            )
            Spacer(Modifier.height(12.dp))

            Row {
                FuelHeroStat(
                    "Fuel",
                    "%.1f L".format(totalLiters),
                    Modifier.weight(1f),
                )
                FuelHeroStat(
                    "Mileage",
                    if (mileage > 0) "%.2f km/L".format(mileage) else "—",
                    Modifier.weight(1f),
                )
                FuelHeroStat(
                    "Completed",
                    completedTrips.toString(),
                    Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun FuelActivePill() {
    val transition = rememberInfiniteTransition(label = "fuel-active")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "fuel-active-alpha",
    )

    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
    ) {
        Row(
            Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF9800).copy(alpha = alpha)),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                "ACTIVE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun FuelHeroStat(
    label: String,
    value: String,
    modifier: Modifier,
) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.62f),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun FuelSectionTitle(title: String, subtitle: String) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FuelDashboardStat(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(15.dp)) {
            Surface(
                modifier = Modifier.size(34.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp),
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            Spacer(Modifier.height(11.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun FuelTripCalculator(
    tripDistance: String,
    onTripDistanceChange: (String) -> Unit,
    mileage: Double,
    averagePrice: Double,
    tripLiters: Double,
    tripCost: Double,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Route,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "Trip cost calculator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Estimate fuel for your next trip",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedTextField(
                value = tripDistance,
                onValueChange = onTripDistanceChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Trip distance") },
                suffix = { Text("km") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp),
            )

            if (mileage > 0 && averagePrice > 0 && tripDistance.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FuelCalculatorResult(
                        "Fuel needed",
                        "%.2f L".format(tripLiters),
                        Modifier.weight(1f),
                    )
                    FuelCalculatorResult(
                        "Estimated cost",
                        currency(tripCost),
                        Modifier.weight(1f),
                    )
                }
            } else {
                Text(
                    if (mileage > 0) {
                        "Enter a distance to estimate fuel and cost."
                    } else {
                        "Complete a fuel cycle to calculate estimates from your actual mileage."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FuelCalculatorResult(
    label: String,
    value: String,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun FuelEmptyState(onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.LocalGasStation,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "Start tracking your fuel",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "Add your first petrol fill with the amount, price and odometer reading.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Add first fill")
            }
        }
    }
}

@Composable
private fun InProgressDot() {
    val transition = rememberInfiniteTransition(label = "fuel-in-progress")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "fuel-in-progress-alpha",
    )

    Surface(
        modifier = Modifier.size(14.dp),
        shape = CircleShape,
        color = Color(0xFFFF9800).copy(alpha = alpha),
        shadowElevation = 2.dp,
    ) {}
}

@Composable
private fun FuelMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    icon: @Composable () -> Unit,
    iconContainerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primaryContainer,
    iconColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Surface(
            modifier = Modifier.size(38.dp),
            shape = CircleShape,
            color = iconContainerColor,
        ) {
            Box(contentAlignment = Alignment.Center) {
                icon()
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            color = valueColor,
        )
    }
}

@Composable
private fun FuelStatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) { Column(Modifier.padding(14.dp)) { Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(4.dp)); Text(value, style = MaterialTheme.typography.titleMedium) } }
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
