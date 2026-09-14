package com.stickr.app.core.image

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StickerExporterTest {

    @Test
    fun `constants follow strict WhatsApp sticker specifications`() {
        assertEquals(512, StickerExporter.WHATSAPP_CANVAS_SIZE)
        assertEquals(16, StickerExporter.WHATSAPP_SAFETY_MARGIN_PX)
        assertEquals(102400, StickerExporter.MAX_WHATSAPP_SIZE_BYTES)
    }

    @Test
    fun `renderOn512Canvas produces exact 512x512 canvas for landscape source`() {
        val wideSource = Bitmap.createBitmap(1200, 600, Bitmap.Config.ARGB_8888)
        val canvasBitmap = StickerExporter.renderOn512Canvas(wideSource, marginPx = 16)

        assertEquals(512, canvasBitmap.width)
        assertEquals(512, canvasBitmap.height)
    }

    @Test
    fun `renderOn512Canvas produces exact 512x512 canvas for portrait source`() {
        val tallSource = Bitmap.createBitmap(400, 1600, Bitmap.Config.ARGB_8888)
        val canvasBitmap = StickerExporter.renderOn512Canvas(tallSource, marginPx = 16)

        assertEquals(512, canvasBitmap.width)
        assertEquals(512, canvasBitmap.height)
    }

    @Test
    fun `prepareForWhatsApp returns valid bytes strictly below 100 KB`() = runBlocking {
        val source = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
        // Fill some pixels
        for (x in 50 until 250) {
            for (y in 50 until 250) {
                source.setPixel(x, y, Color.RED)
            }
        }

        val webpBytes = StickerExporter.prepareForWhatsApp(source)

        assertNotNull(webpBytes)
        assertTrue("Le fichier doit être non vide", webpBytes.isNotEmpty())
        assertTrue(
            "Le fichier doit être strictement inférieur à 100 Ko (${webpBytes.size} octets reçus)",
            webpBytes.size < StickerExporter.MAX_WHATSAPP_SIZE_BYTES
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `prepareForWhatsApp throws IllegalArgumentException on recycled bitmap`() = runBlocking {
        val source = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        source.recycle()

        StickerExporter.prepareForWhatsApp(source)
        Unit
    }
}
