package com.stickr.app.presentation.screens.editor

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Espace de travail central tactile interactif :
 * - Affiche un motif damier (checkerboard) subtil pour visualiser la transparence.
 * - Capture les gestes multitouch (zoom, rotation, déplacement) sans recompositions superflues
 *   en s'appuyant sur [graphicsLayer] pour garantir une fluidité constante à 60/120 FPS.
 *
 * @param bitmap Le Bitmap courant (sujet détouré + contour appliqué).
 * @param scale Facteur de zoom actuel.
 * @param rotation Rotation en degrés.
 * @param offsetX Translation horizontale en px.
 * @param offsetY Translation verticale en px.
 * @param isSegmenting True si une opération IA MediaPipe est en cours.
 * @param onTransformChanged Callback déclenché à chaque manipulation multitouch.
 * @param onPickImageClick Action de sélection d'une photo.
 */
@Composable
fun InteractiveCanvas(
    bitmap: Bitmap?,
    scale: Float,
    rotation: Float,
    offsetX: Float,
    offsetY: Float,
    isSegmenting: Boolean,
    onTransformChanged: (pan: Offset, zoom: Float, rotate: Float) -> Unit,
    onPickImageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val checkerLight = Color(0xFF282828)
    val checkerDark = Color(0xFF1C1C1C)

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .drawBehind {
                // Rendu ultra-rapide du damier de transparence
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
            .pointerInput(bitmap) {
                if (bitmap != null) {
                    detectTransformGestures { _, pan, zoom, rotate ->
                        onTransformChanged(pan, zoom, rotate)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            // Affichage GPU direct via RenderNode (graphicsLayer)
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Aperçu du sticker",
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        rotationZ = rotation
                        translationX = offsetX
                        translationY = offsetY
                    }
                    .fillMaxSize()
                    .padding(24.dp)
            )
        } else {
            // État initial : invitation à importer une photo
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
                    text = "Importe une photo pour créer ton sticker avec détourage IA",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFAAAAAA)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onPickImageClick) {
                    Text("Choisir une image")
                }
            }
        }

        // Calque semi-transparent d'analyse IA en cours
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
