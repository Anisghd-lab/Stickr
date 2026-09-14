package com.stickr.app.data.repository

import android.graphics.Bitmap
import com.stickr.app.core.image.ImageSegmenterHelper
import com.stickr.app.domain.model.SegmentationResult
import com.stickr.app.domain.repository.ImageSegmentationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageSegmentationRepositoryImpl @Inject constructor(
    private val segmentationHelper: ImageSegmenterHelper
) : ImageSegmentationRepository {

    override suspend fun segmentImage(bitmap: Bitmap): SegmentationResult {
        return segmentationHelper.segmentSubject(bitmap).fold(
            onSuccess = { cutout ->
                SegmentationResult.Success(
                    originalBitmap = bitmap,
                    cutoutBitmap = cutout
                )
            },
            onFailure = { error ->
                SegmentationResult.Error(
                    message = error.localizedMessage ?: "Échec de la segmentation",
                    throwable = error
                )
            }
        )
    }
}
