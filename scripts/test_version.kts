#!/usr/bin/env kscript

// Build the dagger example using different versions, via bazelisk.

@file:Include("test_version_utils.kt")


val TARGET = "//examples/dagger/..."
val PRE_FLAGS = "--bazelrc=/dev/null"
val BAZEL_EXEC = "bazel $PRE_FLAGS"
// Versions to test.
val VERSIONS = """
    0.16.0
    0.16.1
    0.17.1
    0.17.2
    0.18.0
    0.18.1
    0.19.0
    0.19.1
    0.19.2
    0.20.0
    0.21.0
    0.22.0
    0.23.0
    0.23.1
    0.23.2
    0.24.0
    0.24.1
    0.25.0
    0.25.1
    0.25.2
    0.25.3
    0.26.0
    0.26.1
    0.27.0
    0.27.1
    0.27.2
    0.28.0
    0.28.1
    """.trimIndent().trim().split("\n")

val VERSION_WIDTH = VERSIONS.map(String::length).max() ?: 10
println("Testing with bazel versions: ${VERSIONS.joinToString(" ")}")

enum class Status { SUCCESS, FAILURE }
data class VerifyResult(
    val status: Status,
    val code: Int = 0,
    val output: String? = null
)

val outputFile = File(if (args.size >= 1) args[0] else "matrix.md")

fun Process.errorText(): String = errorStream.bufferedReader().readText()
fun Process.result(): VerifyResult =
    when (this.exitValue()) {
        0 -> VerifyResult(Status.SUCCESS, 0, errorText())
        else -> VerifyResult(Status.FAILURE, exitValue(), errorText())
    }

val matrix = mutableMapOf<String, VerifyResult>()
loop@ for (version in VERSIONS) {
    try {
        // clean as a fire and forget.
        print("Testing version $version...")
        print(" cleaning...")
        "$BAZEL_EXEC clean".cmd().withEnv("USE_BAZEL_VERSION", version).exec(10)
        print(" running...")
        val proc: Process =
            "$BAZEL_EXEC build $TARGET".cmd().withEnv("USE_BAZEL_VERSION", version).exec(10)
        with(proc.result()) {
            matrix[version] = this
            if (status == Status.FAILURE) {
                print(" writing log...")
                val outputlog = "Execution log for $BAZEL_EXEC build $TARGET\n${output ?: "No log"}"
                outputFile.resolveSibling("bazel_$version.log").writeText(outputlog)
            }
            println(" done: ${this.status}")
        }
    } catch (err: Exception) {
        println("${err.javaClass.simpleName} while running bazel: ${err.message}")
        err.printStackTrace()
    }
}

fun format(matrix: Map<String, VerifyResult>): String =
    matrix.entries
        .map { (version, result) ->
            val status = when (result.status) {
                Status.SUCCESS -> "![Yes]"
                Status.FAILURE -> "![No]"
                else -> "![Unknown]"
            }
            val error = when {
                result.output == null -> ""
                result.output.contains("error 404") -> "404 error fetching bazel"
                result.output.contains("ERROR") -> {
                    result.output
                        .lines()
                        .filter { it.contains("ERROR") }
                        .joinToString("<br />")
                }
                result.output.contains("FATAL") -> {
                    result.output
                        .lines()
                        .filter { it.contains("FATAL") }
                        .map { it.after("] ").trim() }
                        .joinToString("<br />")
                }
                else -> ""
            }
            "\n    | ${version.pad()} | ${status.pad(6)} | $error |"
        }
        .joinToString("")
println("writing markdown to $outputFile")
outputFile.writeText(
    """
    # Bazel Kotlin Rules compatibility
    
    Which version of *rules_kotlin* can you use with which version of Bazel (best
    effort testing)?
    
    | Compatibility | Current | Errors |
    | ---- | ----  | ---- |${format(matrix)}
    
    [Yes]: https://img.shields.io/static/v1.svg?label=&message=Yes&color=green
    [No]: https://img.shields.io/static/v1.svg?label=&message=No&color=red
    [Unknown]: https://img.shields.io/static/v1.svg?label=&message=???&color=lightgrey
    
    """.trimIndent()
)
