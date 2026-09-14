package com.stickr.app.presentation.screens.editor

import android.graphics.Bitmap
import android.graphics.Color
import com.stickr.app.feature.editor.model.EditorLayer
import com.stickr.app.feature.editor.model.TextLayer

/**
 * Configuration snapshot pour l'historique Annuler / Rétablir (Undo/Redo).
 */
data class BorderConfig(
    val borderSizePx: Float,
    val borderColor: Int
)

/**
 * État de l'interface graphique de l'éditeur tactile de stickers multi-calques.
 *
 * @param packId ID du pack de destination.
 * @param originalBitmap Image brute importée par l'utilisateur.
 * @param cutoutBitmap Sujet détouré par l'IA (fond transparent, sans bordure).
 * @param renderedBitmap Image finale compositée affichée sur le canvas (sujet + contour).
 * @param isSegmenting True pendant le calcul du modèle d'inférence MediaPipe.
 * @param isLoadingImage True pendant le décodage initial de l'image.
 * @param isSaving True pendant la compression WebP et l'enregistrement Room.
 * @param isSaveSuccess True lorsque le sticker a été enregistré avec succès.
 * @param borderSizePx Épaisseur actuelle du contour (0 à 32 px).
 * @param borderColor Couleur actuelle du contour au format @ColorInt (par défaut blanc).
 * @param scale Facteur de zoom actuel du sticker sur le canvas tactile.
 * @param rotation Angle de rotation en degrés du sticker.
 * @param offsetX Déplacement horizontal en pixels.
 * @param offsetY Déplacement vertical en pixels.
 * @param canUndo True si une action de contour peut être annulée.
 * @param canRedo True si une action de contour peut être rétablie.
 * @param errorMessage Message d'erreur éventuel à présenter à l'utilisateur.
 * @param layers Liste ordonnée de calques graphiques (sujet, accessoires, textes stylisés).
 * @param selectedLayerId Identifiant du calque actuellement sélectionné par l'utilisateur.
 * @param isTextDialogOpen True lorsque la boîte de dialogue d'édition de texte est ouverte.
 * @param editingTextLayer Calque de texte en cours d'édition (null si ajout d'un nouveau calque).
 * @param isDecorationPickerOpen True lorsque la feuille de sélection d'accessoires est ouverte.
 */
data class StickerEditorUiState(
    val packId: String = "",
    val originalBitmap: Bitmap? = null,
    val cutoutBitmap: Bitmap? = null,
    val renderedBitmap: Bitmap? = null,
    val isSegmenting: Boolean = false,
    val isLoadingImage: Boolean = false,
    val isSaving: Boolean = false,
    val isSaveSuccess: Boolean = false,
    val borderSizePx: Float = 0f,
    val borderColor: Int = Color.WHITE,
    val scale: Float = 1.0f,
    val rotation: Float = 0.0f,
    val offsetX: Float = 0.0f,
    val offsetY: Float = 0.0f,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val errorMessage: String? = null,
    val layers: List<EditorLayer> = emptyList(),
    val selectedLayerId: String? = null,
    val isTextDialogOpen: Boolean = false,
    val editingTextLayer: TextLayer? = null,
    val isDecorationPickerOpen: Boolean = false
) {
    val hasImage: Boolean
        get() = originalBitmap != null

    val isCutout: Boolean
        get() = cutoutBitmap != null

    val selectedLayer: EditorLayer?
        get() = layers.firstOrNull { it.id == selectedLayerId }
}
