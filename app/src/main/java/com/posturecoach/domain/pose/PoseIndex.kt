package com.posturecoach.domain.pose

/**
 * MediaPipe Pose Landmarker indices (33-keypoint model).
 * https://developers.google.com/mediapipe/solutions/vision/pose_landmarker
 */
object PoseIndex {
    const val NOSE = 0
    const val LEFT_EYE_INNER = 1
    const val LEFT_EYE = 2
    const val LEFT_EYE_OUTER = 3
    const val RIGHT_EYE_INNER = 4
    const val RIGHT_EYE = 5
    const val RIGHT_EYE_OUTER = 6
    const val LEFT_EAR = 7
    const val RIGHT_EAR = 8
    const val MOUTH_LEFT = 9
    const val MOUTH_RIGHT = 10
    const val LEFT_SHOULDER = 11
    const val RIGHT_SHOULDER = 12
    const val LEFT_ELBOW = 13
    const val RIGHT_ELBOW = 14
    const val LEFT_WRIST = 15
    const val RIGHT_WRIST = 16
    const val LEFT_HIP = 23
    const val RIGHT_HIP = 24
    const val LEFT_KNEE = 25
    const val RIGHT_KNEE = 26
    const val LEFT_ANKLE = 27
    const val RIGHT_ANKLE = 28

    val NAMES = mapOf(
        NOSE to "nose",
        LEFT_EYE to "leftEye",
        RIGHT_EYE to "rightEye",
        LEFT_EAR to "leftEar",
        RIGHT_EAR to "rightEar",
        LEFT_SHOULDER to "leftShoulder",
        RIGHT_SHOULDER to "rightShoulder",
        LEFT_ELBOW to "leftElbow",
        RIGHT_ELBOW to "rightElbow",
        LEFT_HIP to "leftHip",
        RIGHT_HIP to "rightHip",
        LEFT_KNEE to "leftKnee",
        RIGHT_KNEE to "rightKnee",
        LEFT_ANKLE to "leftAnkle",
        RIGHT_ANKLE to "rightAnkle",
    )

    val SKELETON_EDGES: List<Pair<Int, Int>> = listOf(
        LEFT_SHOULDER to RIGHT_SHOULDER,
        LEFT_SHOULDER to LEFT_HIP,
        RIGHT_SHOULDER to RIGHT_HIP,
        LEFT_HIP to RIGHT_HIP,
        LEFT_SHOULDER to LEFT_ELBOW,
        LEFT_ELBOW to LEFT_WRIST,
        RIGHT_SHOULDER to RIGHT_ELBOW,
        RIGHT_ELBOW to RIGHT_WRIST,
        LEFT_HIP to LEFT_KNEE,
        LEFT_KNEE to LEFT_ANKLE,
        RIGHT_HIP to RIGHT_KNEE,
        RIGHT_KNEE to RIGHT_ANKLE,
        LEFT_EAR to LEFT_SHOULDER,
        RIGHT_EAR to RIGHT_SHOULDER,
    )
}
