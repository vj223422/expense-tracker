package com.expensetracker.app.feature.fuel

import com.expensetracker.app.data.entity.FuelLogEntity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.ZoneOffset
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max

@Composable
fun FuelTrackerScreen(
    onNavigateBack: () -> Unit,
    viewModel: FuelTrackerViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var tripDistance by rememberSaveable { mutableStateOf("") }

    val mileage = state.averageMileage ?: 0.0
    val tripKm = tripDistance.toDoubleOrNull() ?: 0.0
    val tripLiters = if (mileage > 0) tripKm / mileage else 0.0
    val tripCost = tripLiters * state.averagePricePerLiter

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fuel & Mileage") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("‹", style = MaterialTheme.typography.headlineLarge)
                    }
                },
                actions = {
                    IconButton(onClick = { showAdd = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add fuel")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FuelStatCard(
                        "Fuel spent",
                        currency(state.totalSpentMinor / 100.0),
                        Modifier.weight(1f),
                    )
                    FuelStatCard(
                        "Fuel used",
                        "%.2f L".format(state.totalLiters),
                        Modifier.weight(1f),
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FuelStatCard(
                        "Avg. price",
                        if (state.averagePricePerLiter > 0) {
                            currency(state.averagePricePerLiter) + "/L"
                        } else {
                            "—"
                        },
                        Modifier.weight(1f),
                    )
                    FuelStatCard(
                        "Mileage",
                        state.averageMileage?.let { "%.2f km/L".format(it) } ?: "Add 2 fills",
                        Modifier.weight(1f),
                    )
                }
            }

            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Route,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Per-trip fuel estimate",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }

                        OutlinedTextField(
                            value = tripDistance,
                            onValueChange = { tripDistance = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Trip distance (km)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                            ),
                        )

                        if (mileage > 0 && state.averagePricePerLiter > 0) {
                            Text(
                                "%.2f L estimated • %s fuel cost".format(
                                    tripLiters,
                                    currency(tripCost),
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                "Based on %.2f km/L and %s/L average price".format(
                                    mileage,
                                    currency(state.averagePricePerLiter),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Text(
                                "Add at least two fuel entries with odometer readings to calculate mileage and trip cost.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item { FuelInsightsCard(logs = state.logs) }

            item {
                Text("Fuel history", style = MaterialTheme.typography.titleLarge)
            }

            if (state.logs.isEmpty()) {
                item {
                    Text(
                        "No fuel entries yet. Tap + to record your first petrol fill.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(state.logs, key = { it.id }) { log ->
                    val endOdometer = log.endOdometerKm
                    val distance = endOdometer
                        ?.let { it - log.odometerKm }
                        ?.takeIf { it > 0.0 }
                    val mileageForTrip = distance
                        ?.takeIf { log.liters > 0.0 }
                        ?.let { it / log.liters }
                    val costPerKm = distance
                        ?.takeIf { it > 0.0 }
                        ?.let { (log.amountMinor / 100.0) / it }

                    Card {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(44.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.LocalGasStation,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }

                                Spacer(Modifier.width(12.dp))

                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Start: ${formatDate(log.epochDay)} • %.1f km".format(
                                            log.odometerKm,
                                        ),
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    Text(
                                        "End: ${log.endEpochDay?.let(::formatDate) ?: "—"} • ${endOdometer?.let { "%.1f km".format(it) } ?: "—"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        "%.2f L • %s/L • %s".format(
                                            log.liters,
                                            currency(log.amountMinor / 100.0 / log.liters),
                                            currency(log.amountMinor / 100.0),
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }

                                if (distance == null) {
                                    InProgressDot()
                                }

                                IconButton(onClick = { viewModel.deleteFuel(log) }) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Delete",
                                    )
                                }
                            }

                            if (log.note.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    log.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(
                                    top = 12.dp,
                                    bottom = 10.dp,
                                ),
                            )

                            if (distance != null && mileageForTrip != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    FuelMetric(
                                        label = "Distance",
                                        value = "%.1f km".format(distance),
                                        modifier = Modifier.weight(1f),
                                        icon = {
                                            Icon(
                                                Icons.Default.Route,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                        },
                                        iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        iconColor = MaterialTheme.colorScheme.primary,
                                    )
                                    VerticalDivider(modifier = Modifier.height(44.dp))
                                    FuelMetric(
                                        label = "Mileage",
                                        value = "%.2f km/L".format(mileageForTrip),
                                        valueColor = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.weight(1f),
                                        icon = {
                                            Icon(
                                                Icons.Default.Speed,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.tertiary,
                                            )
                                        },
                                        iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        iconColor = MaterialTheme.colorScheme.tertiary,
                                    )
                                    VerticalDivider(modifier = Modifier.height(44.dp))
                                    FuelMetric(
                                        label = "Cost/km",
                                        value = currency(costPerKm ?: 0.0),
                                        modifier = Modifier.weight(1f),
                                        icon = {
                                            Icon(
                                                Icons.Default.CurrencyRupee,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary,
                                            )
                                        },
                                        iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        iconColor = MaterialTheme.colorScheme.secondary,
                                    )
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Icon(
                                        Icons.Default.Route,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Column {
                                        Text(
                                            "Mileage (this trip)",
                                            style = MaterialTheme.typography.labelLarge,
                                        )
                                        Text(
                                            "— —",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
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
        FuelTripInsight(distance / log.liters, cost, cost / distance)
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
                    InsightMiniStat("Distance", "%.0f km".format(recent.sumOf { it.cost / it.costPerKm }), Modifier.weight(1f))
                }
                Text("Mileage trend", style = MaterialTheme.typography.labelLarge)
                FuelLineChart(recent.map { it.mileage }, Modifier.fillMaxWidth().height(150.dp))
                Text("Fuel cost per trip", style = MaterialTheme.typography.labelLarge)
                FuelBarChart(recent.map { it.cost }, Modifier.fillMaxWidth().height(150.dp))
            }
        }
    }
}

private data class FuelTripInsight(val mileage: Double, val cost: Double, val costPerKm: Double)
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
    Canvas(modifier) {
        val left = 18f; val right = size.width - 12f; val top = 14f; val bottom = size.height - 18f
        val min = values.minOrNull() ?: 0.0; val max = values.maxOrNull() ?: min; val range = (max - min).takeIf { it > 0.001 } ?: 1.0
        val step = if (values.size == 1) 0f else (right - left) / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = left + step * i; val y = bottom - (((v - min) / range).toFloat() * (bottom - top))
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(chartColor, 6f, Offset(x, y))
        }
        drawPath(path, chartColor, style = Stroke(5f))
    }
}

@Composable
private fun FuelBarChart(values: List<Double>, modifier: Modifier = Modifier) {
    if (values.isEmpty()) return
    val chartColor = MaterialTheme.colorScheme.tertiary
    Canvas(modifier) {
        val left = 16f; val right = size.width - 12f; val top = 14f; val bottom = size.height - 18f
        val max = (values.maxOrNull() ?: 1.0).coerceAtLeast(1.0); val slot = (right - left) / values.size; val width = (slot - 10f).coerceAtLeast(8f)
        values.forEachIndexed { i, v ->
            val x = left + slot * i + (slot - width) / 2f; val h = ((v / max).toFloat() * (bottom - top)).coerceAtLeast(4f)
            drawRoundRect(chartColor, Offset(x, bottom - h), Size(width, h), CornerRadius(10f, 10f))
        }
    }
}
private fun formatDate(epochDay: Long): String = java.time.LocalDate.ofEpochDay(epochDay).toString()

private fun currency(value: Double): String = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(max(0.0, value))
