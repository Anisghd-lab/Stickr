package com.stickr.app.feature.packdetail

import com.stickr.app.core.database.entity.StickerPackWithStickers

data class PackDetailUiState(
    val packId: String = "",
    val packWithStickers: StickerPackWithStickers? = null,
    val isLoading: Boolean = true,
    val isExporting: Boolean = false,
    val errorMessage: String? = null,
    val userMessage: String? = null
) {
    val stickerCount: Int
        get() = packWithStickers?.stickerCount ?: 0

    val isWhatsAppReady: Boolean
        get() = packWithStickers?.isWhatsAppReady ?: false

    val missingStickersCount: Int
        get() = packWithStickers?.missingStickersCount ?: 3
}
