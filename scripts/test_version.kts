#!/usr/bin/env kscript

// Build the dagger example using different versions, via bazelisk.

@file:Include("test_version_utils.kt")

val TARGET = "//examples/dagger/..."

// Versions to test.
val VERSIONS = listOf(
    "0.19.0",
/*    "0.26.0",
    "0.27.0",
    "0.27.1",
    "0.28.0",
    "0.28.1",
*/    "0.29.0rc4"
)
val VERSION_WIDTH = VERSIONS.map(String::length).max() ?: 10
println("Testing with bazel versions: ${VERSIONS.joinToString(" ")}")

enum class Status { SUCCESS, FAILURE }
data class VerifyResult(val status: Status, val code: Int = 0, val output: String? = null)

val outputFile = File(if (args.size >= 1) args[0] else "matrix.md")

fun Process.result(): VerifyResult =
    when (this.exitValue()) {
        0 -> VerifyResult(Status.SUCCESS)
        else -> VerifyResult(Status.FAILURE, exitValue(), errorStream.bufferedReader().readText())
    }

val matrix = mutableMapOf<String, VerifyResult>()
loop@ for (version in VERSIONS) {
    try {
        val proc: Process =
            "bazel build $TARGET".cmd().withEnv("USE_BAZEL_VERSION", version).exec(10)
        with(proc.result()) {
            matrix[version] = this
            println("$version: ${this.status}")
        }
    } catch (err: Exception) {
        println("${err.javaClass.simpleName} while running bazel: ${err.message}")
        err.printStackTrace()
    }
}

// padEnd with a default.
fun String.pad(width: Int = VERSION_WIDTH) = this.padEnd(width)

fun format(matrix: Map<String, VerifyResult>): String =
    matrix.entries
        .map { (version, result) ->
            val status = when (result.status) {
                Status.SUCCESS -> "![Yes]"
                Status.FAILURE -> "![No]"
                else -> "![Unknown]"
            }
            val error = when {
                result.output.contains("error 404") -> "404 error fetching bazel"
                result.output.contains("ERROR:") -> {
                    result.output
                        .lines()
                        .filter { it.contains("ERROR:") }
                        .joinToString { "<br />" }
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
