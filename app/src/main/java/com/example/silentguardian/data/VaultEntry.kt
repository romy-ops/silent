package com.example.silentguardian.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_entries")
data class VaultEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)