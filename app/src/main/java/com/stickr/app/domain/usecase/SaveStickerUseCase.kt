package com.stickr.app.domain.usecase

import com.stickr.app.domain.model.Sticker
import com.stickr.app.domain.repository.StickerRepository
import javax.inject.Inject

class SaveStickerUseCase @Inject constructor(
    private val repository: StickerRepository
) {
    suspend operator fun invoke(packId: Long, imageUri: String, emojis: List<String> = emptyList()): Long {
        require(packId > 0) { "Valid packId required" }
        require(imageUri.isNotBlank()) { "imageUri cannot be empty" }
        val sticker = Sticker(
            packId = packId,
            imageUri = imageUri,
            emojis = emojis
        )
        return repository.insertSticker(sticker)
    }
}
