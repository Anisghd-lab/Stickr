package com.stickr.app.core.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter.ImageSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.FileNotFoundException
import java.nio.ByteOrder

/**
 * Moteur de segmentation IA locale utilisant Google MediaPipe Tasks Vision.
 *
 * Exécute l'inférence localement sur le CPU/GPU de l'appareil (on-device)
 * sans aucun appel réseau.
 *
 * ⚠️ IMPORTANT :
 * Placer le modèle .tflite dans le dossier `app/src/main/assets/`.
 * Modèles officiels recommandés :
 * - `selfie_segmenter.tflite` (défaut, ~249 Ko) :
 *   https://storage.googleapis.com/mediapipe-models/image_segmenter/selfie_segmenter/float16/latest/selfie_segmenter.tflite
 * - `deeplab_v3.tflite` (~2.7 Mo) :
 *   https://storage.googleapis.com/mediapipe-models/image_segmenter/deeplab_v3/float32/latest/deeplab_v3.tflite
 *
 * @param context Contexte Android pour accéder aux assets.
 * @param modelPath Nom du fichier modèle dans `assets/` (défaut : [DEFAULT_MODEL_NAME]).
 * @param confidenceThreshold Seuil de confiance minimal (0.0f à 1.0f) pour le premier plan.
 */
class ImageSegmenterHelper(
    private val context: Context,
    val modelPath: String = DEFAULT_MODEL_NAME,
    val confidenceThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD
) : Closeable {

    @Volatile
    private var segmenter: ImageSegmenter? = null
    private val initLock = Any()

    /**
     * Initialise ou retourne l'instance existante d'ImageSegmenter.
     * Lève une [FileNotFoundException] si le modèle n'est pas présent dans `assets/`.
     */
    @Throws(Exception::class)
    fun getOrCreateSegmenter(): ImageSegmenter {
        segmenter?.let { return it }
        synchronized(initLock) {
            segmenter?.let { return it }

            // Vérification de la présence du modèle dans les assets
            val assetExists = runCatching {
                context.assets.open(modelPath).use { }
                true
            }.getOrDefault(false)

            if (!assetExists) {
                throw FileNotFoundException(
                    "Modèle MediaPipe introuvable : '$modelPath'. " +
                    "Veuillez placer '$modelPath' dans le dossier 'app/src/main/assets/'."
                )
            }

            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(modelPath)
                .build()

            val options = ImageSegmenterOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE)
                .setOutputConfidenceMasks(true)
                .setOutputCategoryMask(false)
                .build()

            val instance = ImageSegmenter.createFromOptions(context, options)
            segmenter = instance
            return instance
        }
    }

    /**
     * Segmente le sujet principal de [inputBitmap] et extrait un Bitmap transparent ARGB_8888.
     *
     * S'exécute de manière asynchrone sur [Dispatchers.Default] pour éviter tout blocage du thread UI.
     *
     * @param inputBitmap L'image source à détourer.
     * @return [Result] contenant le Bitmap détouré sur fond transparent en cas de succès.
     */
    suspend fun segmentSubject(inputBitmap: Bitmap): Result<Bitmap> = withContext(Dispatchers.Default) {
        runCatching {
            require(!inputBitmap.isRecycled) { "inputBitmap est déjà recyclé" }
            require(inputBitmap.width > 0 && inputBitmap.height > 0) { "Dimensions de Bitmap invalides" }

            // Conversion défensive si le bitmap est en configuration HARDWARE ou non-ARGB_8888
            val safeBitmap = if (inputBitmap.config != Bitmap.Config.ARGB_8888) {
                inputBitmap.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                inputBitmap
            }

            val mpImage = BitmapImageBuilder(safeBitmap).build()
            val activeSegmenter = getOrCreateSegmenter()
            val segmentationResult = activeSegmenter.segment(mpImage)

            val confidenceMasks = segmentationResult.confidenceMasks()
            if (!confidenceMasks.isPresent || confidenceMasks.get().isEmpty()) {
                if (safeBitmap != inputBitmap) safeBitmap.recycle()
                throw IllegalStateException("MediaPipe n'a renvoyé aucun masque de segmentation.")
            }

            val masks = confidenceMasks.get()
            // Pour selfie_segmenter : masque 0 = arrière-plan, masque 1 = personne/sujet.
            // Si un seul masque est produit, on utilise l'index 0.
            val foregroundMask: MPImage = if (masks.size > 1) masks[1] else masks[0]
            val maskByteBuffer = ByteBufferExtractor.extract(foregroundMask)
            maskByteBuffer.rewind()
            maskByteBuffer.order(ByteOrder.nativeOrder())

            val width = safeBitmap.width
            val height = safeBitmap.height
            val maskWidth = foregroundMask.width
            val maskHeight = foregroundMask.height

            val sourcePixels = IntArray(width * height)
            safeBitmap.getPixels(sourcePixels, 0, width, 0, 0, width, height)

            val outputPixels = IntArray(width * height)

            if (maskWidth == width && maskHeight == height) {
                // Correspondance directe 1:1 pixel par pixel
                for (i in sourcePixels.indices) {
                    val confidence = if (maskByteBuffer.hasRemaining()) maskByteBuffer.float else 0f
                    if (confidence >= confidenceThreshold) {
                        outputPixels[i] = sourcePixels[i]
                    } else {
                        outputPixels[i] = Color.TRANSPARENT
                    }
                }
            } else {
                // Mise à l'échelle si la résolution du masque diffère du bitmap original
                val scaleX = maskWidth.toFloat() / width
                val scaleY = maskHeight.toFloat() / height
                val maskFloats = FloatArray(maskWidth * maskHeight)
                maskByteBuffer.asFloatBuffer().get(maskFloats)

                for (y in 0 until height) {
                    val maskY = (y * scaleY).toInt().coerceIn(0, maskHeight - 1)
                    val rowOffset = y * width
                    val maskRowOffset = maskY * maskWidth

                    for (x in 0 until width) {
                        val maskX = (x * scaleX).toInt().coerceIn(0, maskWidth - 1)
                        val confidence = maskFloats[maskRowOffset + maskX]
                        val idx = rowOffset + x

                        if (confidence >= confidenceThreshold) {
                            outputPixels[idx] = sourcePixels[idx]
                        } else {
                            outputPixels[idx] = Color.TRANSPARENT
                        }
                    }
                }
            }

            val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            outputBitmap.setPixels(outputPixels, 0, width, 0, 0, width, height)

            if (safeBitmap != inputBitmap) {
                safeBitmap.recycle()
            }

            outputBitmap
        }
    }

    override fun close() {
        synchronized(initLock) {
            segmenter?.close()
            segmenter = null
        }
    }

    companion object {
        const val DEFAULT_MODEL_NAME = "selfie_segmenter.tflite"
        const val DEEPLAB_V3_MODEL_NAME = "deeplab_v3.tflite"
        const val DEFAULT_CONFIDENCE_THRESHOLD = 0.5f
    }
}

/**
 * Fonction d'extension pour détourer facilement un Bitmap avec [ImageSegmenterHelper].
 */
suspend fun Bitmap.segmentSubject(helper: ImageSegmenterHelper): Result<Bitmap> {
    return helper.segmentSubject(this)
}
