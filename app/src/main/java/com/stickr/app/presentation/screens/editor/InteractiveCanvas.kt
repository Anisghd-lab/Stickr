package com.stickr.app.presentation.screens.editor

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stickr.app.core.image.StickerFlattener
import com.stickr.app.feature.editor.model.DecorationLayer
import com.stickr.app.feature.editor.model.EditorLayer
import com.stickr.app.feature.editor.model.SubjectLayer
import com.stickr.app.feature.editor.model.TextLayer

/**
 * Espace de travail central tactile interactif multi-calques :
 * - Damier de transparence en arrière-plan.
 * - Rendu ordonné des calques (Sujet principal -> Accessoires -> Textes stylisés).
 * - Cadre de sélection interactif (bounding box) avec poignée de suppression rapide (X).
 * - Barre supérieure de sélection rapide de calques (Chips) pour switcher facilement.
 * - Capture multitouch à 60/120 FPS via [graphicsLayer].
 */
@Composable
fun InteractiveCanvas(
    bitmap: Bitmap?,
    layers: List<EditorLayer> = emptyList(),
    selectedLayerId: String? = null,
    scale: Float = 1.0f,
    rotation: Float = 0.0f,
    offsetX: Float = 0.0f,
    offsetY: Float = 0.0f,
    isSegmenting: Boolean = false,
    onTransformChanged: (pan: Offset, zoom: Float, rotate: Float) -> Unit,
    onSelectLayer: (String?) -> Unit = {},
    onDeleteLayer: (String) -> Unit = {},
    onEditTextLayer: (TextLayer) -> Unit = {},
    onPickImageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val checkerLight = Color(0xFF282828)
    val checkerDark = Color(0xFF1C1C1C)

    val subjectLayer = layers.filterIsInstance<SubjectLayer>().firstOrNull()
    val decorationLayers = layers.filterIsInstance<DecorationLayer>()
    val textLayers = layers.filterIsInstance<TextLayer>()

    val hasContent = bitmap != null || layers.isNotEmpty()

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .drawBehind {
                val squarePx = 18.dp.toPx()
                val cols = (size.width / squarePx).toInt() + 1
                val rows = (size.height / squarePx).toInt() + 1

                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val color = if ((r + c) % 2 == 0) checkerLight else checkerDark
                        drawRect(
                            color = color,
                            topLeft = Offset(c * squarePx, r * squarePx),
                            size = Size(squarePx, squarePx)
                        )
                    }
                }
            }
            .pointerInput(hasContent) {
                if (hasContent) {
                    detectTransformGestures { _, pan, zoom, rotate ->
                        onTransformChanged(pan, zoom, rotate)
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures {
                    // Clic dans le vide -> désélectionne les calques secondaires
                    onSelectLayer(null)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (hasContent) {
            // 1. Calque Sujet Principal
            val activeBitmap = subjectLayer?.bitmap ?: bitmap
            if (activeBitmap != null) {
                val isSubjectSelected = selectedLayerId == null || selectedLayerId == (subjectLayer?.id ?: "subject_layer")
                val curScale = subjectLayer?.scale ?: scale
                val curRot = subjectLayer?.rotation ?: rotation
                val curOffX = subjectLayer?.offset?.x ?: offsetX
                val curOffY = subjectLayer?.offset?.y ?: offsetY

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .graphicsLayer {
                            scaleX = curScale
                            scaleY = curScale
                            rotationZ = curRot
                            translationX = curOffX
                            translationY = curOffY
                        }
                        .clickable { onSelectLayer(subjectLayer?.id ?: "subject_layer") },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = activeBitmap.asImageBitmap(),
                        contentDescription = "Sujet du sticker",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // 2. Calques d'Accessoires & Décorations
            decorationLayers.forEach { deco ->
                val isSelected = selectedLayerId == deco.id
                DecorationLayerItem(
                    layer = deco,
                    isSelected = isSelected,
                    onClick = { onSelectLayer(deco.id) },
                    onDelete = { onDeleteLayer(deco.id) }
                )
            }

            // 3. Calques de Texte Stylisé
            textLayers.forEach { textLayer ->
                val isSelected = selectedLayerId == textLayer.id
                TextLayerItem(
                    layer = textLayer,
                    isSelected = isSelected,
                    onClick = { onSelectLayer(textLayer.id) },
                    onDelete = { onDeleteLayer(textLayer.id) },
                    onEdit = { onEditTextLayer(textLayer) }
                )
            }

            // 4. Barre supérieure de sélection rapide des calques (si plusieurs calques)
            if (layers.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    layers.forEach { layer ->
                        val isSelected = layer.id == selectedLayerId || (selectedLayerId == null && layer is SubjectLayer)
                        val label = when (layer) {
                            is SubjectLayer -> "Sujet"
                            is TextLayer -> "T: \"${layer.text.take(8)}\""
                            is DecorationLayer -> "Déco: ${layer.assetPath}"
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectLayer(layer.id) },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xAA1E1E24),
                                labelColor = Color(0xFFDDDDDD)
                            )
                        )
                    }
                }
            }
        } else {
            // État vide : invitation à importer une photo
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Aucune image sélectionnée",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Importe une photo pour créer ton sticker avec calques et détourage IA",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFAAAAAA),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onPickImageClick) {
                    Text("Choisir une image")
                }
            }
        }

        // Overlay de détourage IA
        if (isSegmenting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Détourage IA MediaPipe...",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Extraction instantanée du sujet sans serveur",
                        color = Color(0xFFBBBBBB),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun DecorationLayerItem(
    layer: DecorationLayer,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .graphicsLayer {
                translationX = layer.offset.x
                translationY = layer.offset.y
                scaleX = layer.scale
                scaleY = layer.scale
                rotationZ = layer.rotation
            }
            .size(72.dp)
            .then(
                if (isSelected) {
                    Modifier.border(1.5.dp, Color(0xFF00F2FE), RoundedCornerShape(10.dp))
                } else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = layer.assetPath,
            fontSize = 42.sp,
            textAlign = TextAlign.Center
        )

        if (isSelected) {
            // Poignée de suppression 'X'
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-6).dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935))
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Supprimer l'accessoire",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun TextLayerItem(
    layer: TextLayer,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val typeface = remember(layer.fontFamilyName) {
        StickerFlattener.resolveTypeface(layer.fontFamilyName)
    }

    val paint = remember(layer, typeface) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            this.textSize = layer.fontSize
            this.textAlign = Paint.Align.CENTER
        }
    }

    val bounds = remember(layer.text, paint) {
        val b = Rect()
        paint.getTextBounds(layer.text, 0, layer.text.length, b)
        b
    }

    val boxWidth = with(density) { (bounds.width() + 32).toDp().coerceAtLeast(60.dp) }
    val boxHeight = with(density) { (bounds.height() + 24).toDp().coerceAtLeast(44.dp) }

    Box(
        modifier = modifier
            .graphicsLayer {
                translationX = layer.offset.x
                translationY = layer.offset.y
                scaleX = layer.scale
                scaleY = layer.scale
                rotationZ = layer.rotation
            }
            .size(width = boxWidth, height = boxHeight)
            .then(
                if (isSelected) {
                    Modifier.border(1.5.dp, Color(0xFF00F2FE), RoundedCornerShape(8.dp))
                } else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawContext.canvas.nativeCanvas.let { nativeCanvas ->
                val cx = size.width / 2f
                val cy = (size.height / 2f) + (bounds.height() / 2f)

                // 1. Contour
                if (layer.strokeWidth > 0f) {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = layer.strokeWidth * 2f
                    paint.strokeJoin = Paint.Join.ROUND
                    paint.strokeCap = Paint.Cap.ROUND
                    paint.color = layer.strokeColor
                    nativeCanvas.drawText(layer.text, cx, cy, paint)
                }

                // 2. Remplissage
                paint.style = Paint.Style.FILL
                paint.color = layer.textColor
                nativeCanvas.drawText(layer.text, cx, cy, paint)
            }
        }

        if (isSelected) {
            // Bouton Modifier
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-6).dp, y = (-6).dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onEdit),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Modifier le texte",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }

            // Poignée de suppression 'X'
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-6).dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935))
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Supprimer le texte",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
