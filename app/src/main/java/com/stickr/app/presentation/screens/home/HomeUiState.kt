package com.stickr.app.presentation.screens.home

import com.stickr.app.domain.model.StickerPack

data class HomeUiState(
    val isLoading: Boolean = true,
    val stickerPacks: List<StickerPack> = emptyList(),
    val errorMessage: String? = null
)
