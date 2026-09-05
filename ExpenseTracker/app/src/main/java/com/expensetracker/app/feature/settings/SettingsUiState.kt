package com.expensetracker.app.feature.settings

import com.expensetracker.app.data.prefs.ThemeMode

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = true,
)
