package io.bazel.kotlin.compiler

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmClasspathSnapshottingOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmClasspathSnapshottingOperation.Companion.PARSE_INLINED_LOCAL_CLASSES
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
        snapshotOperation.set(PARSE_INLINED_LOCAL_CLASSES, true)

        // Execute the operation and save the snapshot
        val snapshot =
          toolchains.createBuildSession().use { session ->
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

  companion object {
    /**
     * Static entry point that can be invoked via reflection.
     * @param inputJar path to the input jar file
     * @param outputSnapshot path to the output snapshot file
     * @param granularity "CLASS_LEVEL" or "CLASS_MEMBER_LEVEL"
     */
    @JvmStatic
    fun generate(
      inputJar: String,
      outputSnapshot: String,
      granularity: String,
    ) {
      ClasspathSnapshotGenerator(
        Path.of(inputJar),
        Path.of(outputSnapshot),
        SnapshotGranularity.valueOf(granularity),
      ).run()
    }
  }
}
