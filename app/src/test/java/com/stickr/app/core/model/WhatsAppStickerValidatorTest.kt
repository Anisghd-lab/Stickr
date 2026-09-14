package com.stickr.app.core.model

import android.graphics.Bitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WhatsAppStickerValidatorTest {

    private fun createValidStickers(count: Int): List<WhatsAppStickerItem> {
        return (1..count).map {
            WhatsAppStickerItem(
                imageFile = "sticker_$it.webp",
                emojis = listOf("😀", "🎉")
            )
        }
    }

    @Test
    fun `validatePack fails when stickers count is less than 3`() {
        val pack = WhatsAppStickerPack(
            identifier = "pack_1",
            name = "Mon Pack",
            publisher = "Stickr",
            trayImageFile = "tray_1.png",
            stickers = createValidStickers(2) // Seulement 2 stickers
        )

        val result = WhatsAppStickerValidator.validatePack(pack)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("au moins 3 stickers") == true)
    }

    @Test
    fun `validatePack fails when stickers count exceeds 30`() {
        val pack = WhatsAppStickerPack(
            identifier = "pack_1",
            name = "Mon Pack",
            publisher = "Stickr",
            trayImageFile = "tray_1.png",
            stickers = createValidStickers(31) // 31 stickers
        )

        val result = WhatsAppStickerValidator.validatePack(pack)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("plus de 30 stickers") == true)
    }

    @Test
    fun `validatePack succeeds for valid pack with 3 to 30 stickers`() {
        for (count in listOf(3, 15, 30)) {
            val pack = WhatsAppStickerPack(
                identifier = "pack_$count",
                name = "Pack $count",
                publisher = "Stickr Studio",
                trayImageFile = "tray_$count.png",
                stickers = createValidStickers(count)
            )

            val result = WhatsAppStickerValidator.validatePack(pack)
            assertTrue("Pack avec $count stickers doit être valide", result.isSuccess)
        }
    }

    @Test
    fun `validatePack fails when name or publisher is empty`() {
        val emptyNamePack = WhatsAppStickerPack(
            identifier = "pack_1",
            name = "   ",
            publisher = "Stickr",
            trayImageFile = "tray_1.png",
            stickers = createValidStickers(5)
        )
        assertTrue(WhatsAppStickerValidator.validatePack(emptyNamePack).isFailure)

        val emptyPublisherPack = WhatsAppStickerPack(
            identifier = "pack_1",
            name = "Cool Pack",
            publisher = "",
            trayImageFile = "tray_1.png",
            stickers = createValidStickers(5)
        )
        assertTrue(WhatsAppStickerValidator.validatePack(emptyPublisherPack).isFailure)
    }

    @Test
    fun `validateStickerItem fails when emojis count is zero or exceeds 3`() {
        val zeroEmoji = WhatsAppStickerItem("s.webp", emojis = emptyList())
        assertTrue(WhatsAppStickerValidator.validateStickerItem(zeroEmoji).isFailure)

        val fourEmojis = WhatsAppStickerItem("s.webp", emojis = listOf("🔥", "❤️", "👍", "🚀"))
        assertTrue(WhatsAppStickerValidator.validateStickerItem(fourEmojis).isFailure)

        val validOneEmoji = WhatsAppStickerItem("s.webp", emojis = listOf("🎉"))
        assertTrue(WhatsAppStickerValidator.validateStickerItem(validOneEmoji).isSuccess)

        val validThreeEmojis = WhatsAppStickerItem("s.webp", emojis = listOf("🎉", "✨", "🔥"))
        assertTrue(WhatsAppStickerValidator.validateStickerItem(validThreeEmojis).isSuccess)
    }

    @Test
    fun `validateTrayIconBitmap validates exact 96x96 pixels`() {
        val validTray = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        assertTrue(WhatsAppStickerValidator.validateTrayIconBitmap(validTray).isSuccess)

        val invalidTray = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        assertTrue(WhatsAppStickerValidator.validateTrayIconBitmap(invalidTray).isFailure)
    }
}
