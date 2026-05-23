package com.posturecoach.domain.pose

import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.posturecoach.domain.model.PostureIssue
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

/**
 * Drives the real MediaPipe pose model against four fixture images.
 * Fixtures expected at app/src/androidTest/assets/fixtures/:
 *   - good_posture.jpg → no issues
 *   - forward_head.jpg → FORWARD_HEAD flagged
 *   - slouch.jpg       → SLOUCHING flagged
 *   - blank.jpg        → analyzer returns null
 *
 * Each individual assertion is wrapped in `assumeTrue` for the fixture file existence
 * so the test class still compiles and runs in CI before fixtures land in the repo.
 * Once fixtures are committed the assumes degrade into hard assertions automatically.
 */
@RunWith(AndroidJUnit4::class)
class PoseAnalyzerAndroidTest {

    private lateinit var analyzer: PoseAnalyzerImpl
    private lateinit var workingDir: File

    @Before
    fun setup() {
        val targetCtx = InstrumentationRegistry.getInstrumentation().targetContext
        analyzer = PoseAnalyzerImpl(targetCtx)
        workingDir = File(targetCtx.cacheDir, "pose_fixtures").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        analyzer.close()
        workingDir.deleteRecursively()
    }

    @Test
    fun goodPostureProduces33LandmarksAndNoIssues() = runTest {
        val file = stageFixture("good_posture.jpg") ?: return@runTest
        val result = analyzer.analyzeFile(file.absolutePath)
        assertThat(result).isNotNull()
        assertThat(result!!.landmarks).hasSize(33)
        val issues = PostureRules.analyze(result.landmarks).issues
        assertThat(issues).doesNotContain(PostureIssue.FORWARD_HEAD)
        assertThat(issues).doesNotContain(PostureIssue.SLOUCHING)
    }

    @Test
    fun forwardHeadFixtureFlagsForwardHead() = runTest {
        val file = stageFixture("forward_head.jpg") ?: return@runTest
        val result = analyzer.analyzeFile(file.absolutePath)
        assertThat(result).isNotNull()
        assertThat(result!!.landmarks).hasSize(33)
        val issues = PostureRules.analyze(result.landmarks).issues
        assertThat(issues).contains(PostureIssue.FORWARD_HEAD)
    }

    @Test
    fun slouchFixtureFlagsSlouching() = runTest {
        val file = stageFixture("slouch.jpg") ?: return@runTest
        val result = analyzer.analyzeFile(file.absolutePath)
        assertThat(result).isNotNull()
        assertThat(result!!.landmarks).hasSize(33)
        val issues = PostureRules.analyze(result.landmarks).issues
        assertThat(issues).contains(PostureIssue.SLOUCHING)
    }

    @Test
    fun blankImageReturnsNull() = runTest {
        val file = stageFixture("blank.jpg") ?: return@runTest
        val result = analyzer.analyzeFile(file.absolutePath)
        assertThat(result).isNull()
    }

    /**
     * Copies a fixture asset to a temp file the analyzer can read by path.
     * Returns null (and skips the test via assume) when the asset isn't present
     * so test-suite execution doesn't fail before fixtures are committed.
     */
    private fun stageFixture(name: String): File? {
        val instrCtx = InstrumentationRegistry.getInstrumentation().context
        val available = try {
            instrCtx.assets.list("fixtures")?.contains(name) == true
        } catch (_: Throwable) {
            false
        }
        assumeTrue("Skipping: missing androidTest/assets/fixtures/$name", available)
        val outFile = File(workingDir, name)
        instrCtx.assets.open("fixtures/$name").use { input ->
            outFile.outputStream().use { input.copyTo(it) }
        }
        return outFile
    }
}
