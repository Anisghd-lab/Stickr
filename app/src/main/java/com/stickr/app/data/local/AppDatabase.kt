package com.stickr.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.stickr.app.data.local.dao.StickerDao
import com.stickr.app.data.local.dao.StickerPackDao
import com.stickr.app.data.local.entity.StickerEntity
import com.stickr.app.data.local.entity.StickerPackEntity

@Database(
    entities = [
        StickerPackEntity::class,
        StickerEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stickerPackDao(): StickerPackDao
    abstract fun stickerDao(): StickerDao

    companion object {
        const val DATABASE_NAME = "stickr_database.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
