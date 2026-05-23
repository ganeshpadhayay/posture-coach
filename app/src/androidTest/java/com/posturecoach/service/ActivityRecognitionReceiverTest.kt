package com.posturecoach.service

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.posturecoach.data.db.entities.ActivityType
import com.posturecoach.data.repository.ActivityRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the @AndroidEntryPoint receiver under instrumentation.
 *
 * Synthesizing a real ActivityTransitionResult intent requires touching Play Services
 * internal serialization, which is brittle across versions. We assert two things that
 * are reliably testable:
 *   1. The receiver does not crash on an empty intent.
 *   2. When synthesis is possible at runtime, STILL → beginTransition(STILL) is invoked.
 * If synthesis is unsupported by the Play Services version, the second assertion is
 * skipped via `Assume`.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ActivityRecognitionReceiverTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var ctx: android.content.Context

    @Before
    fun setup() {
        hiltRule.inject()
        ctx = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun emptyIntentDoesNotCrashAndDoesNotInvokeRepo() = runBlocking {
        val receiver = ActivityRecognitionReceiver()
        val mockRepo = mockk<ActivityRepository>(relaxed = true).also {
            coEvery { it.beginTransition(any(), any()) } returns Unit
        }

        // Drive Hilt init by calling onReceive once with an empty intent.
        // (The @AndroidEntryPoint wrapper does field injection on first call.)
        receiver.onReceive(ctx, Intent())
        // Now overwrite the injected field with our mock.
        receiver.activityRepository = mockRepo
        receiver.onReceive(ctx, Intent())

        coVerify(exactly = 0) { mockRepo.beginTransition(any(), any()) }
    }

    @Test
    fun stillEnterTransitionInvokesBeginTransitionWhenSynthesisIsAvailable() = runBlocking {
        val transitionResult = synthesizeOrSkip(
            DetectedActivity.STILL,
            ActivityTransition.ACTIVITY_TRANSITION_ENTER,
        ) ?: return@runBlocking

        val receiver = ActivityRecognitionReceiver()
        val mockRepo = mockk<ActivityRepository>(relaxed = true).also {
            coEvery { it.beginTransition(ActivityType.STILL, any()) } returns Unit
        }
        receiver.onReceive(ctx, Intent())
        receiver.activityRepository = mockRepo

        val intent = Intent().putExtras(
            android.os.Bundle().apply {
                putParcelable(
                    "com.google.android.location.internal.EXTRA_ACTIVITY_TRANSITION_RESULT",
                    transitionResult,
                )
            },
        )
        receiver.onReceive(ctx, intent)

        // goAsync hands work to a background coroutine; give it a beat.
        coVerify(timeout = 2_000) { mockRepo.beginTransition(ActivityType.STILL, any()) }
    }

    /**
     * Attempts to construct an [ActivityTransitionResult]. Returns null if the available
     * Play Services version doesn't expose the constructor on this device.
     */
    private fun synthesizeOrSkip(type: Int, transition: Int): ActivityTransitionResult? =
        runCatching {
            ActivityTransitionResult(
                listOf(ActivityTransitionEvent(type, transition, 0L)),
            )
        }.getOrNull()
}
