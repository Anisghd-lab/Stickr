package com.stickr.app.presentation.screens.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.annotation.ColorInt
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stickr.app.core.data.repository.StickerPackRepository
import com.stickr.app.core.image.StickerBorderProcessor
import com.stickr.app.core.image.StickerExporter
import com.stickr.app.core.image.StickerFlattener
import com.stickr.app.domain.model.SegmentationResult
import com.stickr.app.domain.usecase.SegmentImageUseCase
import com.stickr.app.feature.editor.model.DecorationLayer
import com.stickr.app.feature.editor.model.EditorLayer
import com.stickr.app.feature.editor.model.SubjectLayer
import com.stickr.app.feature.editor.model.TextLayer
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
    private val repository: StickerPackRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val packId: String = savedStateHandle.get<String>("packId") ?: ""

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

                val subjectLayer = SubjectLayer(
                    bitmap = bitmap,
                    borderSizePx = 0f,
                    borderColor = Color.WHITE
                )

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
                        isLoadingImage = false,
                        layers = listOf(subjectLayer),
                        selectedLayerId = subjectLayer.id
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

                    _uiState.update { state ->
                        val updatedLayers = state.layers.map { layer ->
                            if (layer is SubjectLayer) {
                                layer.copy(
                                    bitmap = cutout,
                                    borderSizePx = currentBorderSize,
                                    borderColor = currentBorderColor
                                )
                            } else {
                                layer
                            }
                        }
                        state.copy(
                            isSegmenting = false,
                            cutoutBitmap = cutout,
                            renderedBitmap = rendered,
                            layers = updatedLayers
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
     */
    fun setBorderSize(newSize: Float, commitToHistory: Boolean = false) {
        val currentSize = _uiState.value.borderSizePx
        val currentColor = _uiState.value.borderColor

        if (commitToHistory && currentSize != newSize) {
            undoStack.push(BorderConfig(currentSize, currentColor))
            redoStack.clear()
        }

        _uiState.update { state ->
            val updatedLayers = state.layers.map { layer ->
                if (layer is SubjectLayer) {
                    layer.copy(borderSizePx = newSize)
                } else layer
            }
            state.copy(
                borderSizePx = newSize,
                layers = updatedLayers,
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

        _uiState.update { state ->
            val updatedLayers = state.layers.map { layer ->
                if (layer is SubjectLayer) {
                    layer.copy(borderColor = newColor)
                } else layer
            }
            state.copy(
                borderColor = newColor,
                layers = updatedLayers,
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
        _uiState.update { state ->
            val updatedLayers = state.layers.map { layer ->
                if (layer is SubjectLayer) {
                    layer.copy(
                        borderSizePx = previousConfig.borderSizePx,
                        borderColor = previousConfig.borderColor
                    )
                } else layer
            }
            state.copy(
                borderSizePx = previousConfig.borderSizePx,
                borderColor = previousConfig.borderColor,
                layers = updatedLayers,
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
        _uiState.update { state ->
            val updatedLayers = state.layers.map { layer ->
                if (layer is SubjectLayer) {
                    layer.copy(
                        borderSizePx = nextConfig.borderSizePx,
                        borderColor = nextConfig.borderColor
                    )
                } else layer
            }
            state.copy(
                borderSizePx = nextConfig.borderSizePx,
                borderColor = nextConfig.borderColor,
                layers = updatedLayers,
                canUndo = true,
                canRedo = redoStack.isNotEmpty()
            )
        }
        renderStickerPreview(nextConfig.borderSizePx, nextConfig.borderColor)
    }

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

    // --- Gestion des calques (EditorLayer) ---

    /**
     * Ouvre la boîte de dialogue d'ajout de texte stylisé.
     */
    fun openAddTextDialog() {
        _uiState.update { it.copy(isTextDialogOpen = true, editingTextLayer = null) }
    }

    /**
     * Ouvre la boîte de dialogue pour éditer un calque de texte existant.
     */
    fun openEditTextDialog(layer: TextLayer) {
        _uiState.update { it.copy(isTextDialogOpen = true, editingTextLayer = layer) }
    }

    /**
     * Ferme la boîte de dialogue d'édition de texte.
     */
    fun closeTextDialog() {
        _uiState.update { it.copy(isTextDialogOpen = false, editingTextLayer = null) }
    }

    /**
     * Ajoute un nouveau calque de texte stylisé sur le sticker.
     */
    fun addTextLayer(
        text: String,
        textColor: Int = Color.WHITE,
        strokeColor: Int = Color.BLACK,
        strokeWidth: Float = 6f,
        fontSize: Float = 42f,
        fontFamilyName: String = "Impact"
    ) {
        if (text.isBlank()) return
        val newLayer = TextLayer(
            text = text,
            textColor = textColor,
            strokeColor = strokeColor,
            strokeWidth = strokeWidth,
            fontSize = fontSize,
            fontFamilyName = fontFamilyName,
            offset = Offset.Zero,
            scale = 1.0f,
            rotation = 0.0f
        )
        _uiState.update { state ->
            state.copy(
                layers = state.layers + newLayer,
                selectedLayerId = newLayer.id,
                isTextDialogOpen = false,
                editingTextLayer = null
            )
        }
    }

    /**
     * Modifie un calque de texte existant.
     */
    fun updateTextLayer(
        id: String,
        text: String,
        textColor: Int,
        strokeColor: Int,
        strokeWidth: Float,
        fontSize: Float,
        fontFamilyName: String
    ) {
        _uiState.update { state ->
            val updated = state.layers.map { layer ->
                if (layer is TextLayer && layer.id == id) {
                    layer.copy(
                        text = text,
                        textColor = textColor,
                        strokeColor = strokeColor,
                        strokeWidth = strokeWidth,
                        fontSize = fontSize,
                        fontFamilyName = fontFamilyName
                    )
                } else layer
            }
            state.copy(
                layers = updated,
                isTextDialogOpen = false,
                editingTextLayer = null
            )
        }
    }

    /**
     * Ouvre la feuille de sélection d'accessoires et décorations.
     */
    fun openDecorationPicker() {
        _uiState.update { it.copy(isDecorationPickerOpen = true) }
    }

    /**
     * Ferme la feuille de sélection d'accessoires.
     */
    fun closeDecorationPicker() {
        _uiState.update { it.copy(isDecorationPickerOpen = false) }
    }

    /**
     * Ajoute un accessoire ou une décoration sous forme d'emoji ou asset vectoriel.
     */
    fun addDecorationLayer(assetPath: String) {
        if (assetPath.isBlank()) return
        val newLayer = DecorationLayer(
            assetPath = assetPath,
            sizePx = 72f,
            offset = Offset.Zero,
            scale = 1.0f,
            rotation = 0.0f
        )
        _uiState.update { state ->
            state.copy(
                layers = state.layers + newLayer,
                selectedLayerId = newLayer.id,
                isDecorationPickerOpen = false
            )
        }
    }

    /**
     * Sélectionne un calque actif pour la manipulation tactile.
     */
    fun selectLayer(id: String?) {
        _uiState.update { it.copy(selectedLayerId = id) }
    }

    /**
     * Supprime un calque spécifique par son identifiant.
     */
    fun removeLayer(id: String) {
        _uiState.update { state ->
            val filtered = state.layers.filterNot { it.id == id }
            val nextSelected = if (state.selectedLayerId == id) {
                filtered.firstOrNull { it is SubjectLayer }?.id ?: filtered.firstOrNull()?.id
            } else {
                state.selectedLayerId
            }
            state.copy(
                layers = filtered,
                selectedLayerId = nextSelected
            )
        }
    }

    /**
     * Supprime le calque actuellement sélectionné s'il ne s'agit pas du sujet principal.
     */
    fun deleteSelectedLayer() {
        val currentId = _uiState.value.selectedLayerId ?: return
        if (currentId != "subject_layer") {
            removeLayer(currentId)
        }
    }

    /**
     * Met à jour les coordonnées, le zoom et la rotation du calque sélectionné (ou du sujet principal).
     */
    fun updateSelectedLayerTransform(pan: Offset, zoom: Float, rotate: Float) {
        val currentSelectedId = _uiState.value.selectedLayerId
        _uiState.update { state ->
            val targetLayer = state.layers.firstOrNull { it.id == currentSelectedId }
                ?: state.layers.firstOrNull { it is SubjectLayer }

            if (targetLayer != null) {
                val newScale = (targetLayer.scale * zoom).coerceIn(0.2f, 6.0f)
                val newRotation = (targetLayer.rotation + rotate) % 360f
                val newOffset = Offset(targetLayer.offset.x + pan.x, targetLayer.offset.y + pan.y)

                val updatedLayers = state.layers.map { layer ->
                    if (layer.id == targetLayer.id) {
                        layer.withTransform(newOffset, newScale, newRotation)
                    } else layer
                }

                if (targetLayer is SubjectLayer) {
                    state.copy(
                        layers = updatedLayers,
                        scale = newScale,
                        rotation = newRotation,
                        offsetX = newOffset.x,
                        offsetY = newOffset.y
                    )
                } else {
                    state.copy(layers = updatedLayers)
                }
            } else {
                val newScale = (state.scale * zoom).coerceIn(0.2f, 6.0f)
                val newRotation = (state.rotation + rotate) % 360f
                val newOffsetX = state.offsetX + pan.x
                val newOffsetY = state.offsetY + pan.y
                state.copy(
                    scale = newScale,
                    rotation = newRotation,
                    offsetX = newOffsetX,
                    offsetY = newOffsetY
                )
            }
        }
    }

    /**
     * Délègue la transformation tactile au calque actif pour compatibilité.
     */
    fun onTransformGesture(pan: Offset, zoom: Float, rotate: Float) {
        updateSelectedLayerTransform(pan, zoom, rotate)
    }

    /**
     * Réinitialise le cadrage tactile au centre.
     */
    fun resetTransform() {
        val currentSelectedId = _uiState.value.selectedLayerId
        _uiState.update { state ->
            val updatedLayers = state.layers.map { layer ->
                if (currentSelectedId == null || layer.id == currentSelectedId) {
                    layer.withTransform(Offset.Zero, 1.0f, 0.0f)
                } else layer
            }
            state.copy(
                scale = 1.0f,
                rotation = 0.0f,
                offsetX = 0.0f,
                offsetY = 0.0f,
                layers = updatedLayers
            )
        }
    }

    /**
     * Enregistre le sticker finalisé :
     * 1. Aplatissement graphique via [StickerFlattener] (sujet, contour, textes, accessoires en 512x512).
     * 2. Exportation conforme WhatsApp 512x512 WebP (< 100 Ko).
     * 3. Écriture sécurisée sur le stockage interne de l'application.
     * 4. Insertion en base Room via [StickerPackRepository].
     */
    fun saveSticker(onSuccess: () -> Unit) {
        val state = _uiState.value
        val baseBitmap = state.renderedBitmap ?: state.cutoutBitmap ?: state.originalBitmap ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val filePath = withContext(Dispatchers.Default) {
                    // Préparation de la liste ordonnée des calques avec les dernières propriétés de bordure
                    val layersToFlatten = if (state.layers.isNotEmpty()) {
                        state.layers.map { layer ->
                            if (layer is SubjectLayer) {
                                layer.copy(
                                    bitmap = state.cutoutBitmap ?: state.originalBitmap ?: layer.bitmap,
                                    borderSizePx = state.borderSizePx,
                                    borderColor = state.borderColor
                                )
                            } else {
                                layer
                            }
                        }
                    } else {
                        listOf(
                            SubjectLayer(
                                bitmap = state.cutoutBitmap ?: state.originalBitmap ?: baseBitmap,
                                offset = Offset(state.offsetX, state.offsetY),
                                scale = state.scale,
                                rotation = state.rotation,
                                borderSizePx = state.borderSizePx,
                                borderColor = state.borderColor
                            )
                        )
                    }

                    // Fusion de tous les calques dans un Bitmap 512x512 ARGB_8888
                    val flattenedBitmap = StickerFlattener.flattenLayers(
                        canvasSize = StickerFlattener.DEFAULT_CANVAS_SIZE,
                        layers = layersToFlatten
                    )

                    // Compression stricte WebP 512x512 sous 100 Ko
                    val webpBytes = StickerExporter.prepareForWhatsApp(flattenedBitmap)
                    flattenedBitmap.recycle()

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

                repository.addStickerToPack(
                    packId = _uiState.value.packId,
                    imagePath = filePath,
                    emojis = "✨"
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
