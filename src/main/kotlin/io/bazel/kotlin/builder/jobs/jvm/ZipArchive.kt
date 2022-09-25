package io.bazel.kotlin.builder.jobs.jvm

import java.nio.file.Files.write
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream
import kotlin.io.path.readBytes
import kotlin.io.path.relativeTo

class ZipArchive(private val working: Path) {
  fun extract(archive: Path) = ZipInputStream(archive.inputStream().buffered())
    .use { zis ->
      working.resolve(archive.fileName).createDirectories().let { root ->
        generateSequence {
          zis.nextEntry?.run {
            write(root.resolve(name).createDirectories(), zis.readAllBytes())
          }
        }
      }
    }
    .toList()

  fun compress(archiveName: String, root: Path, files: Iterable<Path>): Path =
    working.resolve(archiveName).apply {
      ZipOutputStream(
        working.resolve(archiveName).outputStream().buffered()
      ).use { zos ->
        files.forEach { file ->
          zos.putNextEntry(ZipEntry(file.relativeTo(root).toString()))
          zos.write(file.readBytes())
          zos.closeEntry()
        }
      }
    }
}
