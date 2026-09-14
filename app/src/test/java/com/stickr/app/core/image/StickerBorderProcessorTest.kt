package com.stickr.app.core.image

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StickerBorderProcessorTest {

    @Test
    fun `addStickerBorder with positive size expands canvas dimensions correctly`() {
        val width = 100
        val height = 80
        val borderSize = 24f
        val source = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val result = StickerBorderProcessor.addStickerBorder(
            source = source,
            borderSizePx = borderSize,
            borderColor = Color.WHITE,
            expandCanvas = true
        )

        assertNotNull(result)
        val expectedPadding = borderSize.toInt()
        assertEquals(width + (expectedPadding * 2), result.width)
        assertEquals(height + (expectedPadding * 2), result.height)
    }

    @Test
    fun `addStickerBorder with zero border size preserves original dimensions`() {
        val width = 120
        val height = 120
        val source = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val result = StickerBorderProcessor.addStickerBorder(
            source = source,
            borderSizePx = 0f
        )

        assertNotNull(result)
        assertEquals(width, result.width)
        assertEquals(height, result.height)
    }

    @Test
    fun `addStickerBorder with expandCanvas false keeps exact source dimensions`() {
        val width = 200
        val height = 150
        val source = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val result = StickerBorderProcessor.addStickerBorder(
            source = source,
            borderSizePx = 16f,
            expandCanvas = false
        )

        assertEquals(width, result.width)
        assertEquals(height, result.height)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `addStickerBorder throws IllegalArgumentException when source is recycled`() {
        val source = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        source.recycle()

        StickerBorderProcessor.addStickerBorder(source)
    }

    @Test
    fun `extension function Bitmap_addStickerBorder delegates properly`() {
        val source = Bitmap.createBitmap(60, 60, Bitmap.Config.ARGB_8888)
        val result = source.addStickerBorder(borderSizePx = 10f, expandCanvas = true)

        assertEquals(80, result.width)
        assertEquals(80, result.height)
    }
}
