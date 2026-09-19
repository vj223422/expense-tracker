package com.expensetracker.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.model.Profile
import com.expensetracker.app.data.prefs.AppPreferences
import com.expensetracker.app.data.prefs.ThemeMode
import com.expensetracker.app.data.repository.ProfileRepository
import com.expensetracker.app.data.local.dao.ReminderDao
import com.expensetracker.app.data.reminder.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val appPreferences: AppPreferences,
    private val profileRepository: ProfileRepository,
    private val reminderDao: ReminderDao,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {
    private val profileDialog = MutableStateFlow<ProfileDialog?>(null)

    private val settingsState: kotlinx.coroutines.flow.Flow<SettingsUiState> = combine(
        appPreferences.themeMode,
        appPreferences.appLockEnabled,
        appPreferences.reminderSound,
        profileRepository.observeProfiles(),
        profileRepository.observeActiveProfileId(),
    ) { themeMode, appLockEnabled, reminderSound, profiles, activeProfileId ->
        SettingsUiState(
            themeMode = themeMode,
            appLockEnabled = appLockEnabled,
            profiles = profiles,
            activeProfileId = activeProfileId,
            reminderSound = reminderSound,
        )
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsState,
        profileDialog,
    ) { state, dialog ->
        state.copy(profileDialog = dialog)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun onThemeModeChange(mode: ThemeMode) { viewModelScope.launch { appPreferences.setThemeMode(mode) } }
    fun onAppLockToggle(enabled: Boolean) { viewModelScope.launch { appPreferences.setAppLockEnabled(enabled) } }
    fun onReminderSoundChange(sound: String) {
        viewModelScope.launch {
            appPreferences.setReminderSound(sound)
            reminderDao.getEnabled().forEach { reminderScheduler.schedule(it, sound) }
        }
    }
    fun onSwitchProfile(profileId: Long) { viewModelScope.launch { profileRepository.switchProfile(profileId) } }
    fun onCreateProfileClick() { profileDialog.value = ProfileDialog.CreateProfile }

    fun onCreateProfileConfirm(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { profileRepository.createProfile(trimmed); profileDialog.value = null }
    }

    fun onRenameProfileClick(profile: Profile) { profileDialog.value = ProfileDialog.RenameProfile(profile) }

    fun onRenameProfileConfirm(name: String) {
        val target = (profileDialog.value as? ProfileDialog.RenameProfile)?.profile ?: return
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { profileRepository.renameProfile(target.id, trimmed); profileDialog.value = null }
    }

    fun onDeleteProfileClick(profile: Profile) { profileDialog.value = ProfileDialog.ConfirmDelete(profile) }
    fun onDeleteProfileConfirm() {
        val target = (profileDialog.value as? ProfileDialog.ConfirmDelete)?.profile ?: return
        viewModelScope.launch { profileRepository.deleteProfile(target.id); profileDialog.value = null }
    }
    fun onProfileDialogDismiss() { profileDialog.value = null }
}
