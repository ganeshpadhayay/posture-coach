package com.posturecoach.data.db

import com.posturecoach.data.db.entities.PostureScanEntity
import com.posturecoach.domain.model.PoseLandmark
import com.posturecoach.domain.model.PostureIssue
import com.posturecoach.domain.model.PostureScan
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class PersistedLandmark(
    val name: String,
    val x: Float,
    val y: Float,
    val visibility: Float,
)

private val landmarkSerializer = ListSerializer(PersistedLandmark.serializer())
private val angleSerializer = MapSerializer(String.serializer(), Float.serializer())

fun PostureScan.toEntity(json: Json): PostureScanEntity = PostureScanEntity(
    id = id,
    timestampMs = timestampMs,
    imagePath = imagePath,
    imageWidth = imageWidth,
    imageHeight = imageHeight,
    issuesCsv = issues.joinToString(",") { it.id },
    anglesJson = json.encodeToString(angleSerializer, angles),
    landmarksJson = json.encodeToString(
        landmarkSerializer,
        landmarks.map { PersistedLandmark(it.name, it.x, it.y, it.visibility) },
    ),
)

fun PostureScanEntity.toDomain(json: Json): PostureScan = PostureScan(
    id = id,
    timestampMs = timestampMs,
    imagePath = imagePath,
    imageWidth = imageWidth,
    imageHeight = imageHeight,
    issues = issuesCsv.split(",")
        .filter { it.isNotBlank() }
        .mapNotNull { PostureIssue.fromId(it) },
    angles = runCatching { json.decodeFromString(angleSerializer, anglesJson) }
        .getOrDefault(emptyMap()),
    landmarks = runCatching {
        json.decodeFromString(landmarkSerializer, landmarksJson).map {
            PoseLandmark(it.name, it.x, it.y, it.visibility)
        }
    }.getOrDefault(emptyList()),
)
