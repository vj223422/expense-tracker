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
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
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
    val rangeDays = when (selectedRange) {
        "1M" -> 30L
        "3M" -> 90L
        "6M" -> 180L
        "1Y" -> 365L
        else -> null
    }
    val cutoffEpochDay = rangeDays?.let { java.time.LocalDate.now().toEpochDay() - it }
    val filteredCompleted = completed
        .filter { cutoffEpochDay == null || it.epochDay >= cutoffEpochDay }
        .sortedBy { it.epochDay }
    val trips = filteredCompleted.mapNotNull { log ->
        val end = log.endOdometerKm ?: return@mapNotNull null
        val distance = (end - log.odometerKm).takeIf { it > 0.0 } ?: return@mapNotNull null
        val cost = log.amountMinor / 100.0
        if (log.liters <= 0.0) return@mapNotNull null
        FuelTripInsight(distance / log.liters, cost, cost / distance, distance)
    }.takeLast(12)

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
                    Column(
                        modifier = Modifier
                            .widthIn(max = 210.dp)
                            .padding(end = 4.dp),
                    ) {
                        Text(
                            "Fuel & Mileage",
                            color = FuelInk,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        Text(
                            "Track your fuel fills and get better insights",
                            color = FuelMuted,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = { showAdd = true },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .widthIn(min = 112.dp, max = 126.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FuelBlue),
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Fuel", fontWeight = FontWeight.SemiBold, maxLines = 1)
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
            item { FuelVehicleHero(activeDistance = 0.0, active = state.logs.any { it.endOdometerKm == null }) }
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(FuelHero),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0DCE0)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(178.dp)
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            // Left content has its own fixed area; the bike is kept on the right
            // so the image can never cover the title, quote, or cycle metrics.
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .width(190.dp),
            ) {
                Text(
                    "TVS Raider",
                    color = FuelInk,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Deadpool",
                        color = Color(0xFFE53935),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "Edition",
                        color = FuelMuted,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    "“More journeys,\nbetter stories”",
                    color = FuelInk,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(128.dp),
            ) {
                Text(
                    "Current Cycle",
                    color = FuelMuted,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                )
                Text(
                    if (active && activeDistance > 0) "%.0f km".format(activeDistance) else "0 km",
                    color = FuelInk,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    "Since last fill",
                    color = FuelMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFE7EDF4)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth((activeDistance / 450.0).coerceIn(0.0, 1.0).toFloat())
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(FuelGreen),
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    "%.0f / 450 km".format(activeDistance),
                    color = FuelMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.LocalGasStation,
                    null,
                    tint = FuelMuted,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    "Track",
                    color = FuelMuted,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(10.dp))
                Icon(
                    Icons.Default.BarChart,
                    null,
                    tint = FuelMuted,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    "Analyse",
                    color = FuelMuted,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(10.dp))
                Icon(
                    Icons.Default.BookmarkBorder,
                    null,
                    tint = FuelMuted,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    "Save",
                    color = FuelMuted,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            AsyncImage(
                model = "https://www.tvsmotor.com/tvs-raider/-/media/Brand-Pages-Webp/Raider/Raider-360/360-raider/SSE/Deadpool/1.webp",
                contentDescription = "TVS Raider Deadpool Edition",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 0.dp, bottom = 0.dp)
                    .size(width = 170.dp, height = 112.dp),
            )
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
                Text(subtitle, color = FuelMuted, style = MaterialTheme.typography.labelSmall, maxLines = 2)
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
                    Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClick = { onSelected(option) }),
                    shape = RoundedCornerShape(18.dp),
                    color = if (option == selected) FuelBlue else Color.Transparent,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            option,
                            color = if (option == selected) Color.White else FuelInk,
                            fontWeight = if (option == selected) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FuelCharts(trips: List<FuelTripInsight>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FuelChartCard("Mileage Trend", Icons.Default.ShowChart, FuelGreen, if (trips.isNotEmpty()) "Avg. %.1f km/L".format(trips.map { it.mileage }.average()) else "No data", Modifier.weight(1f)) {
            FuelLineChart(trips.map { it.mileage }, Modifier.fillMaxWidth().height(170.dp))
            ChartLabels()
        }
        FuelChartCard("Fuel Cost per Fill", Icons.Default.BarChart, FuelBlue, if (trips.isNotEmpty()) "Avg. ${currency(trips.map { it.cost }.average()).replace(".00", "")}" else "No data", Modifier.weight(1f)) {
            FuelBarChart(trips.map { it.cost }, Modifier.fillMaxWidth().height(170.dp))
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
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        listOf("Jun", "Jul", "Aug", "Sep").forEach {
            Text(
                it,
                color = FuelMuted,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun FuelHighlights(trips: List<FuelTripInsight>) {
    val costKm = trips.map { it.costPerKm }.averageOrNull()
    val longest = trips.maxOfOrNull { it.distanceKm }
    val best = trips.maxOfOrNull { it.mileage }
    val cycleCount = trips.size
    val cycleLabel = if (cycleCount == 1) "cycle" else "cycles"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FuelHighlight(
            title = "Cost per km",
            value = costKm?.takeIf { it > 0 }?.let { currency(it).replace(".00", "") } ?: "—",
            subtitle = if (costKm != null && costKm > 0) {
                "Average • " + cycleCount + " completed " + cycleLabel
            } else {
                "No completed cycle data"
            },
            icon = Icons.Default.Payments,
            accent = FuelOrange,
            modifier = Modifier.weight(1f),
        )
        FuelHighlight(
            title = "Longest Run",
            value = longest?.takeIf { it > 0 }?.let { "%.0f km".format(it) } ?: "—",
            subtitle = if (longest != null && longest > 0) {
                "Longest completed cycle"
            } else {
                "No completed cycle data"
            },
            icon = Icons.Default.Route,
            accent = FuelBlue,
            modifier = Modifier.weight(1f),
        )
        FuelHighlight(
            title = "Best Mileage",
            value = best?.takeIf { it > 0 }?.let { "%.1f km/L".format(it) } ?: "—",
            subtitle = if (best != null && best > 0) {
                "Best completed cycle"
            } else {
                "No completed cycle data"
            },
            icon = Icons.Default.Eco,
            accent = FuelGreen,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun FuelHighlight(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier.height(180.dp),
        shape = RoundedCornerShape(15.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, FuelBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(11.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = .12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                title,
                color = FuelMuted,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                value,
                color = FuelInk,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                color = FuelMuted,
                style = MaterialTheme.typography.labelSmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 2,
            )
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
private fun HistoryColumn(value: String, label: String, modifier: Modifier) {
    Column(modifier) {
        Text(
            value,
            color = FuelInk,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            color = FuelMuted,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

@Composable
private fun FuelHistoryRow(log: FuelLogEntity, onDelete: () -> Unit) {
    val distance = log.endOdometerKm?.let { (it - log.odometerKm).coerceAtLeast(0.0) }
    val mileage = if (distance != null && log.liters > 0) distance / log.liters else null
    val cost = log.amountMinor / 100.0
    val costPerKm = if (distance != null && distance > 0) cost / distance else null
    val inProgress = log.endOdometerKm == null
    val date = java.time.LocalDate.ofEpochDay(log.epochDay)
    val dateText = date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, FuelBorder),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(FuelBlue.copy(alpha = .16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.LocalGasStation,
                        contentDescription = null,
                        tint = FuelBlue,
                        modifier = Modifier.size(25.dp),
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        "Start: %s · %.1f km".format(dateText, log.odometerKm),
                        color = FuelInk,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (log.endEpochDay != null && log.endOdometerKm != null) {
                            val endDate = java.time.LocalDate.ofEpochDay(log.endEpochDay)
                            "End: %s · %.1f km".format(
                                endDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)),
                                log.endOdometerKm,
                            )
                        } else {
                            "End: — · —"
                        },
                        color = FuelMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "%.2f L · %s/L · %s".format(
                            log.liters,
                            currency(if (log.liters > 0) cost / log.liters else 0.0).replace(".00", ""),
                            currency(cost).replace(".00", ""),
                        ),
                        color = FuelInk,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    if (inProgress) {
                        val transition = rememberInfiniteTransition(label = "fuelInProgress")
                        val alpha by transition.animateFloat(
                            initialValue = 0.25f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(750),
                                repeatMode = RepeatMode.Reverse,
                            ),
                            label = "fuelInProgressAlpha",
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp, end = 8.dp)
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(FuelOrange.copy(alpha = alpha)),
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete fuel entry",
                            tint = FuelMuted,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = FuelBorder)
            Spacer(Modifier.height(9.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FuelHistoryMetric(
                    icon = Icons.Default.Route,
                    value = distance?.let { "%.1f km".format(it) } ?: "—",
                    label = "Distance",
                    modifier = Modifier.weight(1f),
                )
                VerticalDivider(
                    modifier = Modifier.height(40.dp),
                    color = FuelBorder,
                )
                FuelHistoryMetric(
                    icon = Icons.Default.WaterDrop,
                    value = mileage?.let { "%.2f km/L".format(it) } ?: "—",
                    label = "Mileage",
                    valueColor = if (mileage != null) FuelGreen else FuelMuted,
                    modifier = Modifier.weight(1f),
                )
                VerticalDivider(
                    modifier = Modifier.height(40.dp),
                    color = FuelBorder,
                )
                FuelHistoryMetric(
                    icon = Icons.Default.Payments,
                    value = costPerKm?.let { currency(it).replace(".00", "") } ?: "—",
                    label = "Cost/km",
                    modifier = Modifier.weight(1f),
                )
            }

            if (log.note.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ReceiptLong,
                        null,
                        tint = FuelMuted,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        log.note,
                        color = FuelMuted,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun FuelHistoryMetric(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier,
    valueColor: Color = FuelInk,
) {
    Row(
        modifier = modifier.padding(horizontal = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            null,
            tint = if (valueColor == FuelGreen) FuelGreen else FuelMuted,
            modifier = Modifier.size(23.dp),
        )
        Spacer(Modifier.width(5.dp))
        Column {
            Text(
                value,
                color = valueColor,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                label,
                color = FuelMuted,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
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


private data class FuelTripInsight(
    val mileage: Double,
    val cost: Double,
    val costPerKm: Double,
    val distanceKm: Double,
)

private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()

@Composable
private fun FuelLineChart(values: List<Double>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val left = 10f
        val right = size.width - 4f
        val top = 8f
        val bottom = size.height - 8f
        val grid = Color(0xFFE4EAF2)
        for (i in 0..3) {
            val y = top + (bottom - top) * i / 3f
            drawLine(grid, Offset(left, y), Offset(right, y), 1f)
        }
        if (values.isEmpty()) return@Canvas
        val min = values.minOrNull() ?: 0.0
        val max = values.maxOrNull() ?: min
        val range = (max - min).takeIf { it > 0.001 } ?: max.coerceAtLeast(1.0) * .18
        val low = min - range * .18
        val high = max + range * .18
        val span = (high - low).coerceAtLeast(1.0)
        val step = if (values.size == 1) 0f else (right - left) / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, value ->
            val x = if (values.size == 1) (left + right) / 2f else left + step * i
            val y = bottom - (((value - low) / span).toFloat() * (bottom - top))
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        if (values.size > 1) drawPath(path, FuelGreen, style = Stroke(4f))
        values.forEachIndexed { i, value ->
            val x = if (values.size == 1) (left + right) / 2f else left + step * i
            val y = bottom - (((value - low) / span).toFloat() * (bottom - top))
            drawCircle(FuelGreen, 6f, Offset(x, y))
        }
    }
}

@Composable
private fun FuelBarChart(values: List<Double>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val left = 10f
        val right = size.width - 4f
        val top = 8f
        val bottom = size.height - 8f
        val grid = Color(0xFFE4EAF2)
        for (i in 0..3) {
            val y = top + (bottom - top) * i / 3f
            drawLine(grid, Offset(left, y), Offset(right, y), 1f)
        }
        if (values.isEmpty()) return@Canvas
        val maxValue = (values.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
        val chartMax = maxValue * 1.18
        val slot = (right - left) / values.size
        val width = if (values.size == 1) (slot * .42f).coerceAtLeast(26f) else (slot * .52f).coerceAtLeast(12f)
        values.forEachIndexed { i, value ->
            val x = left + slot * i + (slot - width) / 2f
            val h = ((value / chartMax).toFloat() * (bottom - top)).coerceAtLeast(5f)
            drawRoundRect(FuelBlue, Offset(x, bottom - h), Size(width, h), CornerRadius(7f, 7f))
        }
    }
}

private fun currency(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(max(0.0, value))
