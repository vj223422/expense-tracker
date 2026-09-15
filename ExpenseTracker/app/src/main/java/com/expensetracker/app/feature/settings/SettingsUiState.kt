package com.expensetracker.app.feature.settings

import androidx.compose.runtime.Immutable
import com.expensetracker.app.data.model.Profile
import com.expensetracker.app.data.prefs.ThemeMode

@Immutable
data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appLockEnabled: Boolean = false,
    val profiles: List<Profile> = emptyList(),
    val activeProfileId: Long? = null,
    val profileDialog: ProfileDialog? = null,
) {
    val canDeleteProfiles: Boolean get() = profiles.size > 1
}

sealed interface ProfileDialog {
    data object CreateProfile : ProfileDialog
    data class RenameProfile(val profile: Profile) : ProfileDialog
    data class ConfirmDelete(val profile: Profile) : ProfileDialog
}
