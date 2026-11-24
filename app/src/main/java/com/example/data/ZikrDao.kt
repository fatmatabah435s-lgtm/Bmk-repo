package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ZikrDao {
    @Query("SELECT * FROM zikr_entries ORDER BY isCustom DESC, sortOrder ASC, createdAt DESC")
    fun observeAllEntries(): Flow<List<ZikrEntry>>

    @Query("SELECT * FROM zikr_entries")
    suspend fun getAllEntriesOnce(): List<ZikrEntry>

    @Query("SELECT COUNT(*) FROM zikr_entries")
    suspend fun getEntryCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<ZikrEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: ZikrEntry)

    @Update
    suspend fun updateEntry(entry: ZikrEntry)

    @Query("DELETE FROM zikr_entries WHERE id = :id AND isCustom = 1")
    suspend fun deleteCustomEntry(id: String)

    @Query("UPDATE zikr_entries SET sessionState = 'PAUSED' WHERE sessionState = 'ACTIVE' AND (count > 0 OR elapsedSeconds > 0 OR roundsCompleted > 0)")
    suspend fun pauseActiveEntriesWithProgress()

    @Query("UPDATE zikr_entries SET sessionState = 'READY' WHERE sessionState = 'ACTIVE' AND count = 0 AND elapsedSeconds = 0 AND roundsCompleted = 0")
    suspend fun resetEmptyActiveEntries()
}
