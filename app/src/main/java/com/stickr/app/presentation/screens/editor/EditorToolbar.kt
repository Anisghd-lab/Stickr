package com.stickr.app.presentation.screens.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

/**
 * Palette de couleurs prédéfinies pour le contour de sticker (die-cut border).
 */
val BORDER_COLORS = listOf(
    Color(0xFFFFFFFF), // Blanc classique vinyle
    Color(0xFF111111), // Noir intense
    Color(0xFFFEE140), // Jaune vif
    Color(0xFF00F2FE), // Cyan néon
    Color(0xFF00F5A0), // Vert lime
    Color(0xFFFF0844), // Rouge / Rose néon
    Color(0xFF9B51E0)  // Violet / Mauve
)

/**
 * Barre d'outils d'édition inférieure (EditorToolbar) :
 * - Déclencheur du détourage IA MediaPipe avec indicateur actif.
 * - Boutons d'ajout de calques (Texte stylisé et Accessoires).
 * - Slider d'épaisseur de contour (0 à 32 px).
 * - Palette de sélection rapide des couleurs de contour.
 * - Contrôles Annuler (Undo), Rétablir (Redo) et Réinitialiser zoom.
 */
@Composable
fun EditorToolbar(
    isCutout: Boolean,
    isSegmenting: Boolean,
    borderSizePx: Float,
    selectedBorderColor: Int,
    canUndo: Boolean,
    canRedo: Boolean,
    onAiSegmentClick: () -> Unit,
    onAddTextClick: () -> Unit = {},
    onAddDecorationClick: () -> Unit = {},
    onBorderSizeChange: (Float) -> Unit,
    onBorderSizeCommit: (Float) -> Unit,
    onBorderColorChange: (Int) -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onResetTransformClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Surface(
        color = Color(0xFF18181B),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        tonalElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Ligne supérieure : Bouton Détourage IA + Outils Calques (Texte, Accessoires) + Undo / Redo / Reset
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onAiSegmentClick()
                    },
                    enabled = !isSegmenting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSegmenting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSegmenting) "Détourage..." else if (isCutout) "Re-détourer" else "Détourer IA",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Bouton Ajouter Texte
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            onAddTextClick()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TextFields,
                            contentDescription = "Ajouter texte",
                            tint = Color.White
                        )
                    }

                    // Bouton Ajouter Accessoire
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            onAddDecorationClick()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Celebration,
                            contentDescription = "Accessoires",
                            tint = Color.White
                        )
                    }

                    // Bouton Annuler
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            onUndoClick()
                        },
                        enabled = canUndo,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Annuler",
                            tint = if (canUndo) Color.White else Color(0xFF555555)
                        )
                    }

                    // Bouton Rétablir
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            onRedoClick()
                        },
                        enabled = canRedo,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Rétablir",
                            tint = if (canRedo) Color.White else Color(0xFF555555)
                        )
                    }

                    // Bouton Réinitialiser Cadrage
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            onResetTransformClick()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Réinitialiser cadrage",
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Ligne intermédiaire : Curseur Slider de l'épaisseur de bordure
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Contour sticker",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Text(
                    text = "${borderSizePx.toInt()} px",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Slider(
                value = borderSizePx,
                onValueChange = onBorderSizeChange,
                onValueChangeFinished = { onBorderSizeCommit(borderSizePx) },
                valueRange = 0f..32f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color(0xFF333333)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Ligne inférieure : Palette de couleurs de contour
            AnimatedVisibility(visible = borderSizePx > 0f) {
                Column {
                    Text(
                        text = "Couleur du contour",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFAAAAAA),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (color in BORDER_COLORS) {
                            val colorArgb = color.toArgb()
                            val isSelected = colorArgb == selectedBorderColor

                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF444444),
                                        shape = CircleShape
                                    )
                                    .clickable { onBorderColorChange(colorArgb) }
                            )
                        }
                    }
                }
            }
        }
    }
}
