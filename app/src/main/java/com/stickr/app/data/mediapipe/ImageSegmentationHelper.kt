package com.stickr.app.data.mediapipe

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter.ImageSegmenterOptions
import com.stickr.app.domain.model.SegmentationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

class ImageSegmentationHelper(
    private val context: Context,
    private val modelName: String = "selfie_segmenter.tflite"
) {
    private var imageSegmenter: ImageSegmenter? = null

    init {
        setupImageSegmenter()
    }

    private fun setupImageSegmenter() {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(modelName)
                .build()

            val options = ImageSegmenterOptions.builder()
                .setBaseOptions(baseOptions)
                .setOutputType(ImageSegmenterOptions.OutputType.CONFIDENCE_MASK)
                .setRunningMode(RunningMode.IMAGE)
                .build()

            imageSegmenter = ImageSegmenter.createFromOptions(context, options)
        } catch (e: Exception) {
            // Model file might not yet be in assets during initial setup
            imageSegmenter = null
        }
    }

    suspend fun segment(bitmap: Bitmap): SegmentationResult = withContext(Dispatchers.Default) {
        try {
            val segmenter = imageSegmenter
            if (segmenter == null) {
                // Fallback / placeholder if model is not yet bundled in assets
                return@withContext SegmentationResult.Success(
                    originalBitmap = bitmap,
                    cutoutBitmap = bitmap
                )
            }

            val mpImage = BitmapImageBuilder(bitmap).build()
            val result = segmenter.segment(mpImage)

            val confidenceMasks = result.confidenceMasks()
            if (confidenceMasks.isPresent && confidenceMasks.get().isNotEmpty()) {
                val mask = confidenceMasks.get()[0]
                val byteBuffer: ByteBuffer = ByteBufferExtractor.extract(mask)
                val cutoutBitmap = applyConfidenceMask(bitmap, byteBuffer)
                SegmentationResult.Success(
                    originalBitmap = bitmap,
                    cutoutBitmap = cutoutBitmap
                )
            } else {
                SegmentationResult.Success(
                    originalBitmap = bitmap,
                    cutoutBitmap = bitmap
                )
            }
        } catch (e: Exception) {
            SegmentationResult.Error(e.localizedMessage ?: "Image segmentation failed", e)
        }
    }

    private fun applyConfidenceMask(bitmap: Bitmap, byteBuffer: ByteBuffer): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        byteBuffer.rewind()
        for (i in pixels.indices) {
            val confidence = if (byteBuffer.hasRemaining()) byteBuffer.float else 1f
            // Threshold for foreground object
            if (confidence < 0.5f) {
                pixels[i] = Color.TRANSPARENT
            }
        }

        outputBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return outputBitmap
    }
}
