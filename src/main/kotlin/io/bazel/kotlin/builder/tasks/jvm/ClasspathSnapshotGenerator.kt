package io.bazel.kotlin.builder.tasks.jvm

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmClasspathSnapshottingOperation
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.exists
import kotlin.system.measureTimeMillis

@OptIn(ExperimentalBuildToolsApi::class)
class ClasspathSnapshotGenerator(
  private val inputJar: Path,
  private val outputSnapshot: Path,
  private val granularity: SnapshotGranularity,
) {
  private val hashPath: Path by lazy {
    outputSnapshot.resolveSibling(outputSnapshot.fileName.toString() + ".hash")
  }

  fun run() {
    if (!isSnapshotOutdated()) {
      return
    }

    val timeSpent =
      measureTimeMillis {
        val toolchains = KotlinToolchains.loadImplementation(this.javaClass.classLoader!!)
        val jvmToolchain = toolchains.jvm

        // Create classpath snapshotting operation
        val snapshotOperation = jvmToolchain.createClasspathSnapshottingOperation(inputJar)
        snapshotOperation.set(
          JvmClasspathSnapshottingOperation.GRANULARITY,
          granularity.toClassSnapshotGranularity,
        )

        // Execute the operation and save the snapshot
        val snapshot = toolchains.createBuildSession().use { session ->
          session.executeOperation(snapshotOperation)
        }

        // TODO : make things atomic / avoid race conditions
        val hash = hashFile(inputJar)
        snapshot.saveSnapshot(outputSnapshot)
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
