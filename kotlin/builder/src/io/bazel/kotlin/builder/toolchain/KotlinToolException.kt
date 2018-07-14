package io.bazel.kotlin.builder.toolchain

sealed class KotlinToolException(
    msg: String,
    ex: Throwable? = null
) : RuntimeException(msg, ex)

class CompilationException(msg: String, cause: Throwable? = null) :
    KotlinToolException(msg, cause)

class CompilationStatusException(
    msg: String,
    val status: Int,
    val lines: List<String> = emptyList()
) : KotlinToolException("$msg:${lines.joinToString("\n", "\n")}")