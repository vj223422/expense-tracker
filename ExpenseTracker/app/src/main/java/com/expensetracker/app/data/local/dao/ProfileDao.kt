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

    @Query("DELETE FROM profiles WHERE id = :profileId")
    suspend fun delete(profileId: Long)

    @Query("UPDATE profiles SET name = :name WHERE id = :profileId")
    suspend fun rename(profileId: Long, name: String)

    @Query("SELECT * FROM profiles ORDER BY createdAtEpochMillis ASC")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun count(): Int

    @Transaction
    suspend fun deleteIfNotLast(profileId: Long): Boolean {
        if (count() <= 1) return false
        delete(profileId)
        return true
    }
}
