package io.bazel.kotlin.builder.tasks.jvm

import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.system.measureTimeMillis
import org.jetbrains.kotlin.buildtools.api.CompilationService
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import kotlin.io.path.exists

@OptIn(ExperimentalBuildToolsApi::class)
class ClasspathSnapshotGenerator(
    private val inputJar: Path,
    private val outputSnapshot: Path,
    private val granularity: SnapshotGranularity
) {

    private val hashPath: Path by lazy {
      outputSnapshot.resolveSibling(outputSnapshot.fileName.toString() + ".hash")
    }

    fun run() {
        if (!isSnapshotOutdated()) {
          return
        }

        val timeSpent = measureTimeMillis {
          val compilationService =
            CompilationService.loadImplementation(this.javaClass.classLoader!!)
          val snapshot =
            compilationService.calculateClasspathSnapshot(
              inputJar.toFile(), granularity.toClassSnapshotGranularity
            )
          // TODO : make things atomic / avoid race conditions
          val hash = hashFile(inputJar)
          snapshot.saveSnapshot(outputSnapshot.toFile())
          hashPath.toFile().writeText(hash)
        }

        // TODO: Log impl
        // LOG.info("$timeSpent ms for input jar: $inputJar")
    }

    private fun isSnapshotOutdated(): Boolean {
      if (!outputSnapshot.exists() || !hashPath.exists()) {
        return true
      }
      val storedHash = Files.readAllLines(hashPath).firstOrNull()?.trim()
      val currentHash = hashFile(inputJar)
      return storedHash == null || storedHash != currentHash
    }

    private fun hashFile(path: Path): String {
      val digest = MessageDigest.getInstance("SHA-256")
      FileInputStream(path.toFile()).use { fis ->
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (fis.read(buffer).also { bytesRead = it } != -1) {
          digest.update(buffer, 0, bytesRead)
        }
      }
      return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
