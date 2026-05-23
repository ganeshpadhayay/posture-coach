# Assets

## pose_landmarker_lite.task (bundled)

The MediaPipe pose model is committed to the repo (~5.5 MB, float16 lite variant).
Source:

```
https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/latest/pose_landmarker_lite.task
```

MD5 `04a75ddf7c811ac7a1a4523266dd7d88` (base64 `BKdd33yBGsehpFIyZt19iA==`).

For higher accuracy you can swap in `pose_landmarker_full.task` or
`pose_landmarker_heavy.task` from the same `/mediapipe-models/pose_landmarker/`
prefix and update the constant in `com.posturecoach.domain.pose.PoseAnalyzerImpl`.

## exercises/ (placeholders)

GIFs referenced by `exercises.json` (e.g. `chin_tucks.gif`) should be placed in
`assets/exercises/`. For the MVP they may be omitted — Coil will fall back to a
placeholder and the rest of the UI keeps working.
