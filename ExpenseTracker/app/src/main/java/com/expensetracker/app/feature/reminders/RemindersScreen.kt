package com.expensetracker.app.feature.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.Switch
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.data.entity.NoteEntity
import com.expensetracker.app.data.entity.ReminderEntity
import com.expensetracker.app.data.entity.WaterIntakeEntity
import com.expensetracker.app.data.entity.WaterSettingsEntity
import org.koin.androidx.compose.koinViewModel
import java.text.DateFormat
import java.util.Date
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate
import java.util.Locale

@Composable
fun RemindersScreen(viewModel: RemindersViewModel = koinViewModel()) {
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val waterSettings by viewModel.waterSettings.collectAsStateWithLifecycle()
    val waterIntake by viewModel.waterIntake.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showReminderEditor by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<ReminderEntity?>(null) }
    var showNoteEditor by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var showWaterSetup by remember { mutableStateOf(false) }

    val filteredReminders = remember(reminders, searchQuery) {
        val query = searchQuery.trim()
        if (query.isBlank()) reminders else reminders.filter {
            it.title.contains(query, ignoreCase = true) || it.note.contains(query, ignoreCase = true) || recurrenceLabel(it).contains(query, ignoreCase = true)
        }
    }

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(tab) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            totalDrag += dragAmount
                            if (totalDrag < -100f && tab < 2) {
                                tab += 1
                                change.consume()
                            } else if (totalDrag > 100f && tab > 0) {
                                tab -= 1
                                change.consume()
                            }
                        },
                        onDragEnd = { totalDrag = 0f },
                        onDragCancel = { totalDrag = 0f },
                    )
                },
        ) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Reminders") }, icon = { Icon(Icons.Default.Notifications, null) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Notes") }, icon = { Icon(Icons.Default.Notes, null) })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Water") }, icon = { Text("💧") })
            }
            if (tab == 0) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    singleLine = true,
                    placeholder = { Text("Search reminders") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, contentDescription = "Clear search") }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                )
                ReminderContent(filteredReminders, viewModel, onAdd = { editingReminder = null; showReminderEditor = true }, onEdit = { editingReminder = it; showReminderEditor = true })
            } else if (tab == 1) {
                NotesContent(notes, viewModel, onAdd = { editingNote = null; showNoteEditor = true }, onEdit = { editingNote = it; showNoteEditor = true })
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp),
                ) {
                    item {
                        WaterReminderCard(
                            settings = waterSettings,
                            intake = waterIntake,
                            onToggle = { enabled ->
                                if (enabled && waterSettings == null) showWaterSetup = true else viewModel.toggleWaterReminder(enabled)
                            },
                            onEdit = { showWaterSetup = true },
                            onAdd = { viewModel.addWaterIntake(waterSettings?.intakePerReminderMl ?: 300) },
                        )
                    }
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
    if (showWaterSetup) {
        WaterSetupDialog(
            initial = waterSettings,
            onSave = { goal, start, end, amount -> viewModel.saveWaterSettings(goal, start, end, amount); showWaterSetup = false },
            onDismiss = { showWaterSetup = false },
        )
    }
    if (showNoteEditor) {
        NoteEditorDialog(editingNote, { title, content ->
            viewModel.saveNote(editingNote?.id, title, content)
            showNoteEditor = false
        }, { showNoteEditor = false })
    }
}

@Composable
private fun ReminderContent(reminders: List<ReminderEntity>, vm: RemindersViewModel, onAdd: () -> Unit, onEdit: (ReminderEntity) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.primaryContainer).clickable(onClick = onAdd).padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    Spacer(Modifier.size(14.dp))
                    Text("Add reminder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                }
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Active reminders", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("${reminders.count { it.enabled }} reminders", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (reminders.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(34.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.size(8.dp))
                        Text("No reminders found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Try a different search or tap + to add one", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(reminders, key = { it.id }) { reminder -> ModernReminderCard(reminder, vm, onEdit) }
            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(34.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.size(8.dp))
                        Text("No more reminders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Tap + to add a new reminder", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernReminderCard(reminder: ReminderEntity, vm: RemindersViewModel, onEdit: (ReminderEntity) -> Unit) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Icon(if (reminder.title.contains("birthday", true) || reminder.note.contains("gift", true)) Icons.Default.Notes else Icons.Default.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(reminder.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(recurrenceLabel(reminder), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = reminder.enabled, onCheckedChange = { vm.toggleReminder(reminder) })
                IconButton(onClick = { onEdit(reminder) }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(20.dp)) }
                IconButton(onClick = { vm.deleteReminder(reminder) }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(20.dp)) }
            }
            Divider(Modifier.padding(vertical = 10.dp))
            ReminderDetailRow(Icons.Default.CalendarMonth, "Starts", formatDateTime(reminder.triggerAtEpochMillis))
            ReminderDetailRow(Icons.Default.CalendarMonth, "Ends", reminder.endDateEpochMillis?.let(::formatDate) ?: "No end date")
            ReminderDetailRow(Icons.Default.CalendarMonth, "Repeat", recurrenceLabel(reminder))
            if (reminder.note.isNotBlank()) ReminderDetailRow(Icons.Default.Notes, "Note", reminder.note)
        }
    }
}

@Composable
private fun ReminderDetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(width = 82.dp, height = 24.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}


@Composable
private fun WaterReminderCard(
    settings: WaterSettingsEntity?,
    intake: List<WaterIntakeEntity>,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onAdd: () -> Unit,
) {
    val goal = settings?.dailyGoalMl ?: 4000
    val today = LocalDate.now()
    val todayTotal = intake.filter {
        Instant.ofEpochMilli(it.drankAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate() == today
    }.sumOf { it.amountMl }
    val weekStart = today.minusDays(6)
    val monthStart = today.withDayOfMonth(1)
    val weekTotal = intake.filter {
        Instant.ofEpochMilli(it.drankAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate() >= weekStart
    }.sumOf { it.amountMl }
    val monthTotal = intake.filter {
        Instant.ofEpochMilli(it.drankAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate() >= monthStart
    }.sumOf { it.amountMl }

    val progress = (todayTotal.toFloat() / goal.coerceAtLeast(1)).coerceIn(0f, 1f)
    val remaining = (goal - todayTotal).coerceAtLeast(0)
    val reminderCount = settings?.reminderCount ?: 0
    val completedReminders = if (settings != null && settings.intakePerReminderMl > 0) {
        (todayTotal / settings.intakePerReminderMl).coerceAtMost(reminderCount)
    } else 0
    val frequencyMinutes = settings?.let {
        if (it.reminderCount > 1) {
            kotlin.math.round((it.endTimeMinutes - it.startTimeMinutes).toDouble() / (it.reminderCount - 1)).toInt()
        } else 0
    } ?: 0

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("💧", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Hydration", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        if (settings == null) "Create your daily water plan" else "${settings.intakePerReminderMl} ml per reminder",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = settings?.enabled == true, onCheckedChange = onToggle)
            }

            if (settings != null) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(124.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 10.dp,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 10.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("today", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.size(18.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("${todayTotal} ml", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("of ${goal} ml goal", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            if (remaining > 0) "${remaining} ml remaining" else "Daily goal completed 🎉",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (reminderCount > 0) {
                            Text(
                                "${completedReminders} of ${reminderCount} reminders completed",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    ),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Today's plan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(
                                "${reminderCount} reminders",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            WaterScheduleItem("START", formatWaterTime(settings.startTimeMinutes), Modifier.weight(1f))
                            WaterScheduleItem("EVERY", formatWaterInterval(frequencyMinutes), Modifier.weight(1f))
                            WaterScheduleItem("END", formatWaterTime(settings.endTimeMinutes), Modifier.weight(1f))
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WaterInsight("Today", todayTotal, goal, Modifier.weight(1f))
                    WaterInsight("7 days", weekTotal / 7, goal, Modifier.weight(1f))
                    WaterInsight("Month", monthTotal / today.lengthOfMonth(), goal, Modifier.weight(1f))
                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onAdd,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(17.dp),
                    ) {
                        Text("Log ${settings.intakePerReminderMl} ml", fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onEdit) { Text("Edit") }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Set a daily goal and hydration window. We'll automatically calculate the number of reminders and space them evenly.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = onEdit, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(17.dp)) {
                        Text("Create water plan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun WaterScheduleItem(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WaterInsight(label: String, amount: Int, goal: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f)),
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(amount.toString() + " ml", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                if (amount >= goal) "Goal met" else ((amount * 100) / goal.coerceAtLeast(1)).toString() + "%",
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun WaterSetupDialog(
    initial: WaterSettingsEntity?,
    onSave: (Int, Int, Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var goal by remember(initial) { mutableStateOf(((initial?.dailyGoalMl ?: 4000) / 1000f).toString()) }
    var amount by remember(initial) { mutableStateOf((initial?.intakePerReminderMl ?: 500).toString()) }
    var startMinutes by remember(initial) { mutableIntStateOf(initial?.startTimeMinutes ?: 8 * 60) }
    var endMinutes by remember(initial) { mutableIntStateOf(initial?.endTimeMinutes ?: 20 * 60) }

    val goalMl = ((goal.toFloatOrNull() ?: 0f) * 1000).toInt()
    val intakeMl = amount.toIntOrNull() ?: 0
    val reminderCount = if (goalMl > 0 && intakeMl > 0 && goalMl % intakeMl == 0) goalMl / intakeMl else 0
    val durationMinutes = endMinutes - startMinutes
    val frequencyMinutes = if (reminderCount > 1 && durationMinutes > 0) {
        kotlin.math.round(durationMinutes.toDouble() / (reminderCount - 1)).toInt()
    } else 0
    val valid = goalMl >= 250 &&
        intakeMl in 50..2000 &&
        reminderCount > 0 &&
        (reminderCount == 1 || durationMinutes > 0)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Column {
                Text(if (initial == null) "Set water schedule" else "Edit water schedule", fontWeight = FontWeight.Bold)
                Text(
                    "Frequency is calculated automatically",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = goal,
                    onValueChange = { goal = it },
                    label = { Text("Daily goal (litres)") },
                    leadingIcon = { Text("💧") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter(Char::isDigit) },
                    label = { Text("Intake per reminder (ml)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TimePickerField(
                        label = "Start time",
                        minutes = startMinutes,
                        onTimeSelected = { startMinutes = it },
                        modifier = Modifier.weight(1f),
                    )
                    TimePickerField(
                        label = "End time",
                        minutes = endMinutes,
                        onTimeSelected = { endMinutes = it },
                        modifier = Modifier.weight(1f),
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        if (reminderCount > 0 && (reminderCount == 1 || durationMinutes > 0)) {
                            Text(
                                "${reminderCount} reminders",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                if (reminderCount == 1) {
                                    "One reminder at ${formatWaterTime(startMinutes)}"
                                } else {
                                    "Every ${formatWaterInterval(frequencyMinutes)} from ${formatWaterTime(startMinutes)} to ${formatWaterTime(endMinutes)}"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                "Total: ${goalMl} ml",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Text(
                                if (goalMl > 0 && intakeMl > 0 && goalMl % intakeMl != 0) {
                                    "Goal must divide evenly by the intake amount."
                                } else {
                                    "Choose a valid goal, intake amount and time window."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = {
                    onSave(goalMl, startMinutes, endMinutes, intakeMl)
                },
                shape = RoundedCornerShape(14.dp),
            ) { Text("Save & enable") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun TimePickerField(
    label: String,
    minutes: Int,
    onTimeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = formatWaterTime(minutes),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        modifier = modifier.clickable { showPicker = true },
        trailingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Select $label") },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
    )

    if (showPicker) {
        val state = rememberTimePickerState(
            initialHour = (minutes / 60).coerceIn(0, 23),
            initialMinute = (minutes % 60).coerceIn(0, 59),
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text(label, fontWeight = FontWeight.Bold) },
            text = { TimePicker(state = state) },
            confirmButton = {
                Button(
                    onClick = {
                        onTimeSelected(state.hour * 60 + state.minute)
                        showPicker = false
                    },
                    shape = RoundedCornerShape(14.dp),
                ) { Text("Set time") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } },
        )
    }
}

private fun formatWaterTime(minutes: Int): String {
    val hour = (minutes / 60).coerceIn(0, 23)
    val minute = (minutes % 60).coerceIn(0, 59)
    val hour12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val period = if (hour < 12) "AM" else "PM"
    return String.format(Locale.getDefault(), "%d:%02d %s", hour12, minute, period)
}

private fun formatWaterInterval(minutes: Int): String {
    if (minutes <= 0) return "once"
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
}

@Composable
private fun NotesContent(notes: List<NoteEntity>, vm: RemindersViewModel, onAdd: () -> Unit, onEdit: (NoteEntity) -> Unit) {
    var noteSearch by remember { mutableStateOf("") }
    val filteredNotes = remember(notes, noteSearch) {
        val query = noteSearch.trim()
        if (query.isBlank()) notes else notes.filter {
            it.title.contains(query, ignoreCase = true) || it.content.contains(query, ignoreCase = true)
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Your notes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        if (notes.isEmpty()) "Keep important thoughts in one place" else "${notes.size} ${if (notes.size == 1) "note" else "notes"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Notes, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
            }
        }
        item {
            OutlinedTextField(
                value = noteSearch,
                onValueChange = { noteSearch = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Search notes") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (noteSearch.isNotEmpty()) {
                        IconButton(onClick = { noteSearch = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.primary).clickable(onClick = onAdd).padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Create a note", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    Text("Write something you want to remember", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f))
                }
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
        if (filteredNotes.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                ) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 34.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Notes, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        }
                        Spacer(Modifier.size(14.dp))
                        Text(if (noteSearch.isBlank()) "No notes yet" else "No notes found", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.size(5.dp))
                        Text(
                            if (noteSearch.isBlank()) "Tap Create a note to add your first note." else "Try a different search term.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            items(filteredNotes, key = { it.id }) { note ->
                Card(
                    Modifier.fillMaxWidth().clickable { onEdit(note) },
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Notes, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(23.dp))
                        }
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(note.title.ifBlank { "Untitled note" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (note.content.isNotBlank()) {
                                Spacer(Modifier.size(5.dp))
                                Text(note.content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
                            } else {
                                Spacer(Modifier.size(5.dp))
                                Text("Empty note", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        IconButton(onClick = { onEdit(note) }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.Edit, "Edit note", modifier = Modifier.size(20.dp)) }
                        IconButton(onClick = { vm.deleteNote(note) }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.Delete, "Delete note", modifier = Modifier.size(20.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteEditorDialog(initial: NoteEntity?, onSave: (String, String) -> Unit, onDismiss: () -> Unit) {
    var title by remember(initial) { mutableStateOf(initial?.title.orEmpty()) }
    var content by remember(initial) { mutableStateOf(initial?.content.orEmpty()) }
    val canSave = title.isNotBlank() || content.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Notes, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.size(12.dp))
                Column {
                    Text(if (initial == null) "New note" else "Edit note", fontWeight = FontWeight.Bold)
                    Text("Capture it before you forget", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title") },
                    placeholder = { Text("Give your note a title") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { if (it.length <= 2000) content = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Note") },
                    placeholder = { Text("Write your note here...") },
                    leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    minLines = 7,
                    maxLines = 10,
                    supportingText = { Text("${content.length}/2000") },
                    shape = RoundedCornerShape(16.dp),
                )
            }
        },
        confirmButton = {
            Button(enabled = canSave, onClick = { onSave(title.trim(), content.trim()) }, shape = RoundedCornerShape(14.dp)) {
                Text("Save note")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
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
