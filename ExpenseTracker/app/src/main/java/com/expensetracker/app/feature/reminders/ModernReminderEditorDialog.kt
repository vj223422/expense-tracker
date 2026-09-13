package com.expensetracker.app.feature.reminders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.expensetracker.app.data.entity.ReminderEntity
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernReminderEditorDialog(
    initial: ReminderEntity?,
    onSave: (String, String, Long, Long?, String, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialStart = initial?.triggerAtEpochMillis ?: System.currentTimeMillis()
    var title by remember(initial) { mutableStateOf(initial?.title.orEmpty()) }
    var note by remember(initial) { mutableStateOf(initial?.note.orEmpty()) }
    var selectedStartDate by remember(initial) { mutableLongStateOf(initialStart) }
    var selectedStartTime by remember(initial) { mutableLongStateOf(initialStart) }
    var selectedEndDate by remember(initial) { mutableStateOf<Long?>(initial?.endDateEpochMillis) }
    var recurrence by remember(initial) { mutableStateOf(initial?.recurrence ?: "ONCE") }
    var intervalDays by remember(initial) { mutableLongStateOf((initial?.customIntervalDays ?: 1).toLong()) }
    var showStartDate by remember { mutableStateOf(false) }
    var showStartTime by remember { mutableStateOf(false) }
    var showEndDate by remember { mutableStateOf(false) }
    var dateError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 18.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = MaterialTheme.shapes.large) {
                        Icon(Icons.Default.NotificationsActive, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(14.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (initial == null) "New reminder" else "Edit reminder", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        Text("Set a reminder for anything", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) { Text("×", style = MaterialTheme.typography.headlineMedium) }
                }

                OutlinedTextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Title") }, placeholder = { Text("e.g. Pay electricity bill") }, leadingIcon = { Icon(Icons.Default.Edit, null) }, singleLine = true)
                OutlinedTextField(value = note, onValueChange = { note = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Note (optional)") }, placeholder = { Text("Add a note...") }, leadingIcon = { Icon(Icons.Default.Edit, null) }, minLines = 2, maxLines = 3)

                Text("Start", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DateTimeCard(Modifier.weight(1f), { Icon(Icons.Default.DateRange, null) }, "Start date", DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(selectedStartDate))) { showStartDate = true }
                    DateTimeCard(Modifier.weight(1f), { Icon(Icons.Default.AccessTime, null) }, "Time", DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(selectedStartTime))) { showStartTime = true }
                }

                Text("End", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showEndDate = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
                            Icon(Icons.Default.Event, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(12.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("End date", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(selectedEndDate?.let { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it)) } ?: "No end date", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        if (selectedEndDate != null) TextButton(onClick = { selectedEndDate = null; dateError = null }) { Text("Clear") }
                    }
                }
                Text(if (selectedEndDate != null) "Tap the end date card to change the date." else "Tap the end date card to choose a date, or leave it empty to repeat indefinitely.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                dateError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = MaterialTheme.shapes.large) {
                        Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(12.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Repeats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Choose how often this reminder occurs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("ONCE" to "Once", "DAILY" to "Daily", "WEEKLY" to "Weekly", "MONTHLY" to "Monthly", "YEARLY" to "Yearly").forEach { (key, label) ->
                        RepeatCard(label, recurrence == key) { recurrence = key }
                    }
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { recurrence = "CUSTOM" },
                    colors = CardDefaults.cardColors(containerColor = if (recurrence == "CUSTOM") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, null, tint = if (recurrence == "CUSTOM") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Custom", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Set your own repeating schedule", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("›", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (recurrence == "CUSTOM") {
                    OutlinedTextField(value = intervalDays.toString(), onValueChange = { intervalDays = (it.toIntOrNull()?.coerceAtLeast(1) ?: 1).toLong() }, modifier = Modifier.fillMaxWidth(), label = { Text("Repeat every N days") }, singleLine = true)
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(52.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape = MaterialTheme.shapes.extraLarge) {
                        Text("Cancel", style = MaterialTheme.typography.titleMedium)
                    }
                    Button(
                        enabled = title.isNotBlank(),
                        onClick = {
                            val calendar = Calendar.getInstance().apply {
                                timeInMillis = selectedStartDate
                                val time = Calendar.getInstance().apply { timeInMillis = selectedStartTime }
                                set(Calendar.HOUR_OF_DAY, time.get(Calendar.HOUR_OF_DAY)); set(Calendar.MINUTE, time.get(Calendar.MINUTE)); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                            }
                            val start = calendar.timeInMillis
                            val end = selectedEndDate
                            if (end != null && end < start.startOfDay()) dateError = "End date cannot be before the start date"
                            else { dateError = null; onSave(title.trim(), note.trim(), start, end, recurrence, intervalDays.toInt()) }
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant, disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                    ) { Text("Save reminder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }

    if (showStartDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = selectedStartDate)
        DatePickerDialog(
            onDismissRequest = { showStartDate = false },
            confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { selectedStartDate = it; if (selectedEndDate != null && selectedEndDate!! < it.startOfDay()) selectedEndDate = it }; showStartDate = false }) { Text("Done") } },
            dismissButton = { TextButton(onClick = { showStartDate = false }) { Text("Cancel") } },
        ) { DatePicker(state) }
    }
    if (showEndDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = selectedEndDate ?: selectedStartDate)
        DatePickerDialog(
            onDismissRequest = { showEndDate = false },
            confirmButton = { TextButton(onClick = {
                state.selectedDateMillis?.let {
                    if (it < selectedStartDate.startOfDay()) dateError = "End date cannot be before the start date"
                    else { selectedEndDate = it; dateError = null }
                }
                showEndDate = false
            }) { Text("Done") } },
            dismissButton = { TextButton(onClick = { showEndDate = false }) { Text("Cancel") } },
        ) { DatePicker(state) }
    }
    if (showStartTime) {
        val calendar = Calendar.getInstance().apply { timeInMillis = selectedStartTime }
        val state = rememberTimePickerState(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false)
        Dialog(onDismissRequest = { showStartTime = false }) {
            Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text("Select start time", style = MaterialTheme.typography.headlineSmall)
                    TimeInput(state)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showStartTime = false }) { Text("Cancel") }
                        TextButton(onClick = {
                            selectedStartTime = Calendar.getInstance().apply { timeInMillis = selectedStartTime; set(Calendar.HOUR_OF_DAY, state.hour); set(Calendar.MINUTE, state.minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
                            showStartTime = false
                        }) { Text("Done") }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateTimeCard(modifier: Modifier, icon: @Composable () -> Unit, label: String, value: String, onClick: () -> Unit) {
    Card(modifier = modifier.height(104.dp), onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
                icon()
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2)
            }
        }
    }
}

@Composable
private fun RepeatCard(label: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(92.dp).height(92.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("○", color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
        }
    }
}

private fun Long.startOfDay(): Long = java.time.Instant.ofEpochMilli(this)
    .atZone(java.time.ZoneId.systemDefault())
    .toLocalDate()
    .atStartOfDay(java.time.ZoneId.systemDefault())
    .toInstant()
    .toEpochMilli()
