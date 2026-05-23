package com.posturecoach.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.posturecoach.domain.pose.AnalyzedPose
import com.posturecoach.domain.pose.PoseAnalyzer
import com.posturecoach.testing.FakeScanRepository
import com.posturecoach.testing.LandmarkFactory
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AnalyzePostureUseCaseTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val scanRepo = FakeScanRepository()
    private lateinit var imageFile: File

    @Before
    fun setup() {
        imageFile = tempFolder.newFile("scan.jpg")
        imageFile.writeBytes(ByteArray(8))
    }

    @Test
    fun `success persists scan with computed issues and returns Success`() = runTest {
        val analyzer = StubAnalyzer(
            result = AnalyzedPose(
                landmarks = LandmarkFactory.forwardHead(ratio = 0.3f),
                imageWidth = 1080,
                imageHeight = 1920,
            ),
        )
        val useCase = AnalyzePostureUseCase(analyzer, scanRepo)

        val result = useCase(imageFile.absolutePath)

        assertThat(result).isInstanceOf(AnalyzePostureUseCase.Result.Success::class.java)
        val scan = (result as AnalyzePostureUseCase.Result.Success).scan
        assertThat(scan.imagePath).isEqualTo(imageFile.absolutePath)
        assertThat(scan.imageWidth).isEqualTo(1080)
        assertThat(scan.imageHeight).isEqualTo(1920)
        assertThat(scan.issues).contains(com.posturecoach.domain.model.PostureIssue.FORWARD_HEAD)
        assertThat(scanRepo.getById(scan.id)).isEqualTo(scan)
    }

    @Test
    fun `null analyzer result returns NoPoseDetected and does not save`() = runTest {
        val useCase = AnalyzePostureUseCase(StubAnalyzer(result = null), scanRepo)

        val result = useCase(imageFile.absolutePath)

        assertThat(result).isEqualTo(AnalyzePostureUseCase.Result.NoPoseDetected)
        assertThat(scanRepo.observeAll().first()).isEmpty()
    }

    @Test
    fun `empty landmarks list returns NoPoseDetected and does not save`() = runTest {
        val analyzer = StubAnalyzer(
            result = AnalyzedPose(landmarks = emptyList(), imageWidth = 0, imageHeight = 0),
        )
        val useCase = AnalyzePostureUseCase(analyzer, scanRepo)

        val result = useCase(imageFile.absolutePath)

        assertThat(result).isEqualTo(AnalyzePostureUseCase.Result.NoPoseDetected)
        assertThat(scanRepo.observeAll().first()).isEmpty()
    }

    @Test
    fun `analyzer throwing returns Error wrapping the cause`() = runTest {
        val boom = IllegalStateException("camera busted")
        val useCase = AnalyzePostureUseCase(StubAnalyzer(error = boom), scanRepo)

        val result = useCase(imageFile.absolutePath)

        assertThat(result).isInstanceOf(AnalyzePostureUseCase.Result.Error::class.java)
        assertThat((result as AnalyzePostureUseCase.Result.Error).cause).isSameInstanceAs(boom)
    }

    @Test
    fun `success generates a valid UUID id and recent timestamp`() = runTest {
        val analyzer = StubAnalyzer(
            result = AnalyzedPose(landmarks = LandmarkFactory.aligned(), 100, 100),
        )
        val useCase = AnalyzePostureUseCase(analyzer, scanRepo)
        val before = System.currentTimeMillis()

        val result = useCase(imageFile.absolutePath) as AnalyzePostureUseCase.Result.Success
        val after = System.currentTimeMillis()

        val uuidRegex = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
        assertThat(result.scan.id).matches(uuidRegex.pattern)
        assertThat(result.scan.timestampMs).isAtLeast(before)
        assertThat(result.scan.timestampMs).isAtMost(after)
    }

    private class StubAnalyzer(
        var result: AnalyzedPose? = null,
        var error: Throwable? = null,
    ) : PoseAnalyzer {
        override suspend fun analyzeFile(path: String): AnalyzedPose? {
            error?.let { throw it }
            return result
        }

        override fun close() = Unit
    }
}
