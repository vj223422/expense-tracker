package com.expensetracker.app.feature.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.core.designsystem.CreateProfileDialog
import com.expensetracker.app.data.model.Profile
import com.expensetracker.app.data.prefs.ThemeMode
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showReceived by rememberSaveable { mutableStateOf(true) }
    var reminderNotifications by rememberSaveable { mutableStateOf(true) }
    SettingsContent(uiState, viewModel::onThemeModeChange, viewModel::onAppLockToggle, viewModel::onSwitchProfile,
        viewModel::onCreateProfileClick, viewModel::onRenameProfileClick, viewModel::onDeleteProfileClick,
        showReceived, { showReceived = it }, reminderNotifications, { reminderNotifications = it })
    when (val dialog = uiState.profileDialog) {
        is ProfileDialog.CreateProfile -> CreateProfileDialog(uiState.profiles.map { it.name }, viewModel::onCreateProfileConfirm, viewModel::onProfileDialogDismiss)
        is ProfileDialog.RenameProfile -> RenameProfileDialog(dialog.profile, viewModel::onRenameProfileConfirm, viewModel::onProfileDialogDismiss)
        is ProfileDialog.ConfirmDelete -> DeleteProfileDialog(dialog.profile, viewModel::onDeleteProfileConfirm, viewModel::onProfileDialogDismiss)
        null -> Unit
    }
}

@Composable
private fun SettingsContent(uiState: SettingsUiState, onTheme: (ThemeMode) -> Unit, onLock: (Boolean) -> Unit,
    onProfile: (Long) -> Unit, onAdd: () -> Unit, onRename: (Profile) -> Unit, onDelete: (Profile) -> Unit,
    showReceived: Boolean, onShowReceived: (Boolean) -> Unit, reminders: Boolean, onReminders: (Boolean) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        ProfilesSection(uiState.profiles, uiState.activeProfileId, uiState.canDeleteProfiles, onProfile, onAdd, onRename, onDelete)
        SettingsSection("Theme", "Choose how the app looks") {
            ThemeModeRow("System default", "Use your device setting", Icons.Default.BrightnessAuto, uiState.themeMode == ThemeMode.SYSTEM) { onTheme(ThemeMode.SYSTEM) }
            ThemeModeRow("Light", "Always use light theme", Icons.Default.LightMode, uiState.themeMode == ThemeMode.LIGHT) { onTheme(ThemeMode.LIGHT) }
            ThemeModeRow("Dark", "Always use dark theme", Icons.Default.DarkMode, uiState.themeMode == ThemeMode.DARK) { onTheme(ThemeMode.DARK) }
        }
        SettingsSection("App Settings", "Customize your experience") {
            ToggleRow(Icons.Default.BarChart, "Show received in home summary", "Include income in home dashboard cards", showReceived, onShowReceived)
            ToggleRow(Icons.Default.NotificationsActive, "Reminder notifications", "Get notified about your reminders", reminders, onReminders)
            ToggleRow(Icons.Default.Fingerprint, "App lock", "Use fingerprint to open the app", uiState.appLockEnabled, onLock)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("About", style = MaterialTheme.typography.titleLarge)
            Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(56.dp), shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp)) }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Kanakku", style = MaterialTheme.typography.titleMedium)
                        Text("A lightweight, fully offline expense tracker.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Version 1.0.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.selectableGroup().padding(vertical = 6.dp)) { content() }
        }
    }
}

@Composable
private fun ThemeModeRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    val rowColor by animateColorAsState(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, animationSpec = tween(150), label = "themeRow")
    Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(rowColor).selectable(selected = selected, onClick = onClick, role = Role.RadioButton).heightIn(min = 72.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Surface(Modifier.size(44.dp), shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) } }
        Spacer(Modifier.width(16.dp))
        Column { Text(title, style = MaterialTheme.typography.bodyLarge); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun ToggleRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch).semantics(mergeDescendants = true) {}.heightIn(min = 72.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(Modifier.size(44.dp), shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.primaryContainer) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) } }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.bodyLarge); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Spacer(Modifier.width(12.dp)); Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun ProfilesSection(profiles: List<Profile>, activeProfileId: Long?, canDelete: Boolean, onSwitch: (Long) -> Unit, onAdd: () -> Unit, onRename: (Profile) -> Unit, onDelete: (Profile) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Profiles", style = MaterialTheme.typography.titleLarge); Text("Manage multiple profiles (e.g. Personal, Family)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            TextButton(onClick = onAdd) { Text("＋ Add", style = MaterialTheme.typography.titleMedium) }
        }
        Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.padding(vertical = 6.dp)) {
                profiles.forEachIndexed { index, profile ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp).clip(MaterialTheme.shapes.medium).selectable(selected = profile.id == activeProfileId, onClick = { onSwitch(profile.id) }, role = Role.RadioButton).heightIn(min = 72.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = profile.id == activeProfileId, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Surface(Modifier.size(48.dp), shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.primaryContainer) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) } }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) { Text(profile.name, style = MaterialTheme.typography.bodyLarge); if (index == 0) Text("Default profile", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        IconButton(onClick = { onRename(profile) }) { Icon(Icons.Default.Edit, "Rename profile") }
                        IconButton(onClick = { onDelete(profile) }, enabled = canDelete) { Icon(Icons.Default.DeleteOutline, "Delete profile") }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenameProfileDialog(profile: Profile, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember(profile.id, profile.name) { mutableStateOf(profile.name) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Rename profile") }, text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Profile name") }, singleLine = true) }, confirmButton = { TextButton(onClick = { onConfirm(name) }, enabled = name.trim().isNotEmpty()) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun DeleteProfileDialog(profile: Profile, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Delete \"${profile.name}\"?") }, text = { Text("All its transactions and budget limits will be deleted too. This can't be undone.") }, confirmButton = { TextButton(onClick = onConfirm) { Text("Delete") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
