package com.stickr.app.domain.repository

import android.graphics.Bitmap
import com.stickr.app.domain.model.SegmentationResult

interface ImageSegmentationRepository {
    suspend fun segmentImage(bitmap: Bitmap): SegmentationResult
}
