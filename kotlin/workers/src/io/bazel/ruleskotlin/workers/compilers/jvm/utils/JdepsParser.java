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
package io.bazel.ruleskotlin.workers.compilers.jvm.utils;

import com.google.devtools.build.lib.view.proto.Deps;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class JdepsParser {
    private final String filename;
    private final String packageSuffix;
    private final Predicate<String> isImplicit;

    private final Map<String, Deps.Dependency.Builder> depMap = new HashMap<>();
    private final Set<String> packages = new HashSet<>();

    private JdepsParser(String filename, Predicate<String> isImplicit) {
        this.filename = filename;
        this.packageSuffix = " (" + filename + ")";
        this.isImplicit = isImplicit;
    }

    private void consumeJarLine(String classJarPath, Deps.Dependency.Kind kind) {
        Path path = Paths.get(classJarPath);

        // ignore absolute files, -- jdk jar paths etc.
        // only process jar files
        if (!(path.isAbsolute() || !classJarPath.endsWith(".jar"))) {
            Deps.Dependency.Builder entry = depMap.computeIfAbsent(classJarPath, (key) -> {
                Deps.Dependency.Builder depBuilder = Deps.Dependency.newBuilder();
                depBuilder.setPath(classJarPath);
                depBuilder.setKind(kind);

                if (isImplicit.test(classJarPath)) {
                    depBuilder.setKind(Deps.Dependency.Kind.IMPLICIT);
                }
                return depBuilder;
            });

            // don't flip an implicit dep.
            if (entry.getKind() != Deps.Dependency.Kind.IMPLICIT) {
                entry.setKind(kind);
            }
        }
    }

    private enum Mode {
        COLLECT_DEPS,
        DETERMINE_JDK,
        COLLECT_PACKAGES_JDK8,
        COLLECT_PACKAGES_JDK9
    }

    private Mode mode = Mode.COLLECT_DEPS;

    // maybe simplify this by tokenizing on whitespace and arrows.
    private void processLine(String line) {
        String trimmedLine = line.trim();
        switch (mode) {
            case COLLECT_DEPS:
                if (!line.startsWith(" ")) {
                    String[] parts = line.split(" -> ");
                    if (parts.length == 2) {
                        if (!parts[0].equals(filename)) {
                            throw new RuntimeException("should only get dependencies for dep: " + filename);
                        }
                        consumeJarLine(parts[1], Deps.Dependency.Kind.EXPLICIT);
                    }
                } else {
                    mode = Mode.DETERMINE_JDK;
                    processLine(line);
                }
                break;
            case DETERMINE_JDK:
                mode = Mode.COLLECT_PACKAGES_JDK8;
                if (!line.endsWith(packageSuffix)) {
                    mode = Mode.COLLECT_PACKAGES_JDK9;
                }
                processLine(line);
                break;
            case COLLECT_PACKAGES_JDK8:
                if (trimmedLine.endsWith(packageSuffix)) {
                    packages.add(trimmedLine.substring(0, trimmedLine.length() - packageSuffix.length()));
                } else if (trimmedLine.startsWith("-> ")) {
                    // ignore package detail lines, in the jdk8 format these start with arrows.
                } else throw new RuntimeException("unexpected line while collecting packages: " + line);
                break;
            case COLLECT_PACKAGES_JDK9:
                String[] pkg = trimmedLine.split("\\s+");
                packages.add(pkg[0]);
                break;
        }
    }


    public static Predicate<String> pathSuffixMatchingPredicate(Path directory, String... jars) {
        String[] suffixes = Stream.of(jars).map(lib -> directory.resolve(lib).toString()).toArray(String[]::new);
        return (jar) -> {
            for (String implicitJarsEnding : suffixes) {
                if (jar.endsWith(implicitJarsEnding)) {
                    return true;
                }
            }
            return false;
        };
    }

    public static Deps.Dependencies parse(String label, String classJar, String classPath, Stream<String> jdepLines, Predicate<String> isImplicit) {
        String filename = Paths.get(classJar).getFileName().toString();
        JdepsParser jdepsParser = new JdepsParser(filename, isImplicit);
        Stream.of(classPath.split(":")).forEach(x -> jdepsParser.consumeJarLine(x, Deps.Dependency.Kind.UNUSED));
        jdepLines.forEach(jdepsParser::processLine);

        Deps.Dependencies.Builder rootBuilder = Deps.Dependencies.newBuilder();
        rootBuilder.setSuccess(false);
        rootBuilder.setRuleLabel(label);

        rootBuilder.addAllContainedPackage(jdepsParser.packages);
        jdepsParser.depMap.values().forEach(b -> rootBuilder.addDependency(b.build()));

        rootBuilder.setSuccess(true);
        return rootBuilder.build();
    }
}