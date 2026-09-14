package com.stickr.app.presentation.screens.editor.components

import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LineWeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stickr.app.core.image.StickerFlattener
import com.stickr.app.feature.editor.model.TextLayer

private val FONT_OPTIONS = listOf(
    "Impact",
    "Sans-Serif",
    "Serif",
    "Monospace",
    "Cursive"
)

private val TEXT_FILL_COLORS = listOf(
    Color(0xFFFFFFFF), // Blanc
    Color(0xFF111111), // Noir
    Color(0xFFFEE140), // Jaune vif
    Color(0xFFFF3366), // Rose néon
    Color(0xFFFF3B30), // Rouge
    Color(0xFF00F2FE), // Cyan néon
    Color(0xFF00F5A0), // Vert menthe
    Color(0xFF9B51E0)  // Violet
)

private val TEXT_STROKE_COLORS = listOf(
    Color(0xFF000000), // Noir
    Color(0xFFFFFFFF), // Blanc
    Color(0xFF333333), // Anthracite
    Color(0xFF888888), // Gris clair
    Color(0xFF8B0000), // Rouge sombre
    Color(0xFF003366)  // Bleu nuit
)

/**
 * Boîte de dialogue modale pour la création et la modification de calques de texte stylisé.
 *
 * Propose la saisie de texte, la sélection de police typographique (mème Impact, Sans-Serif, Serif, etc.),
 * des palettes de couleurs distinctes pour le remplissage et le contour, ainsi que des curseurs
 * pour ajuster la taille de police et l'épaisseur du trait de contour.
 */
@Composable
fun TextEditDialog(
    initialTextLayer: TextLayer? = null,
    onDismissRequest: () -> Unit,
    onConfirm: (
        text: String,
        textColor: Int,
        strokeColor: Int,
        strokeWidth: Float,
        fontSize: Float,
        fontFamilyName: String
    ) -> Unit
) {
    var text by remember { mutableStateOf(initialTextLayer?.text ?: "") }
    var selectedFont by remember { mutableStateOf(initialTextLayer?.fontFamilyName ?: "Impact") }
    var textColorArgb by remember { mutableIntStateOf(initialTextLayer?.textColor ?: android.graphics.Color.WHITE) }
    var strokeColorArgb by remember { mutableIntStateOf(initialTextLayer?.strokeColor ?: android.graphics.Color.BLACK) }
    var fontSize by remember { mutableFloatStateOf(initialTextLayer?.fontSize ?: 42f) }
    var strokeWidth by remember { mutableFloatStateOf(initialTextLayer?.strokeWidth ?: 6f) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color(0xFF1E1E24),
        title = {
            Text(
                text = if (initialTextLayer != null) "Modifier le texte" else "Ajouter du texte stylisé",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Saisie du texte
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Tape ton texte ici...", color = Color(0xFF888888)) },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFF444444)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Aperçu visuel en direct
                Text(
                    text = "Aperçu en direct",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFAAAAAA)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF121212)),
                    contentAlignment = Alignment.Center
                ) {
                    val previewText = text.ifBlank { "TEXTE APERÇU" }
                    Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                        val squarePx = 14.dp.toPx()
                        val cols = (size.width / squarePx).toInt() + 1
                        val rows = (size.height / squarePx).toInt() + 1
                        for (r in 0 until rows) {
                            for (c in 0 until cols) {
                                val colColor = if ((r + c) % 2 == 0) Color(0xFF222222) else Color(0xFF181818)
                                drawRect(
                                    color = colColor,
                                    topLeft = Offset(c * squarePx, r * squarePx),
                                    size = Size(squarePx, squarePx)
                                )
                            }
                        }

                        val typeface = StickerFlattener.resolveTypeface(selectedFont)
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            this.typeface = typeface
                            this.textSize = fontSize.coerceIn(20f, 48f)
                            this.textAlign = Paint.Align.CENTER
                        }

                        val bounds = Rect()
                        paint.getTextBounds(previewText, 0, previewText.length, bounds)
                        val cx = size.width / 2f
                        val cy = (size.height / 2f) + (bounds.height() / 2f)

                        if (strokeWidth > 0f) {
                            paint.style = Paint.Style.STROKE
                            paint.strokeWidth = strokeWidth * 1.5f
                            paint.strokeJoin = Paint.Join.ROUND
                            paint.strokeCap = Paint.Cap.ROUND
                            paint.color = strokeColorArgb
                            drawContext.canvas.nativeCanvas.drawText(previewText, cx, cy, paint)
                        }

                        paint.style = Paint.Style.FILL
                        paint.color = textColorArgb
                        drawContext.canvas.nativeCanvas.drawText(previewText, cx, cy, paint)
                    }
                }

                // Choix de police typographique
                Text(
                    text = "Police d'écriture",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFAAAAAA)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FONT_OPTIONS.forEach { fontName ->
                        val selected = selectedFont.equals(fontName, ignoreCase = true)
                        FilterChip(
                            selected = selected,
                            onClick = { selectedFont = fontName },
                            label = { Text(fontName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF2A2A32),
                                labelColor = Color(0xFFCCCCCC)
                            )
                        )
                    }
                }

                // Couleur du texte (Fill)
                Text(
                    text = "Couleur du texte",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFAAAAAA)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TEXT_FILL_COLORS.forEach { color ->
                        val colorArgb = color.toArgb()
                        val isSelected = colorArgb == textColorArgb
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
                                .clickable { textColorArgb = colorArgb }
                        )
                    }
                }

                // Couleur du contour (Stroke)
                Text(
                    text = "Couleur du contour",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFAAAAAA)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TEXT_STROKE_COLORS.forEach { color ->
                        val colorArgb = color.toArgb()
                        val isSelected = colorArgb == strokeColorArgb
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
                                .clickable { strokeColorArgb = colorArgb }
                        )
                    }
                }

                // Slider Taille du texte
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = null,
                            tint = Color(0xFFAAAAAA),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Taille : ${fontSize.toInt()} sp",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFAAAAAA)
                        )
                    }
                }
                Slider(
                    value = fontSize,
                    onValueChange = { fontSize = it },
                    valueRange = 20f..72f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color(0xFF333333)
                    )
                )

                // Slider Épaisseur contour
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LineWeight,
                            contentDescription = null,
                            tint = Color(0xFFAAAAAA),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Contour : ${strokeWidth.toInt()} px",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFAAAAAA)
                        )
                    }
                }
                Slider(
                    value = strokeWidth,
                    onValueChange = { strokeWidth = it },
                    valueRange = 0f..16f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color(0xFF333333)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onConfirm(text.trim(), textColorArgb, strokeColorArgb, strokeWidth, fontSize, selectedFont)
                    }
                },
                enabled = text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (initialTextLayer != null) "Modifier" else "Ajouter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Annuler", color = Color(0xFFAAAAAA))
            }
        }
    )
}
