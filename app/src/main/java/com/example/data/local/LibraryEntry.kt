package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "library_entries")
data class LibraryEntry(
    @PrimaryKey val packageName: String,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val lastLaunchedAt: Long = 0L,
    val launchCount: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)
