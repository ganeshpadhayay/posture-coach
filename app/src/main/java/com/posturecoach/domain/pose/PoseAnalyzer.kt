package com.posturecoach.domain.pose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.posturecoach.domain.model.PoseLandmark
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AnalyzedPose(
    val landmarks: List<PoseLandmark>,
    val imageWidth: Int,
    val imageHeight: Int,
)

interface PoseAnalyzer {
    suspend fun analyzeFile(path: String): AnalyzedPose?
    fun close()
}

@Singleton
class PoseAnalyzerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : PoseAnalyzer {

    @Volatile
    private var landmarker: PoseLandmarker? = null

    private fun ensure(): PoseLandmarker {
        landmarker?.let { return it }
        synchronized(this) {
            landmarker?.let { return it }
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(MODEL_ASSET)
                .build()
            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE)
                .setNumPoses(1)
                .setMinPoseDetectionConfidence(0.5f)
                .setMinPosePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .build()
            val created = PoseLandmarker.createFromOptions(context, options)
            landmarker = created
            return created
        }
    }

    override suspend fun analyzeFile(path: String): AnalyzedPose? = withContext(Dispatchers.Default) {
        val bitmap = decodeBitmapOriented(path) ?: return@withContext null
        try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            val result = ensure().detect(mpImage)
            mapResult(result, bitmap.width, bitmap.height)
        } catch (t: Throwable) {
            Log.w(TAG, "Pose detect failed: ${t.message}")
            null
        } finally {
            bitmap.recycle()
        }
    }

    private fun mapResult(result: PoseLandmarkerResult, width: Int, height: Int): AnalyzedPose? {
        val first = result.landmarks().firstOrNull() ?: return null
        val mapped = first.mapIndexed { index, lm ->
            val vis: Float = lm.visibility().orElse(1f) ?: 1f
            PoseLandmark(
                name = PoseIndex.NAMES[index] ?: "lm_$index",
                x = lm.x(),
                y = lm.y(),
                visibility = vis,
            )
        }
        return AnalyzedPose(landmarks = mapped, imageWidth = width, imageHeight = height)
    }

    private fun decodeBitmapOriented(path: String): Bitmap? {
        val opts = BitmapFactory.Options().apply {
            inJustDecodeBounds = false
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return runCatching { BitmapFactory.decodeFile(path, opts) }.getOrNull()
    }

    override fun close() {
        synchronized(this) {
            landmarker?.close()
            landmarker = null
        }
    }

    companion object {
        private const val TAG = "PoseAnalyzer"
        private const val MODEL_ASSET = "pose_landmarker_lite.task"
    }
}
