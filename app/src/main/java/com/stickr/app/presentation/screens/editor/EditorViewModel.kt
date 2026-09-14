package com.stickr.app.presentation.screens.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stickr.app.domain.model.SegmentationResult
import com.stickr.app.domain.usecase.SaveStickerUseCase
import com.stickr.app.domain.usecase.SegmentImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val segmentImageUseCase: SegmentImageUseCase,
    private val saveStickerUseCase: SaveStickerUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val packId: Long = savedStateHandle.get<Long>("packId") ?: 0L

    private val _uiState = MutableStateFlow(EditorUiState(packId = packId))
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    fun setSourceBitmap(bitmap: Bitmap) {
        _uiState.update { it.copy(originalBitmap = bitmap, processedBitmap = bitmap) }
    }

    /**
     * Perform local AI background segmentation using Google MediaPipe Tasks Vision
     */
    fun performAutoSegmentation() {
        val currentBitmap = _uiState.value.originalBitmap ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSegmenting = true, errorMessage = null) }
            when (val result = segmentImageUseCase(currentBitmap)) {
                is SegmentationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSegmenting = false,
                            processedBitmap = result.cutoutBitmap
                        )
                    }
                }
                is SegmentationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSegmenting = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun toggleBorder() {
        val current = _uiState.value
        val newHasBorder = !current.hasBorder
        _uiState.update { it.copy(hasBorder = newHasBorder) }
    }

    fun setStickerText(text: String) {
        _uiState.update { it.copy(stickerText = text) }
    }

    fun saveSticker(onSuccess: () -> Unit) {
        val bitmapToSave = _uiState.value.processedBitmap ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val fileUri = saveBitmapToFile(bitmapToSave)
                saveStickerUseCase(
                    packId = _uiState.value.packId,
                    imageUri = fileUri
                )
                _uiState.update { it.copy(isSaving = false, isSavedSuccess = true) }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    private suspend fun saveBitmapToFile(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val stickersDir = File(context.filesDir, "stickers")
        if (!stickersDir.exists()) {
            stickersDir.mkdirs()
        }
        val file = File(stickersDir, "sticker_${UUID.randomUUID()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        file.absolutePath
    }
}
