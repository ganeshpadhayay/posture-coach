# Pose fixture images

These four JPGs feed `PoseAnalyzerAndroidTest`:

| File | What it should look like | Test expectation |
| --- | --- | --- |
| `good_posture.jpg` | Side profile, ear-shoulder-hip stacked, level shoulders | 33 landmarks, no issues |
| `forward_head.jpg` | Side profile with head pushed clearly forward of the shoulders | `FORWARD_HEAD` flagged |
| `slouch.jpg` | Seated side profile leaning forward (spine > 15° from vertical) | `SLOUCHING` flagged |
| `blank.jpg` | A solid color or scenery with no people | analyzer returns `null` |

Until these files are committed the corresponding tests are skipped via `Assume`.
Add the files to `app/src/androidTest/assets/fixtures/` and the tests will start
running automatically.
