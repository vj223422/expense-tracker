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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.expensetracker.app.core.theme.LocalReducedMotion
import com.expensetracker.app.core.theme.MotionDurations
import com.expensetracker.app.core.theme.MotionEasing
import com.expensetracker.app.data.model.Profile
import com.expensetracker.app.data.prefs.ThemeMode
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsContent(
        uiState = uiState,
        onThemeModeChange = viewModel::onThemeModeChange,
        onDynamicColorToggle = viewModel::onDynamicColorToggle,
        onSwitchProfile = viewModel::onSwitchProfile,
        onAddProfileClick = viewModel::onCreateProfileClick,
        onDeleteProfileClick = viewModel::onDeleteProfileClick,
    )

    when (val dialog = uiState.profileDialog) {
        is ProfileDialog.CreateProfile -> CreateProfileDialog(
            existingNames = uiState.profiles.map { it.name },
            onConfirm = viewModel::onCreateProfileConfirm,
            onDismiss = viewModel::onProfileDialogDismiss,
        )
        is ProfileDialog.ConfirmDelete -> DeleteProfileDialog(
            profile = dialog.profile,
            onConfirm = viewModel::onDeleteProfileConfirm,
            onDismiss = viewModel::onProfileDialogDismiss,
        )
        null -> Unit
    }
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorToggle: (Boolean) -> Unit,
    onSwitchProfile: (Long) -> Unit,
    onAddProfileClick: () -> Unit,
    onDeleteProfileClick: (Profile) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ProfilesSection(
            profiles = uiState.profiles,
            activeProfileId = uiState.activeProfileId,
            canDelete = uiState.canDeleteProfiles,
            onSwitchProfile = onSwitchProfile,
            onAddProfileClick = onAddProfileClick,
            onDeleteProfileClick = onDeleteProfileClick,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(title = "Theme")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(
                    modifier = Modifier
                        .selectableGroup()
                        .padding(vertical = 8.dp),
                ) {
                    ThemeModeRow(
                        label = "System default",
                        selected = uiState.themeMode == ThemeMode.SYSTEM,
                        onClick = { onThemeModeChange(ThemeMode.SYSTEM) },
                    )
                    ThemeModeRow(
                        label = "Light",
                        selected = uiState.themeMode == ThemeMode.LIGHT,
                        onClick = { onThemeModeChange(ThemeMode.LIGHT) },
                    )
                    ThemeModeRow(
                        label = "Dark",
                        selected = uiState.themeMode == ThemeMode.DARK,
                        onClick = { onThemeModeChange(ThemeMode.DARK) },
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(title = "Dynamic Color")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                DynamicColorRow(
                    checked = uiState.dynamicColorEnabled,
                    onCheckedChange = onDynamicColorToggle,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(title = "About")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(text = "Kanakku", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "A lightweight, fully offline expense tracker.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfilesSection(
    profiles: List<Profile>,
    activeProfileId: Long?,
    canDelete: Boolean,
    onSwitchProfile: (Long) -> Unit,
    onAddProfileClick: () -> Unit,
    onDeleteProfileClick: (Profile) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title = "Profiles", actionLabel = "Add", onActionClick = onAddProfileClick)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(
                modifier = Modifier
                    .selectableGroup()
                    .padding(vertical = 8.dp),
            ) {
                profiles.forEach { profile ->
                    ProfileRow(
                        profile = profile,
                        selected = profile.id == activeProfileId,
                        canDelete = canDelete,
                        onClick = { onSwitchProfile(profile.id) },
                        onDeleteClick = { onDeleteProfileClick(profile) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(
    profile: Profile,
    selected: Boolean,
    canDelete: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    val rowColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        animationSpec = tween(
            durationMillis = if (reducedMotion) 0 else MotionDurations.SHORT,
            easing = MotionEasing.Standard,
        ),
        label = "profileRowColor",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(rowColor)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .semantics(mergeDescendants = true) {}
            .heightIn(min = 48.dp)
            .padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = profile.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = onDeleteClick, enabled = canDelete) {
            Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = "Delete profile",
                tint = if (canDelete) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                },
            )
        }
    }
}

@Composable
private fun DeleteProfileDialog(profile: Profile, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete \"${profile.name}\"?") },
        text = { Text("All its transactions and budget limits will be deleted too. This can't be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun ThemeModeRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    val rowColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        animationSpec = tween(
            durationMillis = if (reducedMotion) 0 else MotionDurations.SHORT,
            easing = MotionEasing.Standard,
        ),
        label = "themeModeRowColor",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(rowColor)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .semantics(mergeDescendants = true) {}
            .heightIn(min = 48.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun DynamicColorRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch)
            .semantics(mergeDescendants = true) {}
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Enable dynamic color", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "Match the app's colors to your wallpaper (Android 12+)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}
