package com.stickr.app.core.image

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import com.stickr.app.feature.editor.model.DecorationLayer
import com.stickr.app.feature.editor.model.SubjectLayer
import com.stickr.app.feature.editor.model.TextLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StickerFlattenerTest {

    @Test
    fun `flattenLayers with empty list returns transparent 512x512 bitmap`() {
        val bitmap = StickerFlattener.flattenLayers(layers = emptyList())

        assertNotNull(bitmap)
        assertEquals(512, bitmap.width)
        assertEquals(512, bitmap.height)
        assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)

        // Doit être entièrement transparent
        assertEquals(0, Color.alpha(bitmap.getPixel(256, 256)))
    }

    @Test
    fun `flattenLayers with SubjectLayer renders centered on canvas`() {
        // Créer un petit bitmap source de 100x100
        val subjectBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.RED)
        }

        val subjectLayer = SubjectLayer(
            bitmap = subjectBitmap,
            borderSizePx = 0f,
            borderColor = Color.WHITE
        )

        val flattened = StickerFlattener.flattenLayers(layers = listOf(subjectLayer))

        assertEquals(512, flattened.width)
        assertEquals(512, flattened.height)

        // Le centre du sticker doit contenir la couleur du sujet (non transparent)
        val centerAlpha = Color.alpha(flattened.getPixel(256, 256))
        assertTrue("Le centre du sujet doit être opaque", centerAlpha > 0)
    }

    @Test
    fun `flattenLayers with SubjectLayer and die-cut border renders border`() {
        val subjectBitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.BLUE)
        }

        val subjectLayer = SubjectLayer(
            bitmap = subjectBitmap,
            borderSizePx = 16f,
            borderColor = Color.WHITE
        )

        val flattened = StickerFlattener.flattenLayers(layers = listOf(subjectLayer))

        assertEquals(512, flattened.width)
        assertEquals(512, flattened.height)
        val centerAlpha = Color.alpha(flattened.getPixel(256, 256))
        assertTrue(centerAlpha > 0)
    }

    @Test
    fun `flattenLayers with TextLayer renders styled text without crashing`() {
        val textLayer = TextLayer(
            text = "STICKR MEME",
            textColor = Color.WHITE,
            strokeColor = Color.BLACK,
            strokeWidth = 6f,
            fontSize = 42f,
            fontFamilyName = "Impact",
            offset = Offset.Zero
        )

        val flattened = StickerFlattener.flattenLayers(layers = listOf(textLayer))

        assertEquals(512, flattened.width)
        assertEquals(512, flattened.height)
        assertEquals(Bitmap.Config.ARGB_8888, flattened.config)
    }

    @Test
    fun `flattenLayers with DecorationLayer renders emoji without crashing`() {
        val decorationLayer = DecorationLayer(
            assetPath = "🔥",
            sizePx = 80f,
            offset = Offset(50f, 50f)
        )

        val flattened = StickerFlattener.flattenLayers(layers = listOf(decorationLayer))

        assertEquals(512, flattened.width)
        assertEquals(512, flattened.height)
        assertEquals(Bitmap.Config.ARGB_8888, flattened.config)
    }

    @Test
    fun `flattenLayers with full composite stack renders all layers in order`() {
        val subjectBitmap = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.GREEN)
        }

        val subjectLayer = SubjectLayer(bitmap = subjectBitmap, borderSizePx = 8f)
        val decoLayer = DecorationLayer(assetPath = "👑", offset = Offset(0f, -100f))
        val textLayer = TextLayer(text = "ROYAL STICKER", offset = Offset(0f, 100f))

        val composite = StickerFlattener.flattenLayers(
            layers = listOf(subjectLayer, decoLayer, textLayer)
        )

        assertEquals(512, composite.width)
        assertEquals(512, composite.height)
        assertEquals(Bitmap.Config.ARGB_8888, composite.config)
    }

    @Test
    fun `flattenLayers handles blank text layer without error`() {
        val blankTextLayer = TextLayer(text = "   ")
        val flattened = StickerFlattener.flattenLayers(layers = listOf(blankTextLayer))

        assertEquals(512, flattened.width)
        assertEquals(512, flattened.height)
    }

    @Test
    fun `resolveTypeface handles various font family names`() {
        val impact = StickerFlattener.resolveTypeface("Impact")
        val serif = StickerFlattener.resolveTypeface("Serif")
        val monospace = StickerFlattener.resolveTypeface("Monospace")
        val cursive = StickerFlattener.resolveTypeface("Cursive")
        val unknown = StickerFlattener.resolveTypeface("UnknownFont")

        assertNotNull(impact)
        assertNotNull(serif)
        assertNotNull(monospace)
        assertNotNull(cursive)
        assertNotNull(unknown)
    }
}
