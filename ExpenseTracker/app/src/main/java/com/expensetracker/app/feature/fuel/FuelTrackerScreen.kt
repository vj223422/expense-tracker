package com.expensetracker.app.feature.fuel

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max

@Composable
fun FuelTrackerScreen(onNavigateBack: () -> Unit, viewModel: FuelTrackerViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var tripDistance by rememberSaveable { mutableStateOf("") }
    val mileage = state.averageMileage ?: 0.0
    val tripKm = tripDistance.toDoubleOrNull() ?: 0.0
    val tripLiters = if (mileage > 0) tripKm / mileage else 0.0
    val tripCost = tripLiters * state.averagePricePerLiter

    Scaffold(topBar = {
        TopAppBar(title = { Text("Fuel & Mileage") },
            navigationIcon = { IconButton(onClick = onNavigateBack) { Text("‹", style = MaterialTheme.typography.headlineLarge) } },
            actions = { IconButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, "Add fuel") } })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FuelStatCard("Fuel spent", currency(state.totalSpentMinor / 100.0), Modifier.weight(1f))
                FuelStatCard("Fuel used", "%.2f L".format(state.totalLiters), Modifier.weight(1f))
            }}
            item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FuelStatCard("Avg. price", if (state.averagePricePerLiter > 0) currency(state.averagePricePerLiter) + "/L" else "—", Modifier.weight(1f))
                FuelStatCard("Mileage", state.averageMileage?.let { "%.2f km/L".format(it) } ?: "Add 2 fills", Modifier.weight(1f))
            }}
            item { Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Route, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(10.dp)); Text("Per-trip fuel estimate", style = MaterialTheme.typography.titleMedium) }
                OutlinedTextField(tripDistance, { tripDistance = it }, Modifier.fillMaxWidth(), label = { Text("Trip distance (km)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                if (mileage > 0 && state.averagePricePerLiter > 0) {
                    Text("%.2f L estimated • %s fuel cost".format(tripLiters, currency(tripCost)), style = MaterialTheme.typography.bodyLarge)
                    Text("Based on %.2f km/L and %s/L average price".format(mileage, currency(state.averagePricePerLiter)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else Text("Add at least two fuel entries with odometer readings to calculate mileage and trip cost.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }}}
            item { Text("Fuel history", style = MaterialTheme.typography.titleLarge) }
            if (state.logs.isEmpty()) item { Text("No fuel entries yet. Tap + to record your first petrol fill.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            else items(state.logs, key = { it.id }) { log -> Card { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(44.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.LocalGasStation, null, tint = MaterialTheme.colorScheme.primary) } }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("%.2f L • %.1f km".format(log.liters, log.odometerKm))
                    Text(currency(log.amountMinor / 100.0) + " • " + currency(log.amountMinor / 100.0 / log.liters) + "/L", style = MaterialTheme.typography.bodyMedium)
                    if (log.note.isNotBlank()) Text(log.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { viewModel.deleteFuel(log) }) { Icon(Icons.Default.DeleteOutline, "Delete") }
            }}}
        }
    }
    if (showAdd) AddFuelDialog({ showAdd = false }, { amount, liters, odometer, note -> viewModel.addFuel(amount, liters, odometer, note); showAdd = false })
}

@Composable
private fun FuelStatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) { Column(Modifier.padding(14.dp)) { Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(4.dp)); Text(value, style = MaterialTheme.typography.titleMedium) } }
}

@Composable
private fun AddFuelDialog(onDismiss: () -> Unit, onSave: (Double, Double, Double, String) -> Unit) {
    var amount by rememberSaveable { mutableStateOf("") }
    var liters by rememberSaveable { mutableStateOf("") }
    var odometer by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    val valid = (amount.toDoubleOrNull() ?: 0.0) > 0 && (liters.toDoubleOrNull() ?: 0.0) > 0 && (odometer.toDoubleOrNull() ?: -1.0) >= 0
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add petrol fill") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(amount, { amount = it }, label = { Text("Amount (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
        OutlinedTextField(liters, { liters = it }, label = { Text("Petrol (liters)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
        OutlinedTextField(odometer, { odometer = it }, label = { Text("Odometer (km)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
        OutlinedTextField(note, { note = it }, label = { Text("Note (optional)") }, singleLine = true)
    }}, confirmButton = { TextButton(onClick = { onSave(amount.toDouble(), liters.toDouble(), odometer.toDouble(), note) }, enabled = valid) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

private fun currency(value: Double): String = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(max(0.0, value))
