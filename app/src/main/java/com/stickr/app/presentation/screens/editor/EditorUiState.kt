package com.stickr.app.presentation.screens.editor

import android.graphics.Bitmap

data class EditorUiState(
    val packId: Long = 0,
    val originalBitmap: Bitmap? = null,
    val cutoutBitmap: Bitmap? = null,
    val processedBitmap: Bitmap? = null,
    val isSegmenting: Boolean = false,
    val isSaving: Boolean = false,
    val hasBorder: Boolean = false,
    val borderColor: Int = android.graphics.Color.WHITE,
    val borderWidth: Float = 12f,
    val stickerText: String = "",
    val errorMessage: String? = null,
    val isSavedSuccess: Boolean = false
)
