package com.stickr.app.presentation.screens.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.ColorInt
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stickr.app.core.image.StickerBorderProcessor
import com.stickr.app.core.image.StickerExporter
import com.stickr.app.domain.model.SegmentationResult
import com.stickr.app.domain.usecase.SaveStickerUseCase
import com.stickr.app.domain.usecase.SegmentImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.ArrayDeque
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class StickerEditorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val segmentImageUseCase: SegmentImageUseCase,
    private val saveStickerUseCase: SaveStickerUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val packId: Long = savedStateHandle.get<Long>("packId") ?: 0L

    private val _uiState = MutableStateFlow(StickerEditorUiState(packId = packId))
    val uiState: StateFlow<StickerEditorUiState> = _uiState.asStateFlow()

    private val undoStack = ArrayDeque<BorderConfig>()
    private val redoStack = ArrayDeque<BorderConfig>()

    private var borderRenderJob: Job? = null

    /**
     * Charge une image depuis un [Uri] (résultat du Photo Picker Android).
     */
    fun loadFromUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingImage = true, errorMessage = null) }
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    val inputStream = context.contentResolver.openInputStream(uri)
                        ?: throw IllegalArgumentException("Impossible d'ouvrir le flux pour l'URI : $uri")

                    // Décodage avec limitation de taille défensive pour éviter les OOM
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeStream(inputStream, null, options)
                    inputStream.close()

                    var sampleSize = 1
                    val maxDim = maxOf(options.outWidth, options.outHeight)
                    while (maxDim / sampleSize > 1600) {
                        sampleSize *= 2
                    }

                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }

                    val stream2 = context.contentResolver.openInputStream(uri)
                    val decoded = BitmapFactory.decodeStream(stream2, null, decodeOptions)
                    stream2?.close()
                    decoded ?: throw IllegalStateException("Décodage de l'image impossible")
                }

                undoStack.clear()
                redoStack.clear()

                _uiState.update {
                    it.copy(
                        originalBitmap = bitmap,
                        cutoutBitmap = null,
                        renderedBitmap = bitmap,
                        borderSizePx = 0f,
                        canUndo = false,
                        canRedo = false,
                        scale = 1.0f,
                        rotation = 0.0f,
                        offsetX = 0.0f,
                        offsetY = 0.0f,
                        isLoadingImage = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingImage = false,
                        errorMessage = "Erreur de chargement de l'image : ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * Exécute le détourage IA automatique via Google MediaPipe Tasks Vision sur [Dispatchers.Default].
     */
    fun performAiSegmentation() {
        val original = _uiState.value.originalBitmap ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSegmenting = true, errorMessage = null) }
            when (val result = segmentImageUseCase(original)) {
                is SegmentationResult.Success -> {
                    val cutout = result.cutoutBitmap
                    val currentBorderSize = _uiState.value.borderSizePx
                    val currentBorderColor = _uiState.value.borderColor

                    val rendered = if (currentBorderSize > 0f) {
                        StickerBorderProcessor.addStickerBorder(
                            source = cutout,
                            borderSizePx = currentBorderSize,
                            borderColor = currentBorderColor
                        )
                    } else {
                        cutout
                    }

                    _uiState.update {
                        it.copy(
                            isSegmenting = false,
                            cutoutBitmap = cutout,
                            renderedBitmap = rendered
                        )
                    }
                }
                is SegmentationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSegmenting = false,
                            errorMessage = "Échec du détourage IA : ${result.message}"
                        )
                    }
                }
            }
        }
    }

    /**
     * Met à jour l'épaisseur de la bordure avec recalcul asynchrone non-bloquant du rendu.
     *
     * @param newSize Nouvelle épaisseur en pixels (0 à 32px).
     * @param commitToHistory True lorsque l'utilisateur relâche le curseur (enregistre dans Undo).
     */
    fun setBorderSize(newSize: Float, commitToHistory: Boolean = false) {
        val currentSize = _uiState.value.borderSizePx
        val currentColor = _uiState.value.borderColor

        if (commitToHistory && currentSize != newSize) {
            undoStack.push(BorderConfig(currentSize, currentColor))
            redoStack.clear()
        }

        _uiState.update {
            it.copy(
                borderSizePx = newSize,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }

        renderStickerPreview(newSize, currentColor)
    }

    /**
     * Modifie la couleur de la bordure et rafraîchit le rendu.
     */
    fun setBorderColor(@ColorInt newColor: Int) {
        val currentSize = _uiState.value.borderSizePx
        val currentColor = _uiState.value.borderColor

        if (currentColor != newColor) {
            undoStack.push(BorderConfig(currentSize, currentColor))
            redoStack.clear()
        }

        _uiState.update {
            it.copy(
                borderColor = newColor,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }

        renderStickerPreview(currentSize, newColor)
    }

    /**
     * Annule la dernière modification de bordure.
     */
    fun undo() {
        if (undoStack.isEmpty()) return
        val currentConfig = BorderConfig(_uiState.value.borderSizePx, _uiState.value.borderColor)
        redoStack.push(currentConfig)

        val previousConfig = undoStack.pop()
        _uiState.update {
            it.copy(
                borderSizePx = previousConfig.borderSizePx,
                borderColor = previousConfig.borderColor,
                canUndo = undoStack.isNotEmpty(),
                canRedo = true
            )
        }
        renderStickerPreview(previousConfig.borderSizePx, previousConfig.borderColor)
    }

    /**
     * Rétablit la dernière modification de bordure annulée.
     */
    fun redo() {
        if (redoStack.isEmpty()) return
        val currentConfig = BorderConfig(_uiState.value.borderSizePx, _uiState.value.borderColor)
        undoStack.push(currentConfig)

        val nextConfig = redoStack.pop()
        _uiState.update {
            it.copy(
                borderSizePx = nextConfig.borderSizePx,
                borderColor = nextConfig.borderColor,
                canUndo = true,
                canRedo = redoStack.isNotEmpty()
            )
        }
        renderStickerPreview(nextConfig.borderSizePx, nextConfig.borderColor)
    }

    /**
     * Recalcule le bitmap de prévisualisation avec contour sur [Dispatchers.Default].
     */
    private fun renderStickerPreview(borderSize: Float, @ColorInt borderColor: Int) {
        val base = _uiState.value.cutoutBitmap ?: _uiState.value.originalBitmap ?: return

        borderRenderJob?.cancel()
        borderRenderJob = viewModelScope.launch(Dispatchers.Default) {
            val newRendered = if (borderSize > 0f) {
                StickerBorderProcessor.addStickerBorder(
                    source = base,
                    borderSizePx = borderSize,
                    borderColor = borderColor
                )
            } else {
                base
            }
            _uiState.update { it.copy(renderedBitmap = newRendered) }
        }
    }

    /**
     * Met à jour les coordonnées et le zoom de transformation tactile.
     */
    fun onTransformGesture(pan: Offset, zoom: Float, rotate: Float) {
        _uiState.update {
            val newScale = (it.scale * zoom).coerceIn(0.2f, 6.0f)
            val newRotation = (it.rotation + rotate) % 360f
            val newOffsetX = it.offsetX + pan.x
            val newOffsetY = it.offsetY + pan.y
            it.copy(
                scale = newScale,
                rotation = newRotation,
                offsetX = newOffsetX,
                offsetY = newOffsetY
            )
        }
    }

    /**
     * Réinitialise le cadrage tactile au centre.
     */
    fun resetTransform() {
        _uiState.update {
            it.copy(
                scale = 1.0f,
                rotation = 0.0f,
                offsetX = 0.0f,
                offsetY = 0.0f
            )
        }
    }

    /**
     * Enregistre le sticker finalisé :
     * 1. Exportation conforme WhatsApp 512x512 WebP (< 100 Ko).
     * 2. Écriture sécurisée sur le stockage interne de l'application.
     * 3. Insertion en base Room via [SaveStickerUseCase].
     */
    fun saveSticker(onSuccess: () -> Unit) {
        val bitmapToExport = _uiState.value.renderedBitmap ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val filePath = withContext(Dispatchers.Default) {
                    // Compression stricte WebP 512x512 sous 100 Ko
                    val webpBytes = StickerExporter.prepareForWhatsApp(bitmapToExport)

                    withContext(Dispatchers.IO) {
                        val stickersDir = File(context.filesDir, "stickers")
                        if (!stickersDir.exists()) {
                            stickersDir.mkdirs()
                        }
                        val fileName = "sticker_${UUID.randomUUID()}.webp"
                        val targetFile = File(stickersDir, fileName)
                        FileOutputStream(targetFile).use { it.write(webpBytes) }
                        targetFile.absolutePath
                    }
                }

                saveStickerUseCase(
                    packId = _uiState.value.packId,
                    imageUri = filePath,
                    emojis = listOf("✨")
                )

                _uiState.update { it.copy(isSaving = false, isSaveSuccess = true) }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Échec de l'enregistrement : ${e.localizedMessage}"
                    )
                }
            }
        }
    }
}
