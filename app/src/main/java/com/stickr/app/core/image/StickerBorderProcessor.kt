package com.stickr.app.core.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.annotation.ColorInt
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

/**
 * Processeur graphique pour la génération de contours nets d'autocollants (die-cut border).
 *
 * Applique une dilatation radiale optimisée sur le canal alpha du bitmap source
 * pour générer un contour plein, uniforme, lisse et anti-aliasé, sans flou parasite
 * ni allocations superflues.
 */
object StickerBorderProcessor {

    private const val DEFAULT_BORDER_SIZE_PX = 24f
    private const val DEFAULT_BORDER_COLOR = Color.WHITE
    private const val RING_STEP_PX = 3f // Pas radial optimal pour concilier vitesse et absence de trous

    /**
     * Ajoute un contour net (die-cut sticker border) autour du sujet d'un Bitmap transparent.
     *
     * @param source Le Bitmap source (doit contenir un sujet sur fond transparent).
     * @param borderSizePx Épaisseur de la bordure en pixels (par défaut 24px).
     * @param borderColor Couleur de la bordure au format @ColorInt (par défaut blanc).
     * @param expandCanvas Si true (recommandé), agrandit le canvas de l'épaisseur de la bordure
     *                     pour éviter que celle-ci ne soit rognée sur les bords.
     * @return Nouveau Bitmap ARGB_8888 avec le contour sticker appliqué.
     */
    fun addStickerBorder(
        source: Bitmap,
        borderSizePx: Float = DEFAULT_BORDER_SIZE_PX,
        @ColorInt borderColor: Int = DEFAULT_BORDER_COLOR,
        expandCanvas: Boolean = true
    ): Bitmap {
        require(!source.isRecycled) { "Le Bitmap source est déjà recyclé." }
        if (source.width <= 0 || source.height <= 0) return source

        // Si aucune bordure demandée, retourner une copie propre
        if (borderSizePx <= 0f) {
            return if (source.config != Bitmap.Config.ARGB_8888) {
                source.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                source.copy(source.config ?: Bitmap.Config.ARGB_8888, true)
            }
        }

        // Sécurité contre la configuration HARDWARE
        val safeSource = if (source.config == Bitmap.Config.HARDWARE || source.config != Bitmap.Config.ARGB_8888) {
            source.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            safeSourceReference(source)
        }

        val padding = if (expandCanvas) ceil(borderSizePx).toInt() else 0
        val outWidth = safeSource.width + (padding * 2)
        val outHeight = safeSource.height + (padding * 2)

        val outputBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)

        // Extraction du masque alpha pur (ALPHA_8)
        val alphaMask = safeSource.extractAlpha()

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            color = borderColor
            isDither = true
        }

        val originX = padding.toFloat()
        val originY = padding.toFloat()

        // 1. Dessiner le masque alpha au centre
        canvas.drawBitmap(alphaMask, originX, originY, borderPaint)

        // 2. Dilatation radiale par cercles concentriques pour former le contour die-cut plein
        var currentRadius = RING_STEP_PX
        while (currentRadius <= borderSizePx) {
            val circumference = 2.0 * Math.PI * currentRadius
            val numSteps = maxOf(8, (circumference / RING_STEP_PX).toInt())
            val angleStep = (2.0 * Math.PI) / numSteps

            for (i in 0 until numSteps) {
                val angle = i * angleStep
                val dx = originX + (currentRadius * cos(angle)).toFloat()
                val dy = originY + (currentRadius * sin(angle)).toFloat()
                canvas.drawBitmap(alphaMask, dx, dy, borderPaint)
            }

            currentRadius += RING_STEP_PX
        }

        // Si le dernier cercle n'atteint pas exactement borderSizePx, dessiner le cercle frontière extérieur
        if (currentRadius - RING_STEP_PX < borderSizePx) {
            val outerRadius = borderSizePx
            val circumference = 2.0 * Math.PI * outerRadius
            val numSteps = maxOf(12, (circumference / RING_STEP_PX).toInt())
            val angleStep = (2.0 * Math.PI) / numSteps

            for (i in 0 until numSteps) {
                val angle = i * angleStep
                val dx = originX + (outerRadius * cos(angle)).toFloat()
                val dy = originY + (outerRadius * sin(angle)).toFloat()
                canvas.drawBitmap(alphaMask, dx, dy, borderPaint)
            }
        }

        // 3. Dessiner l'image source originale par-dessus le contour blanc
        canvas.drawBitmap(safeSource, originX, originY, null)

        // Libération de la mémoire
        alphaMask.recycle()
        if (safeSource != source) {
            safeSource.recycle()
        }

        return outputBitmap
    }

    private fun safeSourceReference(source: Bitmap): Bitmap = source
}

/**
 * Extension pratique pour appliquer une bordure sticker directement sur n'importe quel [Bitmap].
 */
fun Bitmap.addStickerBorder(
    borderSizePx: Float = 24f,
    @ColorInt borderColor: Int = Color.WHITE,
    expandCanvas: Boolean = true
): Bitmap {
    return StickerBorderProcessor.addStickerBorder(this, borderSizePx, borderColor, expandCanvas)
}
