/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bazel.kotlin.builder.utils.jars

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.util.TreeMap
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.regex.Pattern
import java.util.stream.Stream

/**
 * Source jar packager for JavaLike files. The placement is discovered from the package entry.
 */
class SourceJarCreator(
  path: Path,
  verbose: Boolean = false
) : JarHelper(path, normalize = true, verbose = verbose) {
  companion object {
    private const val BL = """\p{Blank}*"""
    private const val COM_BL = """$BL(?:/\*[^\n]*\*/$BL)*"""
    private val PKG_PATTERN: Pattern =
      Pattern.compile("""^${COM_BL}package$COM_BL([a-zA-Z0-9._]+)$COM_BL(?:;?.*)$""")

    @JvmStatic
    fun extractPackage(line: String): String? =
      PKG_PATTERN.matcher(line).takeIf { it.matches() }?.group(1)

    private fun isJavaSourceLike(name: String): Boolean {
      return name.endsWith(".kt") || name.endsWith(".java")
    }
  }

  sealed class Entry {
    class File(val path: Path, val content: ByteArray) : Entry() {
      override fun toString(): String = "File $path"
    }

    object Directory : Entry() {
      override fun toString(): String = "Directory"
    }
  }

  private class JarFilenameHelper {
    /**
     * A map from the directories in the underlying filesystem to jar directory names.
     */
    private val directoryToPackageMap = mutableMapOf<Path, String>()

    /**
     * Entries for which packages paths could not be located during processing.
     */
    private val deferredEntries = mutableMapOf<Path, ByteArray>()

    /**
     * Locate the directory name for the file as it should appear in the jar.
     *
     * If the directory could not be located add it to the deferred list and return null.
     *
     * Files like `package-info.java` could end up getting deferred if they have an annotation embedded on the same
     * line or files that have entries such as `/* weird comment */package lala`
     */
    fun getFilenameOrDefer(sourceFile: Path, body: ByteArray): String? =
      directoryToPackageMap[sourceFile.parent] ?: locatePackagePathOrDefer(sourceFile, body)?.let {
        "$it/${sourceFile.fileName}"
      }

    /**
     * Visit any deferred entries.
     *
     * @param block the visitor, the second param is the package name and may still be null.
     */
    fun visitDeferredEntries(block: (Path, String?, ByteArray) -> Unit) {
      deferredEntries.forEach { sourceFile, bytes ->
        block(sourceFile, directoryToPackageMap[sourceFile.parent], bytes)
      }
    }

    private fun locatePackagePathOrDefer(sourceFile: Path, body: ByteArray): String? =
      body.inputStream().bufferedReader().useLines {
        it.mapNotNull(::extractPackage).firstOrNull()?.replace('.', '/')
      }.also {
        if (it == null) {
          deferredEntries[sourceFile] = body
        }
      }
  }

  private val filenameHelper = JarFilenameHelper()
  private val entries = TreeMap<String, Entry>()

  /**
   * Consume a stream of sources, this should contain valid source files and srcjar.
   */
  fun addSources(sources: Stream<Path>) {
    sources.forEach { path ->
      val fileName = path.fileName.toString()
      when {
        isJavaSourceLike(fileName) -> addJavaLikeSourceFile(path)
        fileName.endsWith(".srcjar") -> addSourceJar(path)
      }
    }
  }

  /**
   * Add a single source jar
   */
  private fun addSourceJar(path: Path) {
    if (verbose) {
      System.err.println("adding source jar: $path")
    }
    JarFile(path.toFile()).use { jar ->
      for (entry in jar.entries()) {
        if (!entry.isDirectory) {
          if (isJavaSourceLike(entry.name)) {
            jar.getInputStream(entry).readBytes().also {
              addEntry(entry.name, path, it)
            }
          }
        }
      }
    }
  }

  /**
   * Add a single source file. This method uses the [JarFilenameHelper] so it should be used when jar filename
   * correction is desired. It should only be used for Java-Like source files.
   */
  private fun addJavaLikeSourceFile(sourceFile: Path) {
    val bytes = Files.readAllBytes(sourceFile)
    filenameHelper.getFilenameOrDefer(sourceFile, bytes)?.also {
      addEntry(it, sourceFile, bytes)
    }
  }

  fun execute() {
    if (verbose) {
      System.err.println("creating source jar file: $jarPath")
    }
    filenameHelper.visitDeferredEntries { path, jarFilename, bytes ->
      if (jarFilename == null) {
        if (verbose) {
          val body = bytes.toString(Charset.defaultCharset())
          System.err.println("""could not determine jar entry name for $path. Body:\n$body}""")
        } else {
          // if not verbose silently add files at the root.
          addEntry(path.fileName.toString(), path, bytes)
        }
      } else {
        System.err.println("adding deferred source file $path -> $jarFilename")
        addEntry(jarFilename, path, bytes)
      }
    }
    Files.newOutputStream(jarPath).use {
      JarOutputStream(it).use { out ->
        for ((key, value) in entries) {
          try {
            when (value) {
              is Entry.File -> out.copyEntry(key, value.path, value.content)
              is Entry.Directory -> out.copyEntry(key)
            }
          } catch (throwable: Throwable) {
            throw RuntimeException("could not copy JarEntry $key $value", throwable)
          }
        }
      }
    }
  }

  private fun addEntry(name: String, path: Path, bytes: ByteArray) {
    name.split("/").also {
      if (it.size >= 2) {
        for (i in ((it.size - 1) downTo 1)) {
          val dirName = it.subList(0, i).joinToString("/", postfix = "/")
          if (entries.putIfAbsent(dirName, Entry.Directory) != null) {
            break
          } else if (verbose) {
            System.err.println("adding directory: $dirName")
          }
        }
      }
    }

    val result = entries.putIfAbsent(name, Entry.File(path, bytes))
    require(result as? Entry.Directory != null || result == null) {
      "source entry jarName: $name from: $path collides with entry from: ${(result as Entry.File).path}"
    }
  }
}
