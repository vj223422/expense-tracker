package com.expensetracker.app.core.designsystem

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/** Shared by the Settings profile list and the top-bar profile switcher — see both call sites.
 * [existingNames] guards against creating a second profile indistinguishable from an existing one
 * in the profile lists (both are name-only), which otherwise makes switching to or deleting the
 * intended one a guess. */
@Composable
fun CreateProfileDialog(existingNames: List<String>, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var touched by rememberSaveable { mutableStateOf(false) }
    val trimmed = name.trim()
    val isBlank = trimmed.isEmpty()
    val isDuplicate = !isBlank && existingNames.any { it.equals(trimmed, ignoreCase = true) }
    val errorMessage = when {
        touched && isBlank -> "Name is required"
        touched && isDuplicate -> "A profile with this name already exists"
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New profile") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    touched = true
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Name") },
                singleLine = true,
                isError = errorMessage != null,
                supportingText = { errorMessage?.let { Text(it) } },
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(trimmed) }, enabled = !isBlank && !isDuplicate) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
