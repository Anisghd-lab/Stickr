package com.stickr.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.stickr.app.core.database.dao.StickerPackDao
import com.stickr.app.core.database.entity.StickerItemEntity
import com.stickr.app.core.database.entity.StickerPackEntity

/**
 * Base de données Room StickrDatabase pour la persistance locale des packs et stickers.
 */
@Database(
    entities = [
        StickerPackEntity::class,
        StickerItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class StickrDatabase : RoomDatabase() {

    abstract fun stickerPackDao(): StickerPackDao

    companion object {
        const val DATABASE_NAME = "stickr_database.db"

        @Volatile
        private var INSTANCE: StickrDatabase? = null

        fun getInstance(context: Context): StickrDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StickrDatabase::class.java,
                    DATABASE_NAME
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
