package com.stickr.app.domain.usecase

import com.stickr.app.domain.model.StickerPack
import com.stickr.app.domain.repository.StickerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStickerPacksUseCase @Inject constructor(
    private val repository: StickerRepository
) {
    operator fun invoke(): Flow<List<StickerPack>> = repository.getAllPacks()
}
