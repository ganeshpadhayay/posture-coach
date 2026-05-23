package com.posturecoach.domain.usecase

import android.graphics.BitmapFactory
import com.posturecoach.data.repository.ScanRepository
import com.posturecoach.domain.model.PostureScan
import com.posturecoach.domain.pose.PoseAnalyzer
import com.posturecoach.domain.pose.PostureRules
import java.util.UUID
import javax.inject.Inject

class AnalyzePostureUseCase @Inject constructor(
    private val poseAnalyzer: PoseAnalyzer,
    private val scanRepository: ScanRepository,
) {

    sealed interface Result {
        data class Success(val scan: PostureScan) : Result
        data object NoPoseDetected : Result
        data class Error(val cause: Throwable) : Result
    }

    suspend operator fun invoke(imagePath: String): Result = try {
        val analyzed = poseAnalyzer.analyzeFile(imagePath)
        if (analyzed == null || analyzed.landmarks.isEmpty()) {
            val dims = decodeDimensions(imagePath)
            if (dims == null) Result.NoPoseDetected else Result.NoPoseDetected
        } else {
            val analysis = PostureRules.analyze(analyzed.landmarks)
            val scan = PostureScan(
                id = UUID.randomUUID().toString(),
                timestampMs = System.currentTimeMillis(),
                imagePath = imagePath,
                imageWidth = analyzed.imageWidth,
                imageHeight = analyzed.imageHeight,
                issues = analysis.issues,
                angles = analysis.angles,
                landmarks = analyzed.landmarks,
            )
            scanRepository.save(scan)
            Result.Success(scan)
        }
    } catch (t: Throwable) {
        Result.Error(t)
    }

    private fun decodeDimensions(path: String): Pair<Int, Int>? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, opts)
        if (opts.outWidth <= 0 || opts.outHeight <= 0) return null
        return opts.outWidth to opts.outHeight
    }
}
