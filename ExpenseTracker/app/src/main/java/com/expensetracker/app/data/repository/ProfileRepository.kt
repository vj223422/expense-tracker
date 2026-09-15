package com.expensetracker.app.data.repository

import com.expensetracker.app.data.model.Profile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfiles(): Flow<List<Profile>>
    fun observeActiveProfileId(): Flow<Long?>
    suspend fun ensureDefaultProfile()
    suspend fun createProfile(name: String): Long
    suspend fun renameProfile(profileId: Long, name: String)
    suspend fun switchProfile(profileId: Long)
    suspend fun deleteProfile(profileId: Long): Boolean
}
