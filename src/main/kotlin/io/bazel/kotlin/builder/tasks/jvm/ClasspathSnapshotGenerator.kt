package io.bazel.kotlin.builder.tasks.jvm

import java.nio.file.Path
import kotlin.system.measureTimeMillis
import org.jetbrains.kotlin.buildtools.api.CompilationService
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import java.io.File
import java.util.logging.Logger
import kotlin.io.path.exists

@OptIn(ExperimentalBuildToolsApi::class)
class ClasspathSnapshotGenerator(
    private val inputJar: Path,
    private val outputSnapshot: Path,
    private val granularity: SnapshotGranularity
) {

    fun run() {
        // if (outputSnapshot.exists()) { return }
        val timeSpent = measureTimeMillis {
            val compilationService = CompilationService.loadImplementation(this.javaClass.classLoader!!)
            val snapshot =
                compilationService.calculateClasspathSnapshot(
                    inputJar.toFile(), granularity.toClassSnapshotGranularity)
            snapshot.saveSnapshot(outputSnapshot.toFile())
        }
        // TODO: Log impl
        // LOG.info("$timeSpent ms for input jar: $inputJar")
    }
}
