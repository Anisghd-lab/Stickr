package com.stickr.app.feature.dashboard

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stickr.app.core.data.repository.StickerPackRepository
import com.stickr.app.core.database.entity.StickerItemEntity
import com.stickr.app.core.database.entity.StickerPackEntity
import com.stickr.app.core.database.entity.StickerPackWithStickers
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DashboardViewModelTest {

    private val repository: StickerPackRepository = mockk(relaxed = true)
    private lateinit var viewModel: DashboardViewModel
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        every { repository.getAllPacks() } returns flowOf(emptyList())
        viewModel = DashboardViewModel(repository)
    }

    @Test
    fun `createNewPack fails with error message when name or publisher is blank`() {
        var createdPackId: String? = null

        viewModel.createNewPack("", "Auteur") { id -> createdPackId = id }
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertEquals(null, createdPackId)

        viewModel.createNewPack("Nom", "   ") { id -> createdPackId = id }
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertEquals(null, createdPackId)
    }

    @Test
    fun `exportToWhatsApp gives error feedback if pack has less than 3 stickers`() {
        val incompletePack = StickerPackWithStickers(
            pack = StickerPackEntity(id = "p1", name = "Test", publisher = "User"),
            stickers = listOf(
                StickerItemEntity(id = "s1", packId = "p1", imagePath = "/path/1.webp"),
                StickerItemEntity(id = "s2", packId = "p1", imagePath = "/path/2.webp")
            )
        )

        var feedbackMessage = ""
        viewModel.exportToWhatsApp(context, incompletePack) { msg ->
            feedbackMessage = msg
        }

        assertTrue(
            "Le message doit indiquer le minimum requis de 3 stickers",
            feedbackMessage.contains("au moins 3 stickers")
        )
    }

    @Test
    fun `exportToWhatsApp attempts export when pack has 3 or more stickers`() {
        val readyPack = StickerPackWithStickers(
            pack = StickerPackEntity(id = "p2", name = "Ready Pack", publisher = "User"),
            stickers = listOf(
                StickerItemEntity(id = "s1", packId = "p2", imagePath = "/path/1.webp"),
                StickerItemEntity(id = "s2", packId = "p2", imagePath = "/path/2.webp"),
                StickerItemEntity(id = "s3", packId = "p2", imagePath = "/path/3.webp")
            )
        )

        coEvery { repository.ensureTrayIcon("p2") } returns "/files/tray/tray_p2.png"

        var feedbackMessage = ""
        viewModel.exportToWhatsApp(context, readyPack) { msg ->
            feedbackMessage = msg
        }

        coVerify { repository.ensureTrayIcon("p2") }
    }
}
