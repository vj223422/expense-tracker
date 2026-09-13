package com.expensetracker.app.feature.reminders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            shape = androidx.compose.material3.MaterialTheme.shapes.extraLarge,
            color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp, vertical = 30.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ReminderHeader()

                ReminderTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Title",
                    placeholder = "Title",
                    leadingIcon = Icons.Default.Description,
                )
                ReminderTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = "Note (optional)",
                    placeholder = "Note (optional)",
                    leadingIcon = Icons.Default.Edit,
                    minLines = 1,
                )

                SectionTitle("Start")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ReminderDateTimeCard(
                        modifier = Modifier.weight(1f),
                        icon = { Icon(Icons.Default.DateRange, null) },
                        label = "Start date",
                        value = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(selectedStartDate)),
                        supporting = if (isToday(selectedStartDate)) "Today" else null,
                        onClick = { showStartDate = true },
                    )
                    ReminderDateTimeCard(
                        modifier = Modifier.weight(1f),
                        icon = { Icon(Icons.Default.AccessTime, null) },
                        label = "Time",
                        value = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(selectedStartTime)),
                        supporting = null,
                        onClick = { showStartTime = true },
                    )
                }

                SectionTitle("End")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(108.dp),
                    onClick = { showEndDate = true },
                    shape = androidx.compose.material3.MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ReminderIconCircle { Icon(Icons.Default.Event, null) }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "End date",
                                fontSize = 16.sp,
                                lineHeight = 20.sp,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                selectedEndDate?.let { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it)) }
                                    ?: "No end date",
                                fontSize = 22.sp,
                                lineHeight = 28.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Text(
                            "›",
                            fontSize = 38.sp,
                            lineHeight = 38.sp,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    "Leave empty to repeat indefinitely.",
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp),
                )
                dateError?.let {
                    Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, fontSize = 14.sp)
                }

                RepeatsHeader()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    listOf(
                        "ONCE" to "Once",
                        "DAILY" to "Daily",
                        "WEEKLY" to "Weekly",
                        "MONTHLY" to "Monthly",
                    ).forEach { (key, label) ->
                        RepeatCard(label = label, selected = recurrence == key) {
                            recurrence = key
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(112.dp),
                    onClick = { recurrence = "CUSTOM" },
                    shape = androidx.compose.material3.MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = if (recurrence == "CUSTOM") {
                            androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
                        } else {
                            androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(58.dp),
                            shape = CircleShape,
                            color = if (recurrence == "CUSTOM") {
                                androidx.compose.material3.MaterialTheme.colorScheme.primary
                            } else {
                                androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                            },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Settings,
                                    null,
                                    modifier = Modifier.size(30.dp),
                                    tint = if (recurrence == "CUSTOM") {
                                        androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        }
                        Spacer(Modifier.width(18.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Custom",
                                fontSize = 20.sp,
                                lineHeight = 25.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "Set your own repeating schedule",
                                fontSize = 16.sp,
                                lineHeight = 21.sp,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            "›",
                            fontSize = 38.sp,
                            lineHeight = 38.sp,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (recurrence == "CUSTOM") {
                    OutlinedTextField(
                        value = intervalDays.toString(),
                        onValueChange = {
                            intervalDays = (it.toIntOrNull()?.coerceAtLeast(1) ?: 1).toLong()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Repeat every N days") },
                        singleLine = true,
                    )
                }

                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = CircleShape,
                        border = BorderStroke(2.dp, androidx.compose.material3.MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text("Cancel", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        enabled = title.isNotBlank(),
                        onClick = {
                            val calendar = Calendar.getInstance().apply {
                                timeInMillis = selectedStartDate
                                val time = Calendar.getInstance().apply { timeInMillis = selectedStartTime }
                                set(Calendar.HOUR_OF_DAY, time.get(Calendar.HOUR_OF_DAY))
                                set(Calendar.MINUTE, time.get(Calendar.MINUTE))
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val start = calendar.timeInMillis
                            val end = selectedEndDate
                            if (end != null && end < start.startOfDay()) {
                                dateError = "End date cannot be before the start date"
                            } else {
                                dateError = null
                                onSave(title.trim(), note.trim(), start, end, recurrence, intervalDays.toInt())
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Text("Save reminder", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (showStartDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = selectedStartDate)
        DatePickerDialog(
            onDismissRequest = { showStartDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        selectedStartDate = it
                        if (selectedEndDate != null && selectedEndDate!! < it.startOfDay()) {
                            selectedEndDate = it
                        }
                    }
                    showStartDate = false
                }) { Text("Done") }
            },
            dismissButton = { TextButton(onClick = { showStartDate = false }) { Text("Cancel") } },
        ) { DatePicker(state) }
    }

    if (showEndDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = selectedEndDate ?: selectedStartDate)
        DatePickerDialog(
            onDismissRequest = { showEndDate = false },
            confirmButton = {
                Row {
                    if (selectedEndDate != null) {
                        TextButton(onClick = {
                            selectedEndDate = null
                            dateError = null
                            showEndDate = false
                        }) { Text("Clear") }
                    }
                    TextButton(onClick = {
                        state.selectedDateMillis?.let {
                            if (it < selectedStartDate.startOfDay()) {
                                dateError = "End date cannot be before the start date"
                            } else {
                                selectedEndDate = it
                                dateError = null
                                showEndDate = false
                            }
                        }
                    }) { Text("Done") }
                }
            },
            dismissButton = { TextButton(onClick = { showEndDate = false }) { Text("Cancel") } },
        ) { DatePicker(state) }
    }

    if (showStartTime) {
        val calendar = Calendar.getInstance().apply { timeInMillis = selectedStartTime }
        val state = rememberTimePickerState(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false)
        Dialog(onDismissRequest = { showStartTime = false }) {
            Surface(
                shape = androidx.compose.material3.MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
            ) {
                Column(
                    Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Text("Select start time", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
                    TimeInput(state)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showStartTime = false }) { Text("Cancel") }
                        TextButton(onClick = {
                            selectedStartTime = Calendar.getInstance().apply {
                                timeInMillis = selectedStartTime
                                set(Calendar.HOUR_OF_DAY, state.hour)
                                set(Calendar.MINUTE, state.minute)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                            showStartTime = false
                        }) { Text("Done") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReminderIconCircle(size = 104.dp, iconSize = 48.dp) {
            Icon(Icons.Default.NotificationsActive, null)
        }
        Spacer(Modifier.width(28.dp))
        Column {
            Text(
                "New reminder",
                fontSize = 31.sp,
                lineHeight = 37.sp,
                fontWeight = FontWeight.SemiBold,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "Set a reminder for anything",
                fontSize = 22.sp,
                lineHeight = 28.sp,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReminderTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (minLines == 1) 90.dp else 96.dp),
        placeholder = { Text(placeholder, fontSize = 21.sp) },
        label = if (value.isNotEmpty()) ({ Text(label) }) else null,
        leadingIcon = {
            Icon(
                leadingIcon,
                null,
                modifier = Modifier.size(30.dp),
                tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        singleLine = minLines == 1,
        minLines = minLines,
        maxLines = if (minLines == 1) 1 else 3,
        shape = androidx.compose.material3.MaterialTheme.shapes.large,
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.outline,
            focusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
        ),
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        fontSize = 23.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold,
        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
    )
}

@Composable
private fun ReminderDateTimeCard(
    modifier: Modifier,
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    supporting: String?,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.height(140.dp),
        onClick = onClick,
        shape = androidx.compose.material3.MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReminderIconCircle { icon() }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    label,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    value,
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                )
                supporting?.let {
                    Text(
                        it,
                        fontSize = 16.sp,
                        lineHeight = 21.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                "›",
                fontSize = 38.sp,
                lineHeight = 38.sp,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReminderIconCircle(
    size: androidx.compose.ui.unit.Dp = 58.dp,
    iconSize: androidx.compose.ui.unit.Dp = 30.dp,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.size(iconSize), contentAlignment = Alignment.Center) { content() }
        }
    }
}

@Composable
private fun RepeatsHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReminderIconCircle(size = 68.dp, iconSize = 36.dp) {
            Icon(Icons.Default.Refresh, null)
        }
        Spacer(Modifier.width(24.dp))
        Column {
            Text(
                "Repeats",
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "Choose how often this reminder occurs",
                fontSize = 16.sp,
                lineHeight = 21.sp,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RepeatCard(label: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(205.dp)
            .height(132.dp),
        onClick = onClick,
        shape = androidx.compose.material3.MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                androidx.compose.material3.MaterialTheme.colorScheme.primary
            } else {
                androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh
            },
        ),
        border = if (selected) {
            BorderStroke(2.dp, androidx.compose.material3.MaterialTheme.colorScheme.primary)
        } else null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = if (selected) {
                    androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
                } else {
                    androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (selected) {
                    androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
                } else {
                    androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    when (label) {
                        "Once" -> Text("○", fontSize = 36.sp, lineHeight = 38.sp)
                        "Daily" -> Text("☼", fontSize = 34.sp, lineHeight = 36.sp)
                        "Weekly", "Monthly" -> Icon(Icons.Default.DateRange, null, modifier = Modifier.size(30.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                label,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) {
                    androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
                } else {
                    androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

private fun isToday(epochMillis: Long): Boolean {
    val selected = Calendar.getInstance().apply { timeInMillis = epochMillis }
    val today = Calendar.getInstance()
    return selected.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
        selected.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
}

private fun Long.startOfDay(): Long = java.time.Instant.ofEpochMilli(this)
    .atZone(java.time.ZoneId.systemDefault())
    .toLocalDate()
    .atStartOfDay(java.time.ZoneId.systemDefault())
    .toInstant()
    .toEpochMilli()
