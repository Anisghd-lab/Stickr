package com.stickr.app.di

import com.stickr.app.core.data.repository.StickerPackRepository
import com.stickr.app.core.data.repository.StickerPackRepositoryImpl
import com.stickr.app.data.repository.ImageSegmentationRepositoryImpl
import com.stickr.app.domain.repository.ImageSegmentationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindStickerPackRepository(
        impl: StickerPackRepositoryImpl
    ): StickerPackRepository

    @Binds
    @Singleton
    abstract fun bindImageSegmentationRepository(
        impl: ImageSegmentationRepositoryImpl
    ): ImageSegmentationRepository
}
