package com.stickr.app.di

import com.stickr.app.data.repository.ImageSegmentationRepositoryImpl
import com.stickr.app.data.repository.StickerRepositoryImpl
import com.stickr.app.domain.repository.ImageSegmentationRepository
import com.stickr.app.domain.repository.StickerRepository
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
    abstract fun bindStickerRepository(
        impl: StickerRepositoryImpl
    ): StickerRepository

    @Binds
    @Singleton
    abstract fun bindImageSegmentationRepository(
        impl: ImageSegmentationRepositoryImpl
    ): ImageSegmentationRepository
}
