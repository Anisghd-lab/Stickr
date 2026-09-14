package com.stickr.app.feature.editor.model

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import java.util.UUID

/**
 * Hiérarchie de calques pour l'éditeur de stickers.
 */
sealed interface EditorLayer {
    val id: String
    val offset: Offset
    val scale: Float
    val rotation: Float

    fun withTransform(newOffset: Offset, newScale: Float, newRotation: Float): EditorLayer
}

/**
 * Calque principal représentant le sujet de la photo (détouré avec contour die-cut).
 */
data class SubjectLayer(
    override val id: String = "subject_layer",
    val bitmap: Bitmap,
    override val offset: Offset = Offset.Zero,
    override val scale: Float = 1.0f,
    override val rotation: Float = 0.0f,
    val borderSizePx: Float = 0f,
    val borderColor: Int = Color.WHITE
) : EditorLayer {
    override fun withTransform(newOffset: Offset, newScale: Float, newRotation: Float): SubjectLayer {
        return copy(offset = newOffset, scale = newScale, rotation = newRotation)
    }
}

/**
 * Calque de texte stylisé avec contour contrasté et police personnalisable (style mème).
 */
data class TextLayer(
    override val id: String = UUID.randomUUID().toString(),
    val text: String,
    val textColor: Int = Color.WHITE,
    val strokeColor: Int = Color.BLACK,
    val strokeWidth: Float = 6f,
    val fontSize: Float = 42f,
    val fontFamilyName: String = "Impact",
    override val offset: Offset = Offset.Zero,
    override val scale: Float = 1.0f,
    override val rotation: Float = 0.0f
) : EditorLayer {
    override fun withTransform(newOffset: Offset, newScale: Float, newRotation: Float): TextLayer {
        return copy(offset = newOffset, scale = newScale, rotation = newRotation)
    }
}

/**
 * Calque d'accessoire ou décoration (lunettes de soleil, flammes, couronne, bulles).
 */
data class DecorationLayer(
    override val id: String = UUID.randomUUID().toString(),
    val assetPath: String, // Emoji ou identifiant d'accessoire vectoriel (ex: "😎", "🔥", "👑", "💬")
    val sizePx: Float = 72f,
    override val offset: Offset = Offset.Zero,
    override val scale: Float = 1.0f,
    override val rotation: Float = 0.0f
) : EditorLayer {
    override fun withTransform(newOffset: Offset, newScale: Float, newRotation: Float): DecorationLayer {
        return copy(offset = newOffset, scale = newScale, rotation = newRotation)
    }
}
