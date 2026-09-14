package com.stickr.app.core.image

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.FileNotFoundException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ImageSegmenterHelperTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `default parameters are correctly configured`() {
        val helper = ImageSegmenterHelper(context)

        assertEquals("selfie_segmenter.tflite", helper.modelPath)
        assertEquals(0.5f, helper.confidenceThreshold, 0.001f)
    }

    @Test
    fun `getOrCreateSegmenter throws FileNotFoundException when model does not exist`() {
        val helper = ImageSegmenterHelper(context, modelPath = "non_existent_model.tflite")

        val result = runCatching { helper.getOrCreateSegmenter() }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is FileNotFoundException)
    }

    @Test
    fun `segmentSubject fails gracefully with recycled bitmap`() = runBlocking {
        val helper = ImageSegmenterHelper(context)
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        bitmap.recycle()

        val result = helper.segmentSubject(bitmap)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }
}
