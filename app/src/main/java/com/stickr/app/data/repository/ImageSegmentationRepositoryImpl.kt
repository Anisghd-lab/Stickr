package com.stickr.app.data.repository

import android.graphics.Bitmap
import com.stickr.app.data.mediapipe.ImageSegmentationHelper
import com.stickr.app.domain.model.SegmentationResult
import com.stickr.app.domain.repository.ImageSegmentationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageSegmentationRepositoryImpl @Inject constructor(
    private val segmentationHelper: ImageSegmentationHelper
) : ImageSegmentationRepository {

    override suspend fun segmentImage(bitmap: Bitmap): SegmentationResult {
        return segmentationHelper.segment(bitmap)
    }
}
