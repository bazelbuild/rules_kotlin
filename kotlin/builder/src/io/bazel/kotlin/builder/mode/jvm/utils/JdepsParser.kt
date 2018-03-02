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
package io.bazel.kotlin.builder.mode.jvm.utils

import com.google.devtools.build.lib.view.proto.Deps
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.function.Predicate
import java.util.stream.Stream

class JdepsParser private constructor(private val filename: String, private val isImplicit: Predicate<String>) {
    private val packageSuffix: String = " ($filename)"

    private val depMap = HashMap<String, Deps.Dependency.Builder>()
    private val packages = HashSet<String>()

    private var mode = Mode.COLLECT_DEPS

    private fun addJar(classJarPath: String, kind: Deps.Dependency.Kind) {
        val path = Paths.get(classJarPath)

        // ignore absolute files, -- jdk jar paths etc.
        // only process jar files
        if (!(path.isAbsolute || !classJarPath.endsWith(".jar"))) {
            val entry = depMap.computeIfAbsent(classJarPath) {
                val depBuilder = Deps.Dependency.newBuilder()
                depBuilder.path = classJarPath
                depBuilder.kind = kind

                if (isImplicit.test(classJarPath)) {
                    depBuilder.kind = Deps.Dependency.Kind.IMPLICIT
                }
                depBuilder
            }

            // don't flip an implicit dep.
            if (entry.kind != Deps.Dependency.Kind.IMPLICIT) {
                entry.kind = kind
            }
        }
    }

    private enum class Mode {
        COLLECT_DEPS,
        DETERMINE_JDK,
        COLLECT_PACKAGES_JDK8,
        COLLECT_PACKAGES_JDK9
    }

    // maybe simplify this by tokenizing on whitespace and arrows.
    private fun processLine(line: String) {
        val trimmedLine = line.trim { it <= ' ' }
        when (mode) {
            Mode.COLLECT_DEPS -> if (!line.startsWith(" ")) {
                val parts = line.split(" -> ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (parts.size == 2) {
                    if (parts[0] != filename) {
                        throw RuntimeException("should only get dependencies for dep: $filename")
                    }
                    addJar(parts[1], Deps.Dependency.Kind.EXPLICIT)
                }
            } else {
                mode = Mode.DETERMINE_JDK
                processLine(line)
            }
            Mode.DETERMINE_JDK -> {
                mode = Mode.COLLECT_PACKAGES_JDK8
                if (!line.endsWith(packageSuffix)) {
                    mode = Mode.COLLECT_PACKAGES_JDK9
                }
                processLine(line)
            }
            Mode.COLLECT_PACKAGES_JDK8 -> when {
                trimmedLine.endsWith(packageSuffix) -> packages.add(trimmedLine.substring(0, trimmedLine.length - packageSuffix.length))
                trimmedLine.startsWith("-> ") -> {
                    // ignore package detail lines, in the jdk8 format these start with arrows.
                }
                else -> throw RuntimeException("unexpected line while collecting packages: $line")
            }
            Mode.COLLECT_PACKAGES_JDK9 -> {
                val pkg = trimmedLine.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                packages.add(pkg[0])
            }
        }
    }

    companion object {
        fun pathSuffixMatchingPredicate(directory: Path, vararg jars: String): Predicate<String> {
            val suffixes = jars.map { directory.resolve(it).toString() }
            return Predicate { jar ->
                for (implicitJarsEnding in suffixes) {
                    if (jar.endsWith(implicitJarsEnding)) {
                        return@Predicate true
                    }
                }
                false
            }
        }

        fun parse(label: String, classJar: String, classPath: String, jdepLines: Stream<String>, isImplicit: Predicate<String>): Deps.Dependencies {
            val filename = Paths.get(classJar).fileName.toString()
            val jdepsParser = JdepsParser(filename, isImplicit)

            Stream.of(*classPath.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()).forEach {
                jdepsParser.addJar(it, Deps.Dependency.Kind.UNUSED)
            }

            jdepLines.forEach { jdepsParser.processLine(it) }

            val rootBuilder = Deps.Dependencies.newBuilder()
            rootBuilder.success = false
            rootBuilder.ruleLabel = label

            rootBuilder.addAllContainedPackage(jdepsParser.packages)
            jdepsParser.depMap.values.forEach { b -> rootBuilder.addDependency(b.build()) }

            rootBuilder.success = true
            return rootBuilder.build()
        }
    }
}