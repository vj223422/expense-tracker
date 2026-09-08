package com.expensetracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.expensetracker.app.data.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Insert
    suspend fun insert(profile: ProfileEntity): Long

    /** Cascades to that profile's expenses and budget limits — see the FK on those entities. */
    @Query("DELETE FROM profiles WHERE id = :profileId")
    suspend fun delete(profileId: Long)

    @Query("SELECT * FROM profiles ORDER BY createdAtEpochMillis ASC")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun count(): Int

    /** Atomic "don't delete the last profile" check — returns false (without deleting) if
     * [profileId] is the only remaining profile, true if it deleted it. Room wraps this whole
     * default method in a single DB transaction, so two concurrent calls can't both observe
     * count() > 1 before either commits its delete. */
    @Transaction
    suspend fun deleteIfNotLast(profileId: Long): Boolean {
        if (count() <= 1) return false
        delete(profileId)
        return true
    }
}
