package com.example.startup_etnofit_2

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ChecksData::class, ReckoningData::class, PreviousReckoningData::class], version = 12, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun checksDataDao(): ChecksDataDao
    abstract fun reckoningDataDao(): ReckoningDataDao
    abstract fun previousReckoningDataDao(): PreviousReckoningDataDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(MIGRATION_11_12)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE checks_data ADD COLUMN realRevenue REAL NOT NULL DEFAULT 0.0")
            }
        }
    }
}