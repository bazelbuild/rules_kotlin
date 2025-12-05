package io.bazel.kotlin.test.ksp

import com.squareup.moshi.JsonClass

/**
 * Simple data class annotated with Moshi's @JsonClass to trigger KSP processing.
 */
@JsonClass(generateAdapter = true)
data class TestModel(
    val id: Int,
    val name: String,
    val tags: List<String> = emptyList()
)
