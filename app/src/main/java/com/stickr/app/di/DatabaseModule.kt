package com.stickr.app.di

import android.content.Context
import androidx.room.Room
import com.stickr.app.data.local.AppDatabase
import com.stickr.app.data.local.dao.StickerDao
import com.stickr.app.data.local.dao.StickerPackDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideStickerPackDao(database: AppDatabase): StickerPackDao {
        return database.stickerPackDao()
    }

    @Provides
    fun provideStickerDao(database: AppDatabase): StickerDao {
        return database.stickerDao()
    }
}
