package com.example.silentguardian.data

import androidx.room.*

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_entries ORDER BY timestamp DESC")
    suspend fun getAllEntries(): List<VaultEntry>

    @Insert
    suspend fun insertEntry(entry: VaultEntry)

    @Query("DELETE FROM vault_entries")
    suspend fun deleteAll()
}