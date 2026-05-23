package com.posturecoach.testing

import com.posturecoach.data.repository.ExerciseRepository
import com.posturecoach.domain.model.Exercise
import com.posturecoach.domain.model.PostureIssue
import io.mockk.mockk
import kotlinx.serialization.json.Json

class FakeExerciseRepository(
    private val catalog: MutableList<Exercise> = mutableListOf(
        Exercise("chin_tucks", "Chin tucks", "forward_head", listOf("Sit tall.", "Tuck."), 60, "exercises/chin_tucks.gif"),
        Exercise("neck_stretch", "Neck side stretch", "forward_head", listOf("Drop ear."), 90, "exercises/neck_stretch.gif"),
        Exercise("wall_angels", "Wall angels", "rounded_shoulders", listOf("Stand against wall."), 90, "exercises/wall_angels.gif"),
        Exercise("cat_cow", "Cat-cow", "slouching", listOf("Hands and knees."), 90, "exercises/cat_cow.gif"),
    ),
) : ExerciseRepository(
    context = mockk(relaxed = true),
    json = Json,
) {
    override suspend fun all(): List<Exercise> = catalog.toList()

    override suspend fun forIssue(issue: PostureIssue): List<Exercise> =
        catalog.filter { it.issue == issue.id }

    override suspend fun forIssues(issues: List<PostureIssue>): List<Exercise> {
        if (issues.isEmpty()) return catalog.toList()
        val ids = issues.map { it.id }.toSet()
        return catalog.filter { it.issue in ids }
    }

    override suspend fun byId(id: String): Exercise? = catalog.firstOrNull { it.id == id }
}
