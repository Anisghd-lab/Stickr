package com.stickr.app.di

import android.content.Context
import com.stickr.app.data.mediapipe.ImageSegmentationHelper
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
    fun provideImageSegmentationHelper(
        @ApplicationContext context: Context
    ): ImageSegmentationHelper {
        return ImageSegmentationHelper(context)
    }
}
