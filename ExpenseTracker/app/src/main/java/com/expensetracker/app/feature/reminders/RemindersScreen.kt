package com.expensetracker.app.feature.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
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
                            onDelete = viewModel::deleteWaterIntake,
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
                Text("Active reminders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
    onDelete: (WaterIntakeEntity) -> Unit,
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
            kotlin.math.round(
                (it.endTimeMinutes - it.startTimeMinutes).toDouble() / (it.reminderCount - 1)
            ).toInt()
        } else 0
    } ?: 0

    val cardColor = Color(0xFF0D2237)
    val panelColor = Color(0xFF102B42)
    val cyan = Color(0xFF08C9F4)
    val brightBlue = Color(0xFF0B91FF)
    val muted = Color(0xFF9FB3C7)
    val textColor = Color(0xFFEAF3FF)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(1.dp, Color(0xFF183B56)),
    ) {
        Column(
            Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF183B59)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.WaterDrop, null, tint = cyan, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.size(9.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        "Hydration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        maxLines = 1,
                    )
                    Text(
                        if (settings == null) "Set up your daily hydration" else "${settings.intakePerReminderMl} ml per reminder",
                        style = MaterialTheme.typography.bodySmall,
                        color = muted,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.size(8.dp))
                Switch(
                    checked = settings?.enabled == true,
                    onCheckedChange = onToggle,
                )
            }

            if (settings != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(0.95f)
                            .aspectRatio(1f)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 8.dp,
                            color = Color(0xFF24455F),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                        )
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 8.dp,
                            color = cyan,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                "${(progress * 100).toInt()}%",
                                fontSize = 22.sp,
                                lineHeight = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textColor,
                            )
                            Text("today", fontSize = 12.sp, color = muted)
                            Spacer(Modifier.size(5.dp))
                            Icon(Icons.Default.WaterDrop, null, tint = cyan, modifier = Modifier.size(18.dp))
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1.05f),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Text(
                            "${todayTotal} ml",
                            fontSize = 24.sp,
                            lineHeight = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor,
                            maxLines = 2,
                        )
                        Text(
                            "of ${goal} ml goal",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = muted,
                            maxLines = 2,
                        )
                        ScheduleDivider(horizontal = true)
                        Text(
                            "${remaining} ml",
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            color = cyan,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        Text("remaining", fontSize = 13.sp, color = muted, maxLines = 1)
                        ScheduleDivider(horizontal = true)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                null,
                                tint = Color(0xFF73B9FF),
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.size(7.dp))
                            Text(
                                "${completedReminders} / ${reminderCount} completed",
                                fontSize = 13.sp,
                                lineHeight = 17.sp,
                                color = textColor,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = panelColor),
                    border = BorderStroke(1.dp, Color(0xFF1C405C)),
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                null,
                                tint = Color(0xFF79B7FF),
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.size(9.dp))
                            Text(
                                "Today's plan",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                            )
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFF1A3A55))
                                    .clickable(onClick = onEdit)
                                    .padding(horizontal = 11.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "View schedule",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textColor,
                                    maxLines = 1,
                                )
                                Spacer(Modifier.size(2.dp))
                                Icon(Icons.Default.ChevronRight, null, tint = textColor, modifier = Modifier.size(14.dp))
                            }
                        }

                        Spacer(Modifier.size(9.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            WaterScheduleCell(
                                Icons.Default.PlayArrow,
                                Color(0xFF00E6B2),
                                "START",
                                formatWaterTime(settings.startTimeMinutes),
                                Modifier.weight(1f),
                            )
                            ScheduleDivider()
                            WaterScheduleCell(
                                Icons.Default.AccessTime,
                                Color(0xFF79B7FF),
                                "EVERY",
                                formatWaterInterval(frequencyMinutes),
                                Modifier.weight(1f),
                            )
                            ScheduleDivider()
                            WaterScheduleCell(
                                Icons.Default.Stop,
                                Color(0xFFFF647E),
                                "END",
                                formatWaterTime(settings.endTimeMinutes),
                                Modifier.weight(1f),
                            )
                            ScheduleDivider()
                            WaterScheduleCell(
                                Icons.Default.Notifications,
                                Color(0xFF79B7FF),
                                "TOTAL",
                                "${reminderCount}",
                                Modifier.weight(1f),
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Today's water logs",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${todayTotal} ml total",
                            fontSize = 10.sp,
                            color = muted,
                        )
                    }

                    val todayLogs = intake.filter {
                        Instant.ofEpochMilli(it.drankAtEpochMillis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate() == today
                    }

                    if (todayLogs.isEmpty()) {
                        Text(
                            "No water logged today",
                            fontSize = 11.sp,
                            color = muted,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    } else {
                        todayLogs.forEach { log ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = panelColor),
                                border = BorderStroke(1.dp, Color(0xFF1C405C)),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 11.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF183B59)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Default.LocalDrink,
                                            null,
                                            tint = cyan,
                                            modifier = Modifier.size(15.dp),
                                        )
                                    }
                                    Spacer(Modifier.size(9.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "${log.amountMl} ml",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor,
                                            maxLines = 1,
                                        )
                                        Text(
                                            formatWaterLogTime(log.drankAtEpochMillis),
                                            fontSize = 11.sp,
                                            color = muted,
                                            maxLines = 1,
                                        )
                                    }
                                    Text(
                                        if (log.source == "REMINDER") "Reminder" else "Manual",
                                        fontSize = 10.sp,
                                        color = muted,
                                        maxLines = 1,
                                    )
                                    IconButton(
                                        onClick = { onDelete(log) },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete water log",
                                            tint = Color(0xFFFF7187),
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    WaterInsight(
                        "Today",
                        todayTotal,
                        goal,
                        Icons.Default.WaterDrop,
                        cyan,
                        Modifier.weight(1f),
                    )
                    WaterInsight(
                        "7 days",
                        weekTotal / 7,
                        goal,
                        Icons.Default.BarChart,
                        Color(0xFFA78BFA),
                        Modifier.weight(1f),
                    )
                    WaterInsight(
                        "Month",
                        monthTotal / today.lengthOfMonth(),
                        goal,
                        Icons.Default.CalendarMonth,
                        Color(0xFF00E6B2),
                        Modifier.weight(1f),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = onAdd,
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = brightBlue,
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(Icons.Default.LocalDrink, null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.size(7.dp))
                        Text(
                            "Log ${settings.intakePerReminderMl} ml",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                    androidx.compose.material3.OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .weight(0.72f)
                            .height(64.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF9BCBFF),
                        ),
                        border = BorderStroke(1.dp, Color(0xFF2A506D)),
                    ) {
                        Icon(Icons.Default.Settings, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Edit", fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Set a daily goal and hydration window. We'll automatically calculate the number of reminders and space them evenly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = muted,
                    )
                    Button(
                        onClick = onEdit,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text("Create water plan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleDivider(horizontal: Boolean = false) {
    Box(
        modifier = if (horizontal) {
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF31516A))
        } else {
            Modifier
                .width(1.dp)
                .height(42.dp)
                .background(Color(0xFF31516A))
        }
    )
}

@Composable
private fun WaterScheduleCell(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(14.dp))
                Spacer(Modifier.size(4.dp))
                Text(
                    label,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    color = Color(0xFFA6B8CA),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                value,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEAF3FF),
                maxLines = 2,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun WaterInsight(
    label: String,
    amount: Int,
    goal: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val percent = ((amount * 100) / goal.coerceAtLeast(1)).coerceIn(0, 100)
    Card(
        modifier = modifier.height(208.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF112D45)),
        border = BorderStroke(1.dp, Color(0xFF1C405C)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(5.dp))
                Text(
                    label,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = Color(0xFFA8BACC),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    softWrap = false,
                )
                Icon(
                    Icons.Default.ChevronRight,
                    null,
                    tint = Color(0xFF8DA8BF),
                    modifier = Modifier.size(14.dp),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    "${amount} ml",
                    fontSize = 21.sp,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFEAF3FF),
                    maxLines = 2,
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF2C4A61)),
            ) {
                if (percent > 0) {
                    Box(
                        Modifier
                            .fillMaxWidth(percent / 100f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(accent),
                    )
                }
            }
            Text(
                "${percent}%",
                fontSize = 11.sp,
                lineHeight = 13.sp,
                color = Color(0xFFDCE8F4),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
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

private fun formatWaterLogTime(epochMillis: Long): String =
    DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(Date(epochMillis))

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
                        Text(if (noteSearch.isBlank()) "No notes yet" else "No notes found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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