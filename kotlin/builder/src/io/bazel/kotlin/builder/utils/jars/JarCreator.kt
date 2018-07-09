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

import java.io.*
import java.nio.file.*
import java.nio.file.Paths.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import java.util.jar.Attributes
import java.util.jar.JarOutputStream
import java.util.jar.Manifest

@Suppress("unused")
/**
 * A class for creating Jar files. Allows normalization of Jar entries by setting their timestamp to
 * the DOS epoch. All Jar entries are sorted alphabetically.
 */
class JarCreator(
    path: Path,
    normalize: Boolean = true,
    verbose: Boolean = false
) : JarHelper(path, normalize, verbose) {
    // Map from Jar entry names to files. Use TreeMap so we can establish a canonical order for the
    // entries regardless in what order they get added.
    private val jarEntries = TreeMap<String, Path>()
    private var manifestFile: String? = null
    private var mainClass: String? = null
    private var targetLabel: String? = null
    private var injectingRuleKind: String? = null

    /**
     * Adds an entry to the Jar file, normalizing the name.
     *
     * @param entryName the name of the entry in the Jar file
     * @param path the path of the input for the entry
     * @return true iff a new entry was added
     */
    private fun addEntry(entryName: String, path: Path): Boolean {
        var normalizedEntryName = entryName
        if (normalizedEntryName.startsWith("/")) {
            normalizedEntryName = normalizedEntryName.substring(1)
        } else if (normalizedEntryName.length >= 3
            && Character.isLetter(normalizedEntryName[0])
            && normalizedEntryName[1] == ':'
            && (normalizedEntryName[2] == '\\' || normalizedEntryName[2] == '/')
        ) {
            // Windows absolute path, e.g. "D:\foo" or "e:/blah".
            // Windows paths are case-insensitive, and support both backslashes and forward slashes.
            normalizedEntryName = normalizedEntryName.substring(3)
        } else if (normalizedEntryName.startsWith("./")) {
            normalizedEntryName = normalizedEntryName.substring(2)
        }
        return jarEntries.put(normalizedEntryName, path) == null
    }

    /**
     * Adds an entry to the Jar file, normalizing the name.
     *
     * @param entryName the name of the entry in the Jar file
     * @param fileName the name of the input file for the entry
     * @return true iff a new entry was added
     */
    fun addEntry(entryName: String, fileName: String): Boolean {
        return addEntry(entryName, get(fileName))
    }

    /**
     * Adds the contents of a directory to the Jar file. All files below this directory will be added
     * to the Jar file using the name relative to the directory as the name for the Jar entry.
     *
     * @param directory the directory to add to the jar
     */
    fun addDirectory(directory: Path) {
        if (!Files.exists(directory)) {
            throw IllegalArgumentException("directory does not exist: $directory")
        }
        try {
            Files.walkFileTree(
                directory,
                object : SimpleFileVisitor<Path>() {

                    @Throws(IOException::class)
                    override fun preVisitDirectory(path: Path, attrs: BasicFileAttributes): FileVisitResult {
                        if (path != directory) {
                            // For consistency with legacy behaviour, include entries for directories except for
                            // the root.
                            addEntry(path, /* isDirectory= */ true)
                        }
                        return FileVisitResult.CONTINUE
                    }

                    @Throws(IOException::class)
                    override fun visitFile(path: Path, attrs: BasicFileAttributes): FileVisitResult {
                        addEntry(path, /* isDirectory= */ false)
                        return FileVisitResult.CONTINUE
                    }

                    fun addEntry(path: Path, isDirectory: Boolean) {
                        val sb = StringBuilder()
                        var first = true
                        for (entry in directory.relativize(path)) {
                            if (!first) {
                                // use `/` as the directory separator for jar paths, even on Windows
                                sb.append('/')
                            }
                            sb.append(entry.fileName)
                            first = false
                        }
                        if (isDirectory) {
                            sb.append('/')
                        }
                        jarEntries[sb.toString()] = path
                    }
                })
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }

    }

    /**
     * Adds a collection of entries to the jar, each with a given source path, and with the resulting
     * file in the root of the jar.
     *
     * <pre>
     * some/long/path.foo => (path.foo, some/long/path.foo)
    </pre> *
     */
    fun addRootEntries(entries: Collection<String>) {
        for (entry in entries) {
            val path = get(entry)
            jarEntries[path.fileName.toString()] = path
        }
    }

    /**
     * Sets the main.class entry for the manifest. A value of `null` (the default) will
     * omit the entry.
     *
     * @param mainClass the fully qualified name of the main class
     */
    fun setMainClass(mainClass: String) {
        this.mainClass = mainClass
    }

    fun setJarOwner(targetLabel: String, injectingRuleKind: String) {
        this.targetLabel = targetLabel
        this.injectingRuleKind = injectingRuleKind
    }

    /**
     * Sets filename for the manifest content. If this is set the manifest will be read from this file
     * otherwise the manifest content will get generated on the fly.
     *
     * @param manifestFile the filename of the manifest file.
     */
    fun setManifestFile(manifestFile: String) {
        this.manifestFile = manifestFile
    }

    @Throws(IOException::class)
    private fun manifestContent(): ByteArray {
        if (manifestFile != null) {
            FileInputStream(manifestFile!!).use { `in` -> return manifestContentImpl(Manifest(`in`)) }
        } else {
            return manifestContentImpl(Manifest())
        }
    }

    @Throws(IOException::class)
    private fun manifestContentImpl(manifest: Manifest): ByteArray {
        val attributes = manifest.mainAttributes
        attributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
        val createdBy = Attributes.Name("Created-By")
        if (attributes.getValue(createdBy) == null) {
            attributes[createdBy] = "io.bazel.rules.kotlin"
        }
        if (mainClass != null) {
            attributes[Attributes.Name.MAIN_CLASS] = mainClass
        }
        if (targetLabel != null) {
            attributes[TARGET_LABEL] = targetLabel
        }
        if (injectingRuleKind != null) {
            attributes[INJECTING_RULE_KIND] = injectingRuleKind
        }
        val out = ByteArrayOutputStream()
        manifest.write(out)
        return out.toByteArray()
    }

    /**
     * Executes the creation of the Jar file.
     *
     * @throws IOException if the Jar cannot be written or any of the entries cannot be read.
     */
    @Throws(IOException::class)
    fun execute() {
        Files.newOutputStream(jarPath).use { os ->
            BufferedOutputStream(os).use { bos ->
                JarOutputStream(bos).use { out ->
                    // Create the manifest entry in the Jar file
                    writeManifestEntry(out, manifestContent())
                    for ((key, value) in jarEntries) {
                        out.copyEntry(key, value)
                    }
                }
            }
        }
    }
}