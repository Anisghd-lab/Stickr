package com.stickr.app.presentation.screens.editor

import android.content.Context
import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.stickr.app.core.data.repository.StickerPackRepository
import com.stickr.app.domain.usecase.SegmentImageUseCase
import com.stickr.app.feature.editor.model.DecorationLayer
import com.stickr.app.feature.editor.model.TextLayer
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StickerEditorViewModelTest {

    private lateinit var context: Context
    private val segmentImageUseCase: SegmentImageUseCase = mockk(relaxed = true)
    private val repository: StickerPackRepository = mockk(relaxed = true)
    private lateinit var viewModel: StickerEditorViewModel

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val savedStateHandle = SavedStateHandle(mapOf("packId" to "pack_101"))
        viewModel = StickerEditorViewModel(
            context = context,
            segmentImageUseCase = segmentImageUseCase,
            repository = repository,
            savedStateHandle = savedStateHandle
        )
    }

    @Test
    fun `initial state contains valid defaults and correct packId`() {
        val state = viewModel.uiState.value

        assertEquals("pack_101", state.packId)
        assertFalse(state.hasImage)
        assertFalse(state.isCutout)
        assertFalse(state.isSegmenting)
        assertFalse(state.canUndo)
        assertFalse(state.canRedo)
        assertEquals(0f, state.borderSizePx, 0.01f)
        assertEquals(Color.WHITE, state.borderColor)
        assertEquals(1.0f, state.scale, 0.01f)
        assertEquals(0.0f, state.rotation, 0.01f)
        assertTrue(state.layers.isEmpty())
        assertNull(state.selectedLayerId)
        assertFalse(state.isTextDialogOpen)
        assertFalse(state.isDecorationPickerOpen)
    }

    @Test
    fun `onTransformGesture updates scale rotation and offsets`() {
        viewModel.onTransformGesture(
            pan = Offset(20f, -15f),
            zoom = 1.5f,
            rotate = 45f
        )

        val state = viewModel.uiState.value
        assertEquals(1.5f, state.scale, 0.01f)
        assertEquals(45f, state.rotation, 0.01f)
        assertEquals(20f, state.offsetX, 0.01f)
        assertEquals(-15f, state.offsetY, 0.01f)
    }

    @Test
    fun `resetTransform restores neutral framing`() {
        viewModel.onTransformGesture(Offset(100f, 50f), 2.5f, 90f)
        viewModel.resetTransform()

        val state = viewModel.uiState.value
        assertEquals(1.0f, state.scale, 0.01f)
        assertEquals(0.0f, state.rotation, 0.01f)
        assertEquals(0.0f, state.offsetX, 0.01f)
        assertEquals(0.0f, state.offsetY, 0.01f)
    }

    @Test
    fun `border adjustment supports undo and redo operations`() {
        assertFalse(viewModel.uiState.value.canUndo)
        assertFalse(viewModel.uiState.value.canRedo)

        // 1. Définir une première bordure avec commit
        viewModel.setBorderSize(12f, commitToHistory = true)
        assertEquals(12f, viewModel.uiState.value.borderSizePx, 0.01f)
        assertTrue(viewModel.uiState.value.canUndo)
        assertFalse(viewModel.uiState.value.canRedo)

        // 2. Définir une deuxième bordure
        viewModel.setBorderSize(24f, commitToHistory = true)
        assertEquals(24f, viewModel.uiState.value.borderSizePx, 0.01f)

        // 3. Annuler (Undo) -> Doit revenir à 12f
        viewModel.undo()
        assertEquals(12f, viewModel.uiState.value.borderSizePx, 0.01f)
        assertTrue(viewModel.uiState.value.canRedo)

        // 4. Annuler encore (Undo) -> Doit revenir à 0f
        viewModel.undo()
        assertEquals(0f, viewModel.uiState.value.borderSizePx, 0.01f)
        assertFalse(viewModel.uiState.value.canUndo)
        assertTrue(viewModel.uiState.value.canRedo)

        // 5. Rétablir (Redo) -> Doit revenir à 12f
        viewModel.redo()
        assertEquals(12f, viewModel.uiState.value.borderSizePx, 0.01f)
        assertTrue(viewModel.uiState.value.canUndo)
    }

    @Test
    fun `setBorderColor updates color and enables undo`() {
        val newColor = Color.YELLOW
        viewModel.setBorderColor(newColor)

        val state = viewModel.uiState.value
        assertEquals(newColor, state.borderColor)
        assertTrue(state.canUndo)
    }

    @Test
    fun `addTextLayer appends layer and sets active selection`() {
        viewModel.addTextLayer(
            text = "STICKR MEME",
            textColor = Color.YELLOW,
            strokeColor = Color.BLACK,
            strokeWidth = 8f,
            fontSize = 48f,
            fontFamilyName = "Impact"
        )

        val state = viewModel.uiState.value
        assertEquals(1, state.layers.size)

        val added = state.layers.first() as TextLayer
        assertEquals("STICKR MEME", added.text)
        assertEquals(Color.YELLOW, added.textColor)
        assertEquals(Color.BLACK, added.strokeColor)
        assertEquals(8f, added.strokeWidth, 0.01f)
        assertEquals(48f, added.fontSize, 0.01f)
        assertEquals("Impact", added.fontFamilyName)
        assertEquals(added.id, state.selectedLayerId)
    }

    @Test
    fun `updateTextLayer modifies specific text layer attributes`() {
        viewModel.addTextLayer(text = "INITIAL")
        val initialId = viewModel.uiState.value.selectedLayerId!!

        viewModel.updateTextLayer(
            id = initialId,
            text = "MODIFIED",
            textColor = Color.CYAN,
            strokeColor = Color.RED,
            strokeWidth = 4f,
            fontSize = 36f,
            fontFamilyName = "Monospace"
        )

        val state = viewModel.uiState.value
        val updated = state.layers.first() as TextLayer
        assertEquals("MODIFIED", updated.text)
        assertEquals(Color.CYAN, updated.textColor)
        assertEquals(Color.RED, updated.strokeColor)
        assertEquals(4f, updated.strokeWidth, 0.01f)
        assertEquals(36f, updated.fontSize, 0.01f)
        assertEquals("Monospace", updated.fontFamilyName)
    }

    @Test
    fun `addDecorationLayer appends emoji layer and selects it`() {
        viewModel.addDecorationLayer("🔥")

        val state = viewModel.uiState.value
        assertEquals(1, state.layers.size)

        val deco = state.layers.first() as DecorationLayer
        assertEquals("🔥", deco.assetPath)
        assertEquals(deco.id, state.selectedLayerId)
    }

    @Test
    fun `removeLayer deletes layer from list and updates selection`() {
        viewModel.addTextLayer("Text 1")
        val id1 = viewModel.uiState.value.selectedLayerId!!
        viewModel.addDecorationLayer("👑")
        val id2 = viewModel.uiState.value.selectedLayerId!!

        assertEquals(2, viewModel.uiState.value.layers.size)
        assertEquals(id2, viewModel.uiState.value.selectedLayerId)

        viewModel.removeLayer(id2)

        val state = viewModel.uiState.value
        assertEquals(1, state.layers.size)
        assertEquals(id1, state.layers.first().id)
        assertEquals(id1, state.selectedLayerId)
    }

    @Test
    fun `updateSelectedLayerTransform affects targeted active layer`() {
        viewModel.addDecorationLayer("😎")
        val layerId = viewModel.uiState.value.selectedLayerId!!

        viewModel.updateSelectedLayerTransform(
            pan = Offset(30f, 40f),
            zoom = 2.0f,
            rotate = 30f
        )

        val layer = viewModel.uiState.value.layers.first() as DecorationLayer
        assertEquals(30f, layer.offset.x, 0.01f)
        assertEquals(40f, layer.offset.y, 0.01f)
        assertEquals(2.0f, layer.scale, 0.01f)
        assertEquals(30f, layer.rotation, 0.01f)
    }

    @Test
    fun `dialog controls toggle state as expected`() {
        assertFalse(viewModel.uiState.value.isTextDialogOpen)
        assertFalse(viewModel.uiState.value.isDecorationPickerOpen)

        viewModel.openAddTextDialog()
        assertTrue(viewModel.uiState.value.isTextDialogOpen)
        assertNull(viewModel.uiState.value.editingTextLayer)

        viewModel.closeTextDialog()
        assertFalse(viewModel.uiState.value.isTextDialogOpen)

        viewModel.openDecorationPicker()
        assertTrue(viewModel.uiState.value.isDecorationPickerOpen)

        viewModel.closeDecorationPicker()
        assertFalse(viewModel.uiState.value.isDecorationPickerOpen)
    }
}
