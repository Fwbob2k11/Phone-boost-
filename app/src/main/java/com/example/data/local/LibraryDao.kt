package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {

    @Query("SELECT * FROM library_entries")
    fun observeAllEntries(): Flow<List<LibraryEntry>>

    @Query("SELECT * FROM library_entries WHERE packageName = :packageName LIMIT 1")
    suspend fun getEntry(packageName: String): LibraryEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: LibraryEntry)

    @Query("DELETE FROM library_entries WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)
}
