package com.stickr.app.feature.dashboard

import com.stickr.app.core.database.entity.StickerPackWithStickers

data class DashboardUiState(
    val isLoading: Boolean = true,
    val packs: List<StickerPackWithStickers> = emptyList(),
    val errorMessage: String? = null,
    val userMessage: String? = null
)
