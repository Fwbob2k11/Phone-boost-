package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [LibraryEntry::class],
    version = 1,
    exportSchema = false
)
abstract class DroidDeckDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao

    companion object {
        @Volatile
        private var instance: DroidDeckDatabase? = null

        fun getInstance(context: Context): DroidDeckDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DroidDeckDatabase::class.java,
                    "droiddeck_hub.db"
                ).build().also { instance = it }
            }
        }
    }
}
