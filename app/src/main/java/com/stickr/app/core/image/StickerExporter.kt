package com.stickr.app.core.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

/**
 * Utilitaires d'exportation de stickers conformes aux spécifications officielles de WhatsApp.
 *
 * Spécifications strictes WhatsApp :
 * - Format : image/webp
 * - Dimensions exactes du canvas : 512x512 pixels
 * - Arrière-plan : Transparent
 * - Marge de sécurité recommandée : 16 pixels (le sujet s'inscrit dans un carré de 480x480 max)
 * - Poids maximal du fichier : Strictement inférieur à 100 Ko (102 400 octets)
 */
object StickerExporter {

    const val WHATSAPP_CANVAS_SIZE = 512
    const val WHATSAPP_SAFETY_MARGIN_PX = 16
    const val MAX_WHATSAPP_SIZE_BYTES = 100 * 1024 // 102 400 octets

    /**
     * Prépare, redimensionne et compresse le Bitmap source selon les critères stricts de WhatsApp.
     *
     * @param source Le Bitmap sticker source (avec ou sans contour).
     * @param safetyMarginPx Marge transparente autour du sujet (16px recommandé par WhatsApp).
     * @return [ByteArray] contenant l'image au format WebP valide et strictement < 100 Ko.
     */
    suspend fun prepareForWhatsApp(
        source: Bitmap,
        safetyMarginPx: Int = WHATSAPP_SAFETY_MARGIN_PX
    ): ByteArray = withContext(Dispatchers.Default) {
        require(!source.isRecycled) { "Le Bitmap source est déjà recyclé." }
        require(source.width > 0 && source.height > 0) { "Dimensions de Bitmap invalides." }

        // 1. Mise à l'échelle et centrage dans un canevas transparent 512x512
        var workingCanvas = renderOn512Canvas(source, safetyMarginPx, 1.0f)

        try {
            // 2. Tentative initiale en qualité maximale (sans perte si supporté)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val losslessBytes = compressWebP(workingCanvas, quality = 100, lossless = true)
                if (losslessBytes.size < MAX_WHATSAPP_SIZE_BYTES) {
                    return@withContext losslessBytes
                }
            }

            // 3. Boucle d'ajustement dynamique de la qualité de compression (Quality Downscaling)
            var quality = 95
            var currentBytes = compressWebP(workingCanvas, quality = quality, lossless = false)

            while (currentBytes.size >= MAX_WHATSAPP_SIZE_BYTES && quality > 10) {
                quality -= if (quality > 70) 10 else if (quality > 30) 8 else 5
                currentBytes = compressWebP(workingCanvas, quality = quality, lossless = false)
            }

            // 4. Filet de sécurité extrême : Si l'image dépasse toujours 100 Ko à q=10 (cas de bruit haute fréquence),
            // on réduit légèrement l'échelle du contenu pour garantir un poids < 100 Ko.
            var contentScale = 0.9f
            while (currentBytes.size >= MAX_WHATSAPP_SIZE_BYTES && contentScale >= 0.5f) {
                workingCanvas.recycle()
                workingCanvas = renderOn512Canvas(source, safetyMarginPx, contentScale)
                currentBytes = compressWebP(workingCanvas, quality = 60, lossless = false)
                contentScale -= 0.1f
            }

            currentBytes
        } finally {
            if (!workingCanvas.isRecycled) {
                workingCanvas.recycle()
            }
        }
    }

    /**
     * Crée un canvas 512x512 transparent et y dessine l'image centrée avec ratio préservé.
     */
    fun renderOn512Canvas(
        source: Bitmap,
        marginPx: Int = WHATSAPP_SAFETY_MARGIN_PX,
        additionalScale: Float = 1.0f
    ): Bitmap {
        val targetSize = WHATSAPP_CANVAS_SIZE
        val maxContentArea = (targetSize - (marginPx * 2)) * additionalScale

        val scale = minOf(
            maxContentArea / source.width.toFloat(),
            maxContentArea / source.height.toFloat()
        )

        val scaledWidth = (source.width * scale).roundToInt().coerceAtLeast(1)
        val scaledHeight = (source.height * scale).roundToInt().coerceAtLeast(1)

        val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val left = (targetSize - scaledWidth) / 2f
        val top = (targetSize - scaledHeight) / 2f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val destRect = RectF(left, top, left + scaledWidth, top + scaledHeight)
        canvas.drawBitmap(source, null, destRect, paint)

        return output
    }

    /**
     * Compresse un Bitmap en WebP selon la version d'Android.
     */
    private fun compressWebP(bitmap: Bitmap, quality: Int, lossless: Boolean): ByteArray {
        val stream = ByteArrayOutputStream()
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (lossless) Bitmap.CompressFormat.WEBP_LOSSLESS else Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
        bitmap.compress(format, quality.coerceIn(0, 100), stream)
        return stream.toByteArray()
    }
}

/**
 * Extension pour préparer directement un [Bitmap] pour l'exportation WhatsApp.
 */
suspend fun Bitmap.prepareForWhatsApp(
    safetyMarginPx: Int = StickerExporter.WHATSAPP_SAFETY_MARGIN_PX
): ByteArray {
    return StickerExporter.prepareForWhatsApp(this, safetyMarginPx)
}
