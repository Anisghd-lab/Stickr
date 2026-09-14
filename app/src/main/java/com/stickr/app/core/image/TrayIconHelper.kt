package com.stickr.app.core.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.stickr.app.core.model.WhatsAppStickerValidator
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

/**
 * Utilitaire pour la création et la normalisation de l'icône de plateau (tray icon) WhatsApp (96x96 px, < 50 Ko).
 */
object TrayIconHelper {

    const val TRAY_SIZE = WhatsAppStickerValidator.TRAY_ICON_SIZE_PX // 96
    const val TRAY_SAFETY_PADDING_PX = 8

    /**
     * Redimensionne et centre n'importe quel Bitmap sur un canvas transparent de 96x96 px.
     */
    fun createTrayIconBitmap(
        source: Bitmap,
        paddingPx: Int = TRAY_SAFETY_PADDING_PX
    ): Bitmap {
        require(!source.isRecycled) { "Le Bitmap source est recyclé." }
        val maxDim = (TRAY_SIZE - (paddingPx * 2)).toFloat()
        val scale = minOf(maxDim / source.width, maxDim / source.height)

        val scaledW = (source.width * scale).roundToInt().coerceAtLeast(1)
        val scaledH = (source.height * scale).roundToInt().coerceAtLeast(1)

        val output = Bitmap.createBitmap(TRAY_SIZE, TRAY_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val left = (TRAY_SIZE - scaledW) / 2f
        val top = (TRAY_SIZE - scaledH) / 2f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val dest = RectF(left, top, left + scaledW, top + scaledH)
        canvas.drawBitmap(source, null, dest, paint)

        return output
    }

    /**
     * Compresse un Bitmap d'icône de plateau en PNG (ou WebP) garanti < 50 Ko.
     */
    fun compressTrayIconToPng(bitmap: Bitmap): ByteArray {
        val safeBitmap = if (bitmap.width != TRAY_SIZE || bitmap.height != TRAY_SIZE) {
            createTrayIconBitmap(bitmap)
        } else {
            bitmap
        }

        val stream = ByteArrayOutputStream()
        safeBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val bytes = stream.toByteArray()

        if (safeBitmap != bitmap) {
            safeBitmap.recycle()
        }

        return bytes
    }
}
