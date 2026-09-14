package com.stickr.app.presentation.screens.packdetail

import com.stickr.app.domain.model.Sticker
import com.stickr.app.domain.model.StickerPack

data class PackDetailUiState(
    val isLoading: Boolean = true,
    val pack: StickerPack? = null,
    val stickers: List<Sticker> = emptyList(),
    val errorMessage: String? = null
)
