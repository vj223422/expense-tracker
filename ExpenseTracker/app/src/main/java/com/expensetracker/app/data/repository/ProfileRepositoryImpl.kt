package com.expensetracker.app.data.repository

import com.expensetracker.app.data.entity.ProfileEntity
import com.expensetracker.app.data.local.dao.ProfileDao
import com.expensetracker.app.data.model.Profile
import com.expensetracker.app.data.prefs.AppPreferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private const val DEFAULT_PROFILE_NAME = "My Expenses"

class ProfileRepositoryImpl(
    private val profileDao: ProfileDao,
    private val appPreferences: AppPreferences,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ProfileRepository {

    override fun observeProfiles(): Flow<List<Profile>> =
        profileDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeActiveProfileId(): Flow<Long?> = appPreferences.activeProfileId

    override suspend fun ensureDefaultProfile() = withContext(ioDispatcher) {
        if (profileDao.count() == 0) {
            createProfile(DEFAULT_PROFILE_NAME)
        }
        Unit
    }

    override suspend fun createProfile(name: String): Long = withContext(ioDispatcher) {
        val id = profileDao.insert(ProfileEntity(name = name, createdAtEpochMillis = System.currentTimeMillis()))
        appPreferences.setActiveProfileId(id)
        id
    }

    override suspend fun switchProfile(profileId: Long) = withContext(ioDispatcher) {
        appPreferences.setActiveProfileId(profileId)
    }

    override suspend fun deleteProfile(profileId: Long): Boolean = withContext(ioDispatcher) {
        val deleted = profileDao.deleteIfNotLast(profileId)
        if (!deleted) return@withContext false

        appPreferences.clearAlertTiersForProfile(profileId)

        if (appPreferences.activeProfileId.first() == profileId) {
            // Re-query rather than reuse the pre-delete list — that snapshot could be stale if
            // another delete happened concurrently, and could name a profile that's now also gone.
            val fallback = profileDao.observeAll().first().firstOrNull()
            if (fallback != null) {
                appPreferences.setActiveProfileId(fallback.id)
            }
        }
        true
    }
}

private fun ProfileEntity.toDomain() = Profile(id = id, name = name, createdAtEpochMillis = createdAtEpochMillis)
