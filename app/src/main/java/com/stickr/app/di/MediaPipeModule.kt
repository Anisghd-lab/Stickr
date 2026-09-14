package com.stickr.app.di

import android.content.Context
import com.stickr.app.core.image.ImageSegmenterHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaPipeModule {

    @Provides
    @Singleton
    fun provideImageSegmenterHelper(
        @ApplicationContext context: Context
    ): ImageSegmenterHelper {
        return ImageSegmenterHelper(context)
    }
}
