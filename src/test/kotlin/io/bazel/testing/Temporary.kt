package io.bazel.testing

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Files.createDirectories
import java.nio.file.Files.write
import java.nio.file.Path

object Temporary {
  inline fun <reified TEST> directoryFor(configuration: FileContext.() -> Unit = {}): Path {
    return Files.createTempDirectory(TEST::class.qualifiedName).also { root ->
      FileContext(root).apply(configuration)
    }
  }

  class FileContext(private val root: Path) {
    fun file(
      path: String,
      contents: String = ""
    ): Path = write(
      root.resolve(path).apply {
        createDirectories(parent)
      },
      contents.toByteArray(UTF_8)
    )
  }
}
