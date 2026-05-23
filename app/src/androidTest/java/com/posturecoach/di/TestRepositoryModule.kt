package com.posturecoach.di

import com.posturecoach.domain.model.PoseLandmark
import com.posturecoach.domain.pose.AnalyzedPose
import com.posturecoach.domain.pose.PoseAnalyzer
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Replaces the production [BindingsModule] in instrumentation tests so we don't depend
 * on a real MediaPipe model or a real Hilt graph for things that need a [PoseAnalyzer].
 *
 * Tests can swap behavior at runtime via the static singleton inside [FakePoseAnalyzer].
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [BindingsModule::class])
object TestRepositoryModule {

    @Provides
    @Singleton
    fun providePoseAnalyzer(): PoseAnalyzer = FakePoseAnalyzer
}

object FakePoseAnalyzer : PoseAnalyzer {
    @Volatile
    var nextResult: AnalyzedPose? = null

    @Volatile
    var nextError: Throwable? = null

    override suspend fun analyzeFile(path: String): AnalyzedPose? {
        nextError?.let { throw it }
        return nextResult
    }

    override fun close() = Unit

    fun reset() {
        nextResult = null
        nextError = null
    }

    fun setLandmarks(landmarks: List<PoseLandmark>, width: Int = 1080, height: Int = 1920) {
        nextResult = AnalyzedPose(landmarks = landmarks, imageWidth = width, imageHeight = height)
        nextError = null
    }
}
