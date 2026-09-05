package com.expensetracker.app.data.repository

import com.expensetracker.app.data.model.Profile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfiles(): Flow<List<Profile>>

    /** null until [ensureDefaultProfile] has run at least once (see App.kt). */
    fun observeActiveProfileId(): Flow<Long?>

    /** Creates a first profile if none exist yet. No-ops otherwise. */
    suspend fun ensureDefaultProfile()

    /** Creates a profile and makes it the active one. Returns its id. */
    suspend fun createProfile(name: String): Long

    suspend fun switchProfile(profileId: Long)

    /**
     * Deletes a profile and, via FK cascade, all its expenses and budget limits. Refuses to
     * delete the last remaining profile (returns false) so the app is never left with none. If
     * the deleted profile was active, switches to another remaining one.
     */
    suspend fun deleteProfile(profileId: Long): Boolean
}
