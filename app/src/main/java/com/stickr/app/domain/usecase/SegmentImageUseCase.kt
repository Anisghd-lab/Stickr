package com.stickr.app.domain.usecase

import android.graphics.Bitmap
import com.stickr.app.domain.model.SegmentationResult
import com.stickr.app.domain.repository.ImageSegmentationRepository
import javax.inject.Inject

class SegmentImageUseCase @Inject constructor(
    private val segmentationRepository: ImageSegmentationRepository
) {
    suspend operator fun invoke(bitmap: Bitmap): SegmentationResult {
        return segmentationRepository.segmentImage(bitmap)
    }
}
