package com.stickr.app.feature.packdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.stickr.app.core.database.entity.StickerItemEntity
import com.stickr.app.core.model.WhatsAppStickerValidator
import kotlinx.coroutines.launch
import java.io.File

/**
 * Écran Détail du Pack :
 * - Gestion complète des stickers du pack (ajout, suppression, émojis).
 * - En-tête avec métadonnées éditables et miniature de plateau.
 * - Bouton proéminent d'exportation officielle vers WhatsApp.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackDetailScreen(
    viewModel: PackDetailViewModel,
    packId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showEditMetadataDialog by remember { mutableStateOf(false) }
    var selectedStickerForEmoji by remember { mutableStateOf<StickerItemEntity?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.packWithStickers?.pack?.name ?: "Détails du pack",
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
                    if (state.packWithStickers != null) {
                        IconButton(onClick = { showEditMetadataDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Modifier le pack",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121212)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToEditor(packId) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter un sticker")
            }
        },
        bottomBar = {
            // Bouton principal proéminent en bas d'écran pour l'export WhatsApp
            if (state.packWithStickers != null) {
                Surface(
                    color = Color(0xFF18181E),
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        val isReady = state.isWhatsAppReady
                        val count = state.stickerCount

                        if (!isReady) {
                            Text(
                                text = "WhatsApp requiert au moins 3 stickers (actuel : $count)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFFA000),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.exportToWhatsApp(context) { msg ->
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                }
                            },
                            enabled = isReady && !state.isExporting,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF25D366),
                                disabledContainerColor = Color(0xFF25D366).copy(alpha = 0.35f),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            if (state.isExporting) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Icon(Icons.Default.Send, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isReady) "Exporter vers WhatsApp" else "Ajoutez encore ${state.missingStickersCount} sticker(s)",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF121212),
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF121212))
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.packWithStickers == null -> {
                    Text(
                        text = "Pack introuvable",
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    val pack = state.packWithStickers!!.pack
                    val stickers = state.packWithStickers!!.stickers

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        // En-tête des métadonnées du pack
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Tray Icon / Thumbnail
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF2A2A36)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val trayPath = pack.trayImagePath
                                    if (!trayPath.isNullOrBlank() && File(trayPath).exists()) {
                                        AsyncImage(
                                            model = File(trayPath),
                                            contentDescription = "Tray Icon",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize().padding(4.dp)
                                        )
                                    } else if (stickers.isNotEmpty() && File(stickers[0].imagePath).exists()) {
                                        AsyncImage(
                                            model = File(stickers[0].imagePath),
                                            contentDescription = "Premier Sticker",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize().padding(4.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Mood,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pack.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Créé par ${pack.publisher}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFAAAAAA)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${stickers.size}/30 stickers",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Grille des stickers
                        if (stickers.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Aucun sticker dans ce pack",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Clique sur le bouton + pour créer ton premier sticker !",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFAAAAAA)
                                    )
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                contentPadding = PaddingValues(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(stickers, key = { it.id }) { sticker ->
                                    StickerItemCard(
                                        sticker = sticker,
                                        onDelete = { viewModel.deleteSticker(sticker.id) },
                                        onEditEmoji = { selectedStickerForEmoji = sticker }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog Modification Métadonnées du pack
    if (showEditMetadataDialog && state.packWithStickers != null) {
        val currentPack = state.packWithStickers!!.pack
        var editName by remember { mutableStateOf(currentPack.name) }
        var editPublisher by remember { mutableStateOf(currentPack.publisher) }

        AlertDialog(
            onDismissRequest = { showEditMetadataDialog = false },
            title = { Text("Modifier le Pack", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Nom du pack") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPublisher,
                        onValueChange = { editPublisher = it },
                        label = { Text("Auteur / Créateur") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            containerColor = Color(0xFF1E1E24),
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updatePackMetadata(editName, editPublisher)
                        showEditMetadataDialog = false
                    },
                    enabled = editName.isNotBlank() && editPublisher.isNotBlank()
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditMetadataDialog = false }) {
                    Text("Annuler", color = Color(0xFFAAAAAA))
                }
            }
        )
    }

    // Dialog Modification Émojis du sticker
    selectedStickerForEmoji?.let { sticker ->
        var editEmojis by remember { mutableStateOf(sticker.emojis) }

        AlertDialog(
            onDismissRequest = { selectedStickerForEmoji = null },
            title = { Text("Émojis du Sticker", color = Color.White) },
            text = {
                Column {
                    Text(
                        text = "Associe 1 à 3 émojis (utilisés par WhatsApp pour la recherche) :",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFAAAAAA)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editEmojis,
                        onValueChange = { editEmojis = it },
                        placeholder = { Text("ex: 🔥,😎,✨") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            containerColor = Color(0xFF1E1E24),
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateStickerEmojis(sticker.id, editEmojis)
                        selectedStickerForEmoji = null
                    }
                ) {
                    Text("Valider")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedStickerForEmoji = null }) {
                    Text("Annuler", color = Color(0xFFAAAAAA))
                }
            }
        )
    }
}

@Composable
fun StickerItemCard(
    sticker: StickerItemEntity,
    onDelete: () -> Unit,
    onEditEmoji: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = File(sticker.imagePath),
                contentDescription = "Sticker",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            )

            // Badge émojis en bas à gauche
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(topEnd = 8.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .clickable(onClick = onEditEmoji)
            ) {
                Text(
                    text = sticker.emojis,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Bouton supprimer en haut à droite
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(28.dp)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Supprimer",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
