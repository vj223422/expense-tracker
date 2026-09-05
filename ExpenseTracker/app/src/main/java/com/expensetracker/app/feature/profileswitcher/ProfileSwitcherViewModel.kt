package com.expensetracker.app.feature.profileswitcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.model.Profile
import com.expensetracker.app.data.repository.ProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileSwitcherUiState(
    val profiles: List<Profile> = emptyList(),
    val activeProfileId: Long? = null,
) {
    val activeProfile: Profile? get() = profiles.firstOrNull { it.id == activeProfileId }
}

/**
 * Backs the top-bar profile avatar (navigation/ExpenseNavHost.kt) — a separate small ViewModel
 * rather than threading profile state through every screen's own VM, since this chrome is
 * rendered once at the NavHost level, outside any single feature.
 */
class ProfileSwitcherViewModel(
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    val uiState: StateFlow<ProfileSwitcherUiState> = combine(
        profileRepository.observeProfiles(),
        profileRepository.observeActiveProfileId(),
    ) { profiles, activeProfileId ->
        ProfileSwitcherUiState(profiles = profiles, activeProfileId = activeProfileId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileSwitcherUiState())

    fun onSwitchProfile(profileId: Long) {
        viewModelScope.launch { profileRepository.switchProfile(profileId) }
    }

    fun onCreateProfile(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { profileRepository.createProfile(trimmed) }
    }
}
