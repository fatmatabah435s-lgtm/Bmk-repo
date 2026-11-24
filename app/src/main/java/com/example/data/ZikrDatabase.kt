package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ZikrEntry::class], version = 1, exportSchema = false)
abstract class ZikrDatabase : RoomDatabase() {
    abstract fun zikrDao(): ZikrDao

    companion object {
        @Volatile
        private var instance: ZikrDatabase? = null

        fun getInstance(context: Context): ZikrDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ZikrDatabase::class.java,
                    "zikr_tally_db"
                ).build().also { instance = it }
            }
        }
    }
}
