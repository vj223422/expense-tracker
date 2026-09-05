package com.expensetracker.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.model.Profile
import com.expensetracker.app.data.prefs.AppPreferences
import com.expensetracker.app.data.prefs.ThemeMode
import com.expensetracker.app.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val appPreferences: AppPreferences,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val profileDialog = MutableStateFlow<ProfileDialog?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        appPreferences.themeMode,
        appPreferences.dynamicColorEnabled,
        profileRepository.observeProfiles(),
        profileRepository.observeActiveProfileId(),
        profileDialog,
    ) { themeMode, dynamicColorEnabled, profiles, activeProfileId, dialog ->
        SettingsUiState(
            themeMode = themeMode,
            dynamicColorEnabled = dynamicColorEnabled,
            profiles = profiles,
            activeProfileId = activeProfileId,
            profileDialog = dialog,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun onThemeModeChange(mode: ThemeMode) {
        viewModelScope.launch { appPreferences.setThemeMode(mode) }
    }

    fun onDynamicColorToggle(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setDynamicColorEnabled(enabled) }
    }

    fun onSwitchProfile(profileId: Long) {
        viewModelScope.launch { profileRepository.switchProfile(profileId) }
    }

    fun onCreateProfileClick() {
        profileDialog.value = ProfileDialog.CreateProfile
    }

    fun onCreateProfileConfirm(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            profileRepository.createProfile(trimmed)
            profileDialog.value = null
        }
    }

    fun onDeleteProfileClick(profile: Profile) {
        profileDialog.value = ProfileDialog.ConfirmDelete(profile)
    }

    fun onDeleteProfileConfirm() {
        val target = (profileDialog.value as? ProfileDialog.ConfirmDelete)?.profile ?: return
        viewModelScope.launch {
            profileRepository.deleteProfile(target.id)
            profileDialog.value = null
        }
    }

    fun onProfileDialogDismiss() {
        profileDialog.value = null
    }
}
