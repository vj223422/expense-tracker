package com.expensetracker.app.feature.reminders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.expensetracker.app.data.entity.ReminderEntity
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

private val ReminderBlue = Color(0xFF1976F3)
private val ReminderBlueContainer = Color(0xFF0D5BD7)
private val ReminderIconTint = Color(0xFF8FFFE5)

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

    fun saveReminder() {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = selectedStartDate
            val time = Calendar.getInstance().apply { timeInMillis = selectedStartTime }
            set(Calendar.HOUR_OF_DAY, time.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, time.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis
        if (selectedEndDate != null && selectedEndDate!! < start.startOfDay()) {
            dateError = "End date cannot be before the start date"
        } else {
            dateError = null
            onSave(title.trim(), note.trim(), start, selectedEndDate, recurrence, intervalDays.toInt())
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.96f)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            shape = androidx.compose.material3.MaterialTheme.shapes.extraLarge,
            color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.fillMaxWidth().fillMaxHeight()) {
                // Compact app bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = if (initial == null) "Add Reminder" else "Edit Reminder",
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                        fontSize = 23.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                    )
                    TextButton(
                        onClick = { if (title.isNotBlank()) saveReminder() },
                        enabled = title.isNotBlank(),
                    ) {
                        Text("Save", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                androidx.compose.material3.HorizontalDivider()

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ReminderTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = "Title",
                        placeholder = "Enter reminder title",
                        leadingIcon = Icons.Default.Description,
                    )
                    ReminderTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = "Note (optional)",
                        placeholder = "Add a note...",
                        leadingIcon = Icons.Default.Edit,
                    )

                    Text(
                        "Start",
                        fontSize = 22.sp,
                        lineHeight = 27.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp),
                    )

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ReminderDateTimeCard(
                            modifier = Modifier.weight(1f),
                            icon = { Icon(Icons.Default.DateRange, null, tint = ReminderBlue) },
                            label = "Start date",
                            value = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(selectedStartDate)),
                            supporting = if (isToday(selectedStartDate)) "Today" else null,
                            onClick = { showStartDate = true },
                        )
                        ReminderDateTimeCard(
                            modifier = Modifier.weight(1f),
                            icon = { Icon(Icons.Default.AccessTime, null, tint = ReminderBlue) },
                            label = "Start time",
                            value = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(selectedStartTime)),
                            supporting = null,
                            onClick = { showStartTime = true },
                        )
                    }

                    Text(
                        "End (optional)",
                        fontSize = 22.sp,
                        lineHeight = 27.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp),
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth().height(78.dp),
                        onClick = { showEndDate = true },
                        shape = androidx.compose.material3.MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ReminderIconCircle(
                                size = 42.dp,
                                iconSize = 22.dp,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                            ) {
                                Icon(Icons.Default.Event, null, tint = ReminderBlue)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "End date",
                                    fontSize = 13.sp,
                                    lineHeight = 16.sp,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    selectedEndDate?.let {
                                        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it))
                                    } ?: "No end date",
                                    fontSize = 17.sp,
                                    lineHeight = 21.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                            Text(
                                "›",
                                fontSize = 28.sp,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Text(
                        "Leave empty to repeat indefinitely.",
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 2.dp),
                    )

                    dateError?.let {
                        Text(
                            it,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            lineHeight = 17.sp,
                        )
                    }

                    Text(
                        "Repeats",
                        fontSize = 22.sp,
                        lineHeight = 27.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Text(
                        "Choose how often this reminder occurs",
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        listOf(
                            "ONCE" to "Once",
                            "DAILY" to "Daily",
                            "WEEKLY" to "Weekly",
                            "MONTHLY" to "Monthly",
                        ).forEach { (key, label) ->
                            RepeatCard(
                                modifier = Modifier.weight(1f),
                                label = label,
                                selected = recurrence == key,
                                blue = ReminderBlue,
                                onClick = { recurrence = key },
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth().height(74.dp),
                        onClick = { recurrence = "CUSTOM" },
                        shape = androidx.compose.material3.MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = if (recurrence == "CUSTOM") {
                                ReminderBlue.copy(alpha = 0.10f)
                            } else {
                                androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                        ),
                        border = if (recurrence == "CUSTOM") BorderStroke(1.5.dp, ReminderBlue) else null,
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ReminderIconCircle(
                                size = 40.dp,
                                iconSize = 22.dp,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                            ) {
                                Icon(Icons.Default.Refresh, null, tint = ReminderBlue)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Custom",
                                    fontSize = 17.sp,
                                    lineHeight = 21.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "Set your own repeating schedule",
                                    fontSize = 13.sp,
                                    lineHeight = 17.sp,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                            Text(
                                "›",
                                fontSize = 28.sp,
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
                }

                // Fixed action area: it never gets pushed off-screen by the form.
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Button(
                        enabled = title.isNotBlank(),
                        onClick = ::saveReminder,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = androidx.compose.material3.MaterialTheme.shapes.large,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ReminderBlue,
                            contentColor = Color.White,
                            disabledContainerColor = ReminderBlue.copy(alpha = 0.45f),
                            disabledContentColor = Color.White.copy(alpha = 0.75f),
                        ),
                    ) {
                        Icon(Icons.Default.NotificationsActive, null, modifier = Modifier.size(21.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (initial == null) "Save Reminder" else "Save Changes",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                        )
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
                        if (selectedEndDate != null && selectedEndDate!! < it.startOfDay()) selectedEndDate = it
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
        val state = rememberTimePickerState(
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false,
        )
        Dialog(onDismissRequest = { showStartTime = false }) {
            Surface(
                shape = androidx.compose.material3.MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
            ) {
                Column(
                    Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Text(
                        "Select start time",
                        style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                    )
                    TimeInput(state)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
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
private fun ReminderIconCircle(
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    color: Color,
    content: @Composable () -> Unit,
) {
    Surface(modifier = Modifier.size(size), shape = CircleShape, color = color) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(iconSize), contentAlignment = Alignment.Center) { content() }
        }
    }
}

@Composable
private fun ReminderTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().height(74.dp),
        placeholder = { Text(placeholder, fontSize = 19.sp) },
        leadingIcon = { Icon(leadingIcon, null, modifier = Modifier.size(24.dp)) },
        singleLine = true,
        shape = androidx.compose.material3.MaterialTheme.shapes.large,
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
        modifier = modifier.height(96.dp),
        onClick = onClick,
        shape = androidx.compose.material3.MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            ReminderIconCircle(size = 48.dp, iconSize = 24.dp, color = ReminderBlueContainer, content = icon)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(label, fontSize = 14.sp, lineHeight = 18.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Text(value, fontSize = 17.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                supporting?.let { Text(it, fontSize = 13.sp, lineHeight = 16.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) }
            }
            Text("›", fontSize = 28.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RepeatCard(
    modifier: Modifier,
    label: String,
    selected: Boolean,
    blue: Color,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.height(86.dp),
        onClick = onClick,
        shape = androidx.compose.material3.MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = if (selected) blue else androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh),
        border = if (selected) BorderStroke(2.dp, Color(0xFF78B8FF)) else null,
    ) {
        Column(Modifier.fillMaxWidth().padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("○", fontSize = 23.sp, lineHeight = 25.sp, color = if (selected) Color.White else blue)
            Text(label, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = if (selected) Color.White else androidx.compose.material3.MaterialTheme.colorScheme.onSurface, maxLines = 1, softWrap = false)
        }
    }
}

private fun Long.startOfDay(): Long = java.time.Instant.ofEpochMilli(this)
    .atZone(java.time.ZoneId.systemDefault())
    .toLocalDate()
    .atStartOfDay(java.time.ZoneId.systemDefault())
    .toInstant()
    .toEpochMilli()

private fun isToday(epochMillis: Long): Boolean {
    val selected = Calendar.getInstance().apply { timeInMillis = epochMillis }
    val today = Calendar.getInstance()
    return selected.get(Calendar.YEAR) == today.get(Calendar.YEAR) && selected.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
}

