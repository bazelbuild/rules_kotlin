/*
 * Copyright 2024 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.bazel.kotlin.builder.cmd

import java.io.File
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.relativeTo
import kotlin.streams.asSequence

/**
 * Generates zipper arguments from tree artifact directories.
 *
 * This tool walks directory trees and generates a zipper args file that maps
 * relative paths to absolute paths, suitable for creating JARs with the zipper tool.
 *
 * Usage:
 *   PackageTreeArtifacts <args_output> <manifest_output> <input_dir>...
 *
 * Arguments:
 *   args_output     - Path where zipper args file will be written
 *   manifest_output - Path where manifest file will be written
 *   input_dir...    - One or more input directories to package
 *
 * The zipper args file format is:
 *   relative/path/to/file=absolute/path/to/file
 *
 * If no files are found, a minimal manifest is created.
 */
fun main(args: Array<String>) {
  if (args.size < 2) {
    System.err.println("Usage: PackageTreeArtifacts <args_output> <manifest_output> <input_dir>...")
    System.exit(1)
  }

  val argsOutput = File(args[0])
  val manifestOutput = File(args[1])
  val inputDirs = args.drop(2).map { File(it).toPath() }

  try {
    generateZipperArgs(argsOutput, manifestOutput, inputDirs)
  } catch (e: Exception) {
    System.err.println("Error generating zipper args: ${e.message}")
    e.printStackTrace()
    System.exit(1)
  }
}

private fun generateZipperArgs(
  argsOutput: File,
  manifestOutput: File,
  inputDirs: List<Path>,
) {
  var fileCount = 0

  PrintWriter(argsOutput).use { writer ->
    for (dir in inputDirs) {
      if (!Files.exists(dir)) {
        continue
      }

      if (!dir.isDirectory()) {
        // Skip non-directories like the old shell script did
        System.err.println("Warning: $dir is not a directory, skipping")
        continue
      }

      // Check if directory is empty
      val isEmpty = Files.list(dir).use { stream -> !stream.findFirst().isPresent }
      if (isEmpty) {
        continue
      }

      // Walk the directory tree and generate zipper entries
      // Include both files and directories to match the old shell behavior
      Files
        .walk(dir)
        .use { stream ->
          stream
            .asSequence()
            .filter { it != dir } // Skip the root directory itself
            .forEach { path ->
              val relativePath = path.relativeTo(dir).toString()
              // Normalize path separators to forward slash for consistency
              val normalizedPath = relativePath.replace(File.separatorChar, '/')
              writer.println("$normalizedPath=$path")
              fileCount++
            }
        }
    }
  }

  // Create manifest file - always create it even if empty
  if (fileCount == 0) {
    // If no files were added, create a minimal manifest so JAR isn't completely empty
    manifestOutput.writeText("Manifest-Version: 1.0\n")
    // Add manifest to zipper args so it gets included (use the path passed in, not absolutePath)
    argsOutput.appendText("META-INF/MANIFEST.MF=${manifestOutput.path}\n")
  } else {
    // Create empty manifest file so the file exists when zipper runs
    manifestOutput.writeText("")
  }
}
