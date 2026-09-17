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

class AppPreferences(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color_enabled")
        val APP_LOCK = booleanPreferencesKey("app_lock_enabled")
        val ACTIVE_PROFILE_ID = longPreferencesKey("active_profile_id")
        fun activeBudgetMonth(profileId: Long) = stringPreferencesKey("active_budget_month_$profileId")
        fun activeBudgetCycleStart(profileId: Long) = longPreferencesKey("active_budget_cycle_start_$profileId")
        fun carryForward(profileId: Long, budgetMonth: String) = longPreferencesKey("budget_carry_forward_${profileId}_$budgetMonth")
        fun lastNotifiedTier(periodCategoryKey: String) = intPreferencesKey("alert_tier_$periodCategoryKey")
    }
    private val safeData = context.dataStore.data.catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
    val themeMode: Flow<ThemeMode> = safeData.map { it[Keys.THEME_MODE]?.let { value -> runCatching { ThemeMode.valueOf(value) }.getOrNull() } ?: ThemeMode.SYSTEM }
    val dynamicColorEnabled: Flow<Boolean> = safeData.map { it[Keys.DYNAMIC_COLOR] ?: true }
    val appLockEnabled: Flow<Boolean> = safeData.map { it[Keys.APP_LOCK] ?: false }
    val activeProfileId: Flow<Long?> = safeData.map { it[Keys.ACTIVE_PROFILE_ID] }
    fun activeBudgetMonth(profileId: Long): Flow<String?> = safeData.map { it[Keys.activeBudgetMonth(profileId)] }
    fun activeBudgetCycleStart(profileId: Long): Flow<Long?> = safeData.map { it[Keys.activeBudgetCycleStart(profileId)] }
    fun carryForward(profileId: Long, budgetMonth: String): Flow<Long> = safeData.map { it[Keys.carryForward(profileId, budgetMonth)] ?: 0L }
    suspend fun setThemeMode(mode: ThemeMode) { context.dataStore.edit { it[Keys.THEME_MODE] = mode.name } }
    suspend fun setDynamicColorEnabled(enabled: Boolean) { context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled } }
    suspend fun setAppLockEnabled(enabled: Boolean) { context.dataStore.edit { it[Keys.APP_LOCK] = enabled } }
    suspend fun setActiveProfileId(profileId: Long) { context.dataStore.edit { it[Keys.ACTIVE_PROFILE_ID] = profileId } }
    suspend fun getActiveBudgetMonth(profileId: Long): String? = activeBudgetMonth(profileId).first()
    suspend fun setActiveBudgetMonth(profileId: Long, budgetMonth: String) { context.dataStore.edit { it[Keys.activeBudgetMonth(profileId)] = budgetMonth } }
    suspend fun getActiveBudgetCycleStart(profileId: Long): Long? = activeBudgetCycleStart(profileId).first()
    suspend fun setActiveBudgetCycleStart(profileId: Long, startEpochMillis: Long) { context.dataStore.edit { it[Keys.activeBudgetCycleStart(profileId)] = startEpochMillis } }
    suspend fun getCarryForward(profileId: Long, budgetMonth: String): Long = carryForward(profileId, budgetMonth).first()
    suspend fun setCarryForward(profileId: Long, budgetMonth: String, amountMinor: Long) { context.dataStore.edit { it[Keys.carryForward(profileId, budgetMonth)] = amountMinor.coerceAtLeast(0L) } }
    suspend fun getLastNotifiedTier(periodCategoryKey: String): Int = safeData.map { it[Keys.lastNotifiedTier(periodCategoryKey)] ?: 0 }.first()
    suspend fun setLastNotifiedTier(periodCategoryKey: String, tier: Int) { context.dataStore.edit { it[Keys.lastNotifiedTier(periodCategoryKey)] = tier } }
    suspend fun clearAlertTiersForProfile(profileId: Long) {
        val prefix = "alert_tier_${profileId}_"
        context.dataStore.edit { prefs -> prefs.asMap().keys.filter { it.name.startsWith(prefix) }.forEach { prefs.remove(it) } }
    }
}