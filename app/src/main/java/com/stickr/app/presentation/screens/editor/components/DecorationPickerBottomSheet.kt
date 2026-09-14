package com.stickr.app.presentation.screens.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class DecorationCategory(
    val title: String,
    val items: List<String>
)

private val DECORATION_CATEGORIES = listOf(
    DecorationCategory(
        title = "Accessoires",
        items = listOf("😎", "🕶️", "👑", "🎩", "🧢", "🎀", "🥸", "🎧", "💍", "💄")
    ),
    DecorationCategory(
        title = "Mèmes & Réactions",
        items = listOf("🔥", "💯", "💥", "⭐", "✨", "🚀", "💣", "💎", "🎉", "🥳")
    ),
    DecorationCategory(
        title = "Émotions & Bulles",
        items = listOf("💬", "💭", "❤️", "💔", "💀", "🤡", "👻", "🤖", "👽", "😈")
    ),
    DecorationCategory(
        title = "Food & Objets",
        items = listOf("🍕", "🍔", "🌮", "🍩", "🍦", "☕", "🍺", "🥑", "🏆", "🎮")
    )
)

/**
 * BottomSheet modale proposant une grille d'accessoires et décorations prêtes à l'emploi
 * (lunettes de soleil, flammes, couronnes, bulles, emojis populaires).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecorationPickerBottomSheet(
    sheetState: SheetState = rememberModalBottomSheetState(),
    onDismissRequest: () -> Unit,
    onDecorationSelected: (assetOrEmoji: String) -> Unit
) {
    var selectedCategoryIndex by remember { mutableStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF1E1E24),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF555555))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Ajouter un accessoire",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Catégories horizontales
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DECORATION_CATEGORIES.forEachIndexed { index, category ->
                    val isSelected = index == selectedCategoryIndex
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategoryIndex = index },
                        label = { Text(category.title) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF2A2A32),
                            labelColor = Color(0xFFCCCCCC)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Grille des accessoires
            val currentItems = DECORATION_CATEGORIES[selectedCategoryIndex].items

            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(bottom = 24.dp)
            ) {
                items(currentItems) { emoji ->
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF26262E))
                            .clickable {
                                onDecorationSelected(emoji)
                                onDismissRequest()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 32.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
