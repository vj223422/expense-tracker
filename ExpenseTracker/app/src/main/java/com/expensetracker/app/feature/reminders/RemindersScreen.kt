package com.expensetracker.app.feature.reminders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
        ModernReminderEditorDialog(editingReminder, { title, note, startAt, endDate, recurrence, days ->
            viewModel.saveReminder(editingReminder?.id, title, note, startAt, endDate, recurrence, days)
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
                Text("Starts ${formatDateTime(reminder.triggerAtEpochMillis)}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    reminder.endDateEpochMillis?.let { "Ends ${formatDate(it)}" } ?: "No end date",
                    style = MaterialTheme.typography.bodySmall,
                )
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
private fun formatDate(epoch: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(epoch))
private fun recurrenceLabel(r: ReminderEntity): String = when (r.recurrence) {
    "DAILY" -> "Every day"
    "WEEKLY" -> "Every week"
    "MONTHLY" -> "Every month"
    "YEARLY" -> "Every year"
    "CUSTOM" -> "Every ${r.customIntervalDays} days"
    else -> "Once"
}
