package com.stickr.app.presentation.screens.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Écran d'édition graphique tactile (StickerEditorScreen).
 *
 * Combine l'importation moderne sans permission (Photo Picker), le détourage IA automatique
 * sur l'appareil (MediaPipe), la manipulation gestuelle multitouch et l'exportation stricte WebP.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerEditorScreen(
    viewModel: StickerEditorViewModel,
    initialImageUri: String? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Sélecteur d'images moderne Android (PickVisualMedia) sans permissions invasives
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadFromUri(it) }
    }

    // Chargement initial si un URI d'image a été passé en argument de navigation
    LaunchedEffect(initialImageUri) {
        if (!initialImageUri.isNullOrBlank()) {
            runCatching {
                val uri = Uri.parse(initialImageUri)
                viewModel.loadFromUri(uri)
            }
        }
    }

    // Notification d'erreurs éventuelles
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Éditeur de Sticker",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Bouton Changer d'image
                    IconButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Changer d'image",
                            tint = Color.White
                        )
                    }

                    // Bouton Enregistrer le sticker WebP
                    if (state.hasImage) {
                        Button(
                            onClick = {
                                viewModel.saveSticker {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Sticker enregistré avec succès !")
                                    }
                                    onNavigateBack()
                                }
                            },
                            enabled = !state.isSaving && !state.isSegmenting,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Enregistrer")
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121212)
                )
            )
        },
        containerColor = Color(0xFF121212),
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF121212))
        ) {
            // Espace de travail central avec canvas tactile et damier de transparence
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                InteractiveCanvas(
                    bitmap = state.renderedBitmap,
                    scale = state.scale,
                    rotation = state.rotation,
                    offsetX = state.offsetX,
                    offsetY = state.offsetY,
                    isSegmenting = state.isSegmenting,
                    onTransformChanged = { pan, zoom, rotate ->
                        viewModel.onTransformGesture(pan, zoom, rotate)
                    },
                    onPickImageClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }

            // Barre d'outils inférieure pour les commandes IA et réglages de contour
            if (state.hasImage) {
                EditorToolbar(
                    isCutout = state.isCutout,
                    isSegmenting = state.isSegmenting,
                    borderSizePx = state.borderSizePx,
                    selectedBorderColor = state.borderColor,
                    canUndo = state.canUndo,
                    canRedo = state.canRedo,
                    onAiSegmentClick = { viewModel.performAiSegmentation() },
                    onBorderSizeChange = { newSize -> viewModel.setBorderSize(newSize, commitToHistory = false) },
                    onBorderSizeCommit = { finalSize -> viewModel.setBorderSize(finalSize, commitToHistory = true) },
                    onBorderColorChange = { newColor -> viewModel.setBorderColor(newColor) },
                    onUndoClick = { viewModel.undo() },
                    onRedoClick = { viewModel.redo() },
                    onResetTransformClick = { viewModel.resetTransform() }
                )
            }
        }
    }
}
