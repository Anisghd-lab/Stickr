package com.stickr.app.core.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import com.stickr.app.feature.editor.model.DecorationLayer
import com.stickr.app.feature.editor.model.EditorLayer
import com.stickr.app.feature.editor.model.SubjectLayer
import com.stickr.app.feature.editor.model.TextLayer

/**
 * Moteur d'aplatissement graphique : fusionne l'ensemble des calques interactifs
 * (sujet détouré, contour die-cut, calques de texte stylisé, accessoires)
 * en un unique Bitmap 512x512 px avec transparence préservée, prêt pour WhatsApp.
 */
object StickerFlattener {

    const val DEFAULT_CANVAS_SIZE = 512
    const val DEFAULT_MARGIN_PX = 16

    /**
     * Fusionne les calques dans l'ordre de leur empilement.
     *
     * @param canvasSize Résolution carrée de sortie (512x512 px pour WhatsApp).
     * @param layers Liste ordonnée des calques à aplatir.
     * @param marginPx Marge de sécurité pour le sujet principal.
     * @return [Bitmap] ARGB_8888 aplati.
     */
    fun flattenLayers(
        canvasSize: Int = DEFAULT_CANVAS_SIZE,
        layers: List<EditorLayer>,
        marginPx: Int = DEFAULT_MARGIN_PX
    ): Bitmap {
        val outputBitmap = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)

        for (layer in layers) {
            when (layer) {
                is SubjectLayer -> renderSubjectLayer(canvas, canvasSize, layer, marginPx)
                is TextLayer -> renderTextLayer(canvas, canvasSize, layer)
                is DecorationLayer -> renderDecorationLayer(canvas, canvasSize, layer)
            }
        }

        return outputBitmap
    }

    private fun renderSubjectLayer(
        canvas: Canvas,
        canvasSize: Int,
        layer: SubjectLayer,
        marginPx: Int
    ) {
        if (layer.bitmap.isRecycled) return

        // Application du contour sticker si nécessaire
        val processedBitmap = if (layer.borderSizePx > 0f) {
            StickerBorderProcessor.addStickerBorder(
                source = layer.bitmap,
                borderSizePx = layer.borderSizePx,
                borderColor = layer.borderColor,
                expandCanvas = true
            )
        } else {
            layer.bitmap
        }

        val maxDim = (canvasSize - (marginPx * 2)).toFloat()
        val scaleFactor = minOf(maxDim / processedBitmap.width, maxDim / processedBitmap.height)
        val destW = processedBitmap.width * scaleFactor
        val destH = processedBitmap.height * scaleFactor

        val centerX = (canvasSize / 2f) + layer.offset.x
        val centerY = (canvasSize / 2f) + layer.offset.y

        canvas.save()
        canvas.translate(centerX, centerY)
        canvas.rotate(layer.rotation)
        canvas.scale(layer.scale, layer.scale)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val destRect = RectF(-destW / 2f, -destH / 2f, destW / 2f, destH / 2f)
        canvas.drawBitmap(processedBitmap, null, destRect, paint)

        canvas.restore()

        if (processedBitmap != layer.bitmap) {
            processedBitmap.recycle()
        }
    }

    private fun renderTextLayer(
        canvas: Canvas,
        canvasSize: Int,
        layer: TextLayer
    ) {
        if (layer.text.isBlank()) return

        val typeface = resolveTypeface(layer.fontFamilyName)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = layer.fontSize
            textAlign = Paint.Align.CENTER
            isDither = true
        }

        val bounds = Rect()
        textPaint.getTextBounds(layer.text, 0, layer.text.length, bounds)
        val textYOffset = bounds.height() / 2f

        val centerX = (canvasSize / 2f) + layer.offset.x
        val centerY = (canvasSize / 2f) + layer.offset.y

        canvas.save()
        canvas.translate(centerX, centerY)
        canvas.rotate(layer.rotation)
        canvas.scale(layer.scale, layer.scale)

        // 1. Contour de lettre (Stroke) pour la lisibilité sur tout fond WhatsApp
        if (layer.strokeWidth > 0f) {
            textPaint.style = Paint.Style.STROKE
            textPaint.strokeWidth = layer.strokeWidth * 2f // Compensation optique
            textPaint.strokeJoin = Paint.Join.ROUND
            textPaint.strokeCap = Paint.Cap.ROUND
            textPaint.color = layer.strokeColor
            canvas.drawText(layer.text, 0f, textYOffset, textPaint)
        }

        // 2. Remplissage de lettre (Fill)
        textPaint.style = Paint.Style.FILL
        textPaint.color = layer.textColor
        canvas.drawText(layer.text, 0f, textYOffset, textPaint)

        canvas.restore()
    }

    private fun renderDecorationLayer(
        canvas: Canvas,
        canvasSize: Int,
        layer: DecorationLayer
    ) {
        if (layer.assetPath.isBlank()) return

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = layer.sizePx
            textAlign = Paint.Align.CENTER
        }

        val bounds = Rect()
        textPaint.getTextBounds(layer.assetPath, 0, layer.assetPath.length, bounds)
        val textYOffset = bounds.height() / 2f

        val centerX = (canvasSize / 2f) + layer.offset.x
        val centerY = (canvasSize / 2f) + layer.offset.y

        canvas.save()
        canvas.translate(centerX, centerY)
        canvas.rotate(layer.rotation)
        canvas.scale(layer.scale, layer.scale)

        canvas.drawText(layer.assetPath, 0f, textYOffset, textPaint)

        canvas.restore()
    }

    fun resolveTypeface(fontName: String): Typeface {
        return when (fontName.lowercase()) {
            "impact" -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            "serif" -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
            "monospace" -> Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            "cursive" -> Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
            else -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
    }
}
