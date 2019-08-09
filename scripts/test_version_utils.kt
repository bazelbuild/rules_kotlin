import java.util.concurrent.TimeUnit
import java.io.File

fun String.cmd(workingDir: File = File(".")): ProcessBuilder {
    val parts = this.split("\\s".toRegex())
    return ProcessBuilder(*parts.toTypedArray())
        .directory(workingDir)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
}

fun List<String>.parseMap(separator: String = "="): Map<String, String> =
    this.map { if (!it.trim().isEmpty()) it.split(separator) else null }
        .filterNotNull()
        .map { (key, value) -> key to value }
        .toMap()

fun ProcessBuilder.withEnv(key: String, value: String): ProcessBuilder {
    this.environment()[key] = value
    return this
}

/** Starts the process and blocks until it terminates or the timeout is reached. */
fun ProcessBuilder.exec(timeout: Long = 30, unit: TimeUnit = TimeUnit.MINUTES): Process =
    this.start().also { it.waitFor(timeout, unit) }

// padEnd with a default.
fun String.pad(width: Int = VERSION_WIDTH) = this.padEnd(width)

/** Returns the substring after the delimiter, or itself if the delelimiter is not found */
fun String.after(delimiter: String) =
    with(this.split(delimiter, limit = 2)) {
        if (size > 1) this[1]
        else this[0]
    }
