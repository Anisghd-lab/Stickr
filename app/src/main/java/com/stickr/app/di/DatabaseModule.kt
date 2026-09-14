package com.stickr.app.di

import android.content.Context
import androidx.room.Room
import com.stickr.app.core.database.StickrDatabase
import com.stickr.app.core.database.dao.StickerPackDao
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
    fun provideStickrDatabase(
        @ApplicationContext context: Context
    ): StickrDatabase {
        return Room.databaseBuilder(
            context,
            StickrDatabase::class.java,
            StickrDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideStickerPackDao(database: StickrDatabase): StickerPackDao {
        return database.stickerPackDao()
    }
}
