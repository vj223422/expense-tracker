package com.expensetracker.app.feature.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.core.designsystem.CreateProfileDialog
import com.expensetracker.app.core.designsystem.SectionHeader
import com.expensetracker.app.data.model.Profile
import com.expensetracker.app.data.prefs.ThemeMode
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsContent(uiState, viewModel::onThemeModeChange, viewModel::onAppLockToggle, viewModel::onSwitchProfile, viewModel::onCreateProfileClick, viewModel::onRenameProfileClick, viewModel::onDeleteProfileClick)
    when (val dialog = uiState.profileDialog) {
        is ProfileDialog.CreateProfile -> CreateProfileDialog(uiState.profiles.map { it.name }, viewModel::onCreateProfileConfirm, viewModel::onProfileDialogDismiss)
        is ProfileDialog.RenameProfile -> RenameProfileDialog(dialog.profile, viewModel::onRenameProfileConfirm, viewModel::onProfileDialogDismiss)
        is ProfileDialog.ConfirmDelete -> DeleteProfileDialog(dialog.profile, viewModel::onDeleteProfileConfirm, viewModel::onProfileDialogDismiss)
        null -> Unit
    }
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAppLockToggle: (Boolean) -> Unit,
    onSwitchProfile: (Long) -> Unit,
    onAddProfileClick: () -> Unit,
    onRenameProfileClick: (Profile) -> Unit,
    onDeleteProfileClick: (Profile) -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        ProfilesSection(uiState.profiles, uiState.activeProfileId, uiState.canDeleteProfiles, onSwitchProfile, onAddProfileClick, onRenameProfileClick, onDeleteProfileClick)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader("Theme")
            Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(Modifier.selectableGroup().padding(vertical = 8.dp)) {
                    ThemeModeRow("System default", uiState.themeMode == ThemeMode.SYSTEM) { onThemeModeChange(ThemeMode.SYSTEM) }
                    ThemeModeRow("Light", uiState.themeMode == ThemeMode.LIGHT) { onThemeModeChange(ThemeMode.LIGHT) }
                    ThemeModeRow("Dark", uiState.themeMode == ThemeMode.DARK) { onThemeModeChange(ThemeMode.DARK) }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader("Security")
            Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) { SecurityRow(uiState.appLockEnabled, onAppLockToggle) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader("About")
            Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Kanakku", style = MaterialTheme.typography.titleMedium)
                    Text("A lightweight, fully offline expense tracker.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SecurityRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch).semantics(mergeDescendants = true) {}.heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Fingerprint, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.width(28.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("App lock", style = MaterialTheme.typography.bodyLarge)
            Text("Use fingerprint or your device biometric to open the app", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun ProfilesSection(profiles: List<Profile>, activeProfileId: Long?, canDelete: Boolean, onSwitchProfile: (Long) -> Unit, onAddProfileClick: () -> Unit, onRenameProfileClick: (Profile) -> Unit, onDeleteProfileClick: (Profile) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Profiles", actionLabel = "Add", onActionClick = onAddProfileClick)
        Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.selectableGroup().padding(vertical = 8.dp)) {
                profiles.forEach { profile ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp).clip(MaterialTheme.shapes.medium).selectable(selected = profile.id == activeProfileId, onClick = { onSwitchProfile(profile.id) }, role = Role.RadioButton).heightIn(min = 48.dp).padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = profile.id == activeProfileId, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(profile.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onRenameProfileClick(profile) }) { Icon(Icons.Filled.Edit, "Rename profile") }
                        IconButton(onClick = { onDeleteProfileClick(profile) }, enabled = canDelete) { Icon(Icons.Filled.DeleteOutline, "Delete profile") }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenameProfileDialog(profile: Profile, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember(profile.id, profile.name) { mutableStateOf(profile.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename profile") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Profile name") }, singleLine = true, supportingText = { if (name.trim().isEmpty()) Text("Profile name can't be empty") }) },
        confirmButton = { TextButton(onClick = { onConfirm(name) }, enabled = name.trim().isNotEmpty()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ThemeModeRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val rowColor by animateColorAsState(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent, animationSpec = tween(150), label = "themeRow")
    Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(rowColor).selectable(selected = selected, onClick = onClick, role = Role.RadioButton).heightIn(min = 48.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun DeleteProfileDialog(profile: Profile, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Delete \"${profile.name}\"?") }, text = { Text("All its transactions and budget limits will be deleted too. This can't be undone.") }, confirmButton = { TextButton(onClick = onConfirm) { Text("Delete") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
