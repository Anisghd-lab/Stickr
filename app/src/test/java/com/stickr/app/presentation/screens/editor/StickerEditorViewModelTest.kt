package com.stickr.app.presentation.screens.editor

import android.content.Context
import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.stickr.app.domain.usecase.SaveStickerUseCase
import com.stickr.app.domain.usecase.SegmentImageUseCase
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
    private val saveStickerUseCase: SaveStickerUseCase = mockk(relaxed = true)
    private lateinit var viewModel: StickerEditorViewModel

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val savedStateHandle = SavedStateHandle(mapOf("packId" to 101L))
        viewModel = StickerEditorViewModel(
            context = context,
            segmentImageUseCase = segmentImageUseCase,
            saveStickerUseCase = saveStickerUseCase,
            savedStateHandle = savedStateHandle
        )
    }

    @Test
    fun `initial state contains valid defaults and correct packId`() {
        val state = viewModel.uiState.value

        assertEquals(101L, state.packId)
        assertFalse(state.hasImage)
        assertFalse(state.isCutout)
        assertFalse(state.isSegmenting)
        assertFalse(state.canUndo)
        assertFalse(state.canRedo)
        assertEquals(0f, state.borderSizePx, 0.01f)
        assertEquals(Color.WHITE, state.borderColor)
        assertEquals(1.0f, state.scale, 0.01f)
        assertEquals(0.0f, state.rotation, 0.01f)
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
}
