package com.posturecoach.data.repository

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.posturecoach.domain.model.PostureIssue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExerciseRepositoryTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val json = Json { ignoreUnknownKeys = true }
    private val repo = ExerciseRepository(context, json)

    @Test
    fun `all returns full catalog from assets`() = runTest {
        val all = repo.all()
        // The bundled exercises.json carries 4 entries: 2 forward-head, 1 rounded, 1 slouch.
        assertThat(all).hasSize(4)
    }

    @Test
    fun `forIssue returns 2 entries for FORWARD_HEAD`() = runTest {
        val ex = repo.forIssue(PostureIssue.FORWARD_HEAD)
        assertThat(ex.map { it.id }).containsExactly("chin_tucks", "neck_stretch")
        ex.forEach { assertThat(it.issue).isEqualTo("forward_head") }
    }

    @Test
    fun `forIssues with empty list returns full catalog`() = runTest {
        val all = repo.forIssues(emptyList())
        assertThat(all).hasSize(4)
    }

    @Test
    fun `forIssues filters across multiple issues`() = runTest {
        val ex = repo.forIssues(listOf(PostureIssue.ROUNDED_SHOULDERS, PostureIssue.SLOUCHING))
        val issues = ex.map { it.issue }.toSet()
        assertThat(issues).containsExactly("rounded_shoulders", "slouching")
    }

    @Test
    fun `byId returns known exercise`() = runTest {
        val found = repo.byId("chin_tucks")
        assertThat(found).isNotNull()
        assertThat(found!!.name).isEqualTo("Chin tucks")
    }

    @Test
    fun `byId returns null for unknown`() = runTest {
        assertThat(repo.byId("unknown")).isNull()
    }

    @Test
    fun `catalog is cached so subsequent calls reuse the same instance contents`() = runTest {
        val first = repo.all()
        val second = repo.all()
        // Equal by value because Exercise is a data class.
        assertThat(second).isEqualTo(first)
    }
}
