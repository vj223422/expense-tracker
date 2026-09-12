package com.expensetracker.app.feature.reminders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.data.entity.NoteEntity
import com.expensetracker.app.data.entity.ReminderEntity
import org.koin.androidx.compose.koinViewModel
import java.text.DateFormat
import java.util.Date
import java.util.TimeZone

@Composable
fun RemindersScreen(viewModel: RemindersViewModel = koinViewModel()) {
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    var showReminderEditor by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<ReminderEntity?>(null) }
    var showNoteEditor by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }

    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Reminders") }, icon = { Icon(Icons.Default.Notifications, null) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Notes") }, icon = { Icon(Icons.Default.Notes, null) })
            }
            if (tab == 0) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
                    Button(onClick = { editingReminder = null; showReminderEditor = true }) { Icon(Icons.Default.Add, null); Spacer(Modifier.padding(3.dp)); Text("Add reminder") }
                }
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(reminders, key = { it.id }) { reminder -> ReminderRow(reminder, viewModel, { editingReminder = reminder; showReminderEditor = true }) }
                }
            } else {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
                    Button(onClick = { editingNote = null; showNoteEditor = true }) { Icon(Icons.Default.Add, null); Spacer(Modifier.padding(3.dp)); Text("Add note") }
                }
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(notes, key = { it.id }) { note -> NoteRow(note, viewModel, { editingNote = note; showNoteEditor = true }) }
                }
            }
        }
    }

    if (showReminderEditor) {
        ReminderEditorDialog(editingReminder, { title, note, at, recurrence, days ->
            viewModel.saveReminder(editingReminder?.id, title, note, at, recurrence, days)
            showReminderEditor = false
        }, { showReminderEditor = false })
    }
    if (showNoteEditor) {
        NoteEditorDialog(editingNote, { title, content ->
            viewModel.saveNote(editingNote?.id, title, content)
            showNoteEditor = false
        }, { showNoteEditor = false })
    }
}

@Composable
private fun ReminderRow(reminder: ReminderEntity, vm: RemindersViewModel, onEdit: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(reminder.title, style = MaterialTheme.typography.titleMedium)
                Text(formatDateTime(reminder.triggerAtEpochMillis), style = MaterialTheme.typography.bodyMedium)
                Text(recurrenceLabel(reminder), style = MaterialTheme.typography.bodySmall)
                if (reminder.note.isNotBlank()) Text(reminder.note, style = MaterialTheme.typography.bodySmall)
            }
            Row {
                TextButton(onClick = { vm.toggleReminder(reminder) }) { Text(if (reminder.enabled) "On" else "Off") }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit") }
                IconButton(onClick = { vm.deleteReminder(reminder) }) { Icon(Icons.Default.Delete, "Delete") }
            }
        }
        Divider()
    }
}

@Composable
private fun NoteRow(note: NoteEntity, vm: RemindersViewModel, onEdit: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onEdit).padding(vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(note.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { vm.deleteNote(note) }) { Icon(Icons.Default.Delete, "Delete") }
        }
        if (note.content.isNotBlank()) Text(note.content, style = MaterialTheme.typography.bodyMedium)
        Divider(Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun ReminderEditorDialog(initial: ReminderEntity?, onSave: (String, String, Long, String, Int) -> Unit, onDismiss: () -> Unit) {
    var title by remember(initial) { mutableStateOf(initial?.title.orEmpty()) }
    var note by remember(initial) { mutableStateOf(initial?.note.orEmpty()) }
    var selectedDate by remember(initial) { mutableLongStateOf(initial?.triggerAtEpochMillis ?: System.currentTimeMillis()) }
    var selectedTime by remember(initial) { mutableLongStateOf(initial?.triggerAtEpochMillis ?: System.currentTimeMillis()) }
    var recurrence by remember(initial) { mutableStateOf(initial?.recurrence ?: "ONCE") }
    var intervalDays by remember(initial) { mutableIntStateOf(initial?.customIntervalDays ?: 1) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }

    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (initial == null) "New reminder" else "Edit reminder") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
            OutlinedTextField(note, { note = it }, label = { Text("Note (optional)") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showDate = true }, modifier = Modifier.weight(1f)) { Text(DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(selectedDate))) }
                OutlinedButton(onClick = { showTime = true }, modifier = Modifier.weight(1f)) { Text(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(selectedTime))) }
            }
            Text("Occurs", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("ONCE" to "Once", "DAILY" to "Daily", "WEEKLY" to "Weekly", "MONTHLY" to "Monthly", "YEARLY" to "Yearly").forEach { (key, label) ->
                    FilterChip(selected = recurrence == key, onClick = { recurrence = key }, label = { Text(label) })
                }
            }
            FilterChip(selected = recurrence == "CUSTOM", onClick = { recurrence = "CUSTOM" }, label = { Text("Custom") })
            if (recurrence == "CUSTOM") {
                OutlinedTextField(intervalDays.toString(), { intervalDays = it.toIntOrNull()?.coerceAtLeast(1) ?: 1 }, label = { Text("Every N days") }, singleLine = true)
            }
        }
    }, confirmButton = { Button(enabled = title.isNotBlank(), onClick = {
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = selectedDate; val time = java.util.Calendar.getInstance().apply { timeInMillis = selectedTime }; set(java.util.Calendar.HOUR_OF_DAY, time.get(java.util.Calendar.HOUR_OF_DAY)); set(java.util.Calendar.MINUTE, time.get(java.util.Calendar.MINUTE)); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }
        onSave(title, note, calendar.timeInMillis, recurrence, intervalDays)
    }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })

    if (showDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(onDismissRequest = { showDate = false }, confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { selectedDate = it }; showDate = false }) { Text("OK") } }) { DatePicker(state) }
    }
    if (showTime) {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = selectedTime }
        val state = rememberTimePickerState(cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE), true)
        TimePickerDialog(onDismiss = { showTime = false }, confirmButton = { TextButton(onClick = { val c = java.util.Calendar.getInstance(); c.set(java.util.Calendar.HOUR_OF_DAY, state.hour); c.set(java.util.Calendar.MINUTE, state.minute); selectedTime = c.timeInMillis; showTime = false }) { Text("OK") } }, title = { Text("Select time") }) { TimeInput(state) }
    }
}

@Composable
private fun NoteEditorDialog(initial: NoteEntity?, onSave: (String, String) -> Unit, onDismiss: () -> Unit) {
    var title by remember(initial) { mutableStateOf(initial?.title.orEmpty()) }
    var content by remember(initial) { mutableStateOf(initial?.content.orEmpty()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (initial == null) "New note" else "Edit note") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
            OutlinedTextField(content, { content = it }, label = { Text("Note") }, minLines = 5)
        }
    }, confirmButton = { Button(enabled = title.isNotBlank() || content.isNotBlank(), onClick = { onSave(title, content) }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

private fun formatDateTime(epoch: Long): String = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epoch))
private fun recurrenceLabel(r: ReminderEntity): String = when (r.recurrence) {
    "DAILY" -> "Every day"
    "WEEKLY" -> "Every week"
    "MONTHLY" -> "Every month"
    "YEARLY" -> "Every year"
    "CUSTOM" -> "Every ${r.customIntervalDays} days"
    else -> "Once"
}
