package com.stickr.app.feature.packdetail

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.stickr.app.core.data.repository.StickerPackRepository
import com.stickr.app.core.database.entity.StickerItemEntity
import com.stickr.app.core.database.entity.StickerPackEntity
import com.stickr.app.core.database.entity.StickerPackWithStickers
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PackDetailViewModelTest {

    private val repository: StickerPackRepository = mockk(relaxed = true)
    private lateinit var viewModel: PackDetailViewModel
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private val testPack = StickerPackWithStickers(
        pack = StickerPackEntity(id = "pack_test", name = "Test Pack", publisher = "Stickr Author"),
        stickers = listOf(
            StickerItemEntity(id = "s_1", packId = "pack_test", imagePath = "/path/1.webp", emojis = "✨"),
            StickerItemEntity(id = "s_2", packId = "pack_test", imagePath = "/path/2.webp", emojis = "🔥")
        )
    )

    @Before
    fun setup() {
        every { repository.getPackById("pack_test") } returns flowOf(testPack)
        val savedStateHandle = SavedStateHandle(mapOf("packId" to "pack_test"))
        viewModel = PackDetailViewModel(repository, savedStateHandle)
    }

    @Test
    fun `pack details are observed and mapped correctly`() {
        val state = viewModel.uiState.value
        assertEquals("pack_test", state.packId)
        assertEquals(2, state.stickerCount)
        assertFalse(state.isWhatsAppReady)
        assertEquals(1, state.missingStickersCount)
    }

    @Test
    fun `deleteSticker calls repository deleteSticker`() {
        viewModel.deleteSticker("s_1")
        coVerify { repository.deleteSticker("s_1") }
    }

    @Test
    fun `updateStickerEmojis calls repository updateStickerEmojis`() {
        viewModel.updateStickerEmojis("s_1", "🎉,🚀")
        coVerify { repository.updateStickerEmojis("s_1", "🎉,🚀") }
    }

    @Test
    fun `exportToWhatsApp denies export when pack has less than 3 stickers`() {
        var feedback = ""
        viewModel.exportToWhatsApp(context) { msg ->
            feedback = msg
        }

        assertTrue(
            "Doit signaler le manque de stickers",
            feedback.contains("au moins 3 stickers")
        )
    }
}
