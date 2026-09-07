package com.expensetracker.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.dataStore by preferencesDataStore(name = "app_preferences")

/**
 * Small typed settings + the alert-dedup ledger (data/notification/LimitAlertEvaluator) — no
 * relational queries needed, so Preferences DataStore over Room (android-skills:datastore).
 */
class AppPreferences(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color_enabled")
        val ACTIVE_PROFILE_ID = longPreferencesKey("active_profile_id")
        fun lastNotifiedTier(periodCategoryKey: String) = intPreferencesKey("alert_tier_$periodCategoryKey")
    }

    private val safeData = context.dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val themeMode: Flow<ThemeMode> = safeData.map { prefs ->
        prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    val dynamicColorEnabled: Flow<Boolean> = safeData.map { prefs -> prefs[Keys.DYNAMIC_COLOR] ?: true }

    /** null until ProfileRepository.ensureDefaultProfile() has run at least once (see App.kt). */
    val activeProfileId: Flow<Long?> = safeData.map { prefs -> prefs[Keys.ACTIVE_PROFILE_ID] }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setActiveProfileId(profileId: Long) {
        context.dataStore.edit { it[Keys.ACTIVE_PROFILE_ID] = profileId }
    }

    /** 0 = none notified yet for this period+scope; see LimitAlertEvaluator.Tier.ordinal. */
    suspend fun getLastNotifiedTier(periodCategoryKey: String): Int =
        safeData.map { it[Keys.lastNotifiedTier(periodCategoryKey)] ?: 0 }.first()

    suspend fun setLastNotifiedTier(periodCategoryKey: String, tier: Int) {
        context.dataStore.edit { it[Keys.lastNotifiedTier(periodCategoryKey)] = tier }
    }
}
