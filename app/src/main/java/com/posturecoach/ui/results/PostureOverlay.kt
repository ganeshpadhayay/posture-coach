package com.posturecoach.ui.results

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.posturecoach.domain.model.PoseLandmark
import com.posturecoach.domain.pose.PoseIndex

@Composable
fun PostureOverlay(
    landmarks: List<PoseLandmark>,
    modifier: Modifier = Modifier,
    landmarkColor: Color = Color(0xFFE85D75),
    skeletonColor: Color = Color(0xFF2D9CAF),
) {
    if (landmarks.isEmpty()) return
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        PoseIndex.SKELETON_EDGES.forEach { (a, b) ->
            val pa = landmarks.getOrNull(a) ?: return@forEach
            val pb = landmarks.getOrNull(b) ?: return@forEach
            if (pa.visibility < 0.2f || pb.visibility < 0.2f) return@forEach
            drawLine(
                color = skeletonColor,
                start = Offset(pa.x * w, pa.y * h),
                end = Offset(pb.x * w, pb.y * h),
                strokeWidth = 6f,
            )
        }
        landmarks.forEachIndexed { index, lm ->
            if (lm.visibility < 0.2f) return@forEachIndexed
            if (index !in PoseIndex.NAMES) return@forEachIndexed
            drawCircle(
                color = landmarkColor,
                radius = 8f,
                center = Offset(lm.x * w, lm.y * h),
                style = Stroke(width = 4f),
            )
        }
    }
}
