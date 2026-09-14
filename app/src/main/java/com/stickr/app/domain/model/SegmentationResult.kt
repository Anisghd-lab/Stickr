package com.stickr.app.domain.model

import android.graphics.Bitmap

sealed class SegmentationResult {
    data class Success(
        val originalBitmap: Bitmap,
        val cutoutBitmap: Bitmap,
        val confidenceMask: Bitmap? = null
    ) : SegmentationResult()

    data class Error(val message: String, val throwable: Throwable? = null) : SegmentationResult()
}
