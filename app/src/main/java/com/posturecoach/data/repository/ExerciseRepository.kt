package com.posturecoach.data.repository

import android.content.Context
import com.posturecoach.domain.model.Exercise
import com.posturecoach.domain.model.ExerciseCatalog
import com.posturecoach.domain.model.PostureIssue
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@Singleton
open class ExerciseRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {

    @Volatile
    private var catalog: ExerciseCatalog? = null
    private val mutex = Mutex()

    open suspend fun all(): List<Exercise> = load().exercises

    open suspend fun forIssue(issue: PostureIssue): List<Exercise> =
        load().exercises.filter { it.issue == issue.id }

    open suspend fun forIssues(issues: List<PostureIssue>): List<Exercise> {
        if (issues.isEmpty()) return load().exercises
        val ids = issues.map { it.id }.toSet()
        return load().exercises.filter { it.issue in ids }
    }

    open suspend fun byId(id: String): Exercise? = load().exercises.firstOrNull { it.id == id }

    private suspend fun load(): ExerciseCatalog {
        catalog?.let { return it }
        return mutex.withLock {
            catalog?.let { return@withLock it }
            val text = withContext(Dispatchers.IO) {
                context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
            }
            val decoded = json.decodeFromString(ExerciseCatalog.serializer(), text)
            catalog = decoded
            decoded
        }
    }

    companion object {
        private const val ASSET_NAME = "exercises.json"
    }
}
