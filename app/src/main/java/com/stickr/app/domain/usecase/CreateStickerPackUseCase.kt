package com.stickr.app.domain.usecase

import com.stickr.app.domain.model.StickerPack
import com.stickr.app.domain.repository.StickerRepository
import javax.inject.Inject

class CreateStickerPackUseCase @Inject constructor(
    private val repository: StickerRepository
) {
    suspend operator fun invoke(name: String, author: String, trayImageUri: String): Long {
        require(name.isNotBlank()) { "Pack name cannot be empty" }
        require(author.isNotBlank()) { "Author name cannot be empty" }
        val pack = StickerPack(
            name = name.trim(),
            author = author.trim(),
            trayImageUri = trayImageUri
        )
        return repository.insertPack(pack)
    }
}
