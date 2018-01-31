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
package io.bazel.ruleskotlin.workers.compilers.jvm.actions;

import com.google.devtools.build.lib.view.proto.Deps;
import io.bazel.ruleskotlin.workers.compilers.jvm.Context;
import io.bazel.ruleskotlin.workers.compilers.jvm.Locations;
import io.bazel.ruleskotlin.workers.compilers.jvm.utils.JdepsParser;
import io.bazel.ruleskotlin.workers.compilers.jvm.utils.Utils;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Predicate;

import static io.bazel.ruleskotlin.workers.compilers.jvm.Flag.*;

public final class GenerateJdepsFile implements BuildAction {
    private static final String JDEPS_PATH = Locations.JAVA_HOME.resolveVerified("bin", "jdeps").toString();

    private static final Predicate<String> IS_KOTLIN_IMPLICIT = JdepsParser.pathSuffixMatchingPredicate(
            Paths.get("external", "com_github_jetbrains_kotlin", "lib"),
            "kotlin-stdlib.jar",
            "kotlin-stdlib-jdk7.jar",
            "kotlin-stdlib-jdk8.jar");

    public static final GenerateJdepsFile INSTANCE = new GenerateJdepsFile();

    private GenerateJdepsFile() {
    }

    @Override
    public Integer apply(Context ctx) {
        final String
                classJar = OUTPUT_CLASSJAR.get(ctx),
                classPath = CLASSPATH.get(ctx),
                output = OUTPUT_JDEPS.get(ctx);
        Deps.Dependencies jdepsContent;
        try {
            List<String> jdepLines = Utils.waitForOutput(new String[]{JDEPS_PATH, "-cp", classPath, classJar}, System.err);
            jdepsContent = JdepsParser.parse(
                    LABEL.get(ctx),
                    classJar,
                    classPath,
                    jdepLines.stream(),
                    IS_KOTLIN_IMPLICIT
            );
        } catch (Exception e) {
            throw new RuntimeException("error reading or parsing jdeps file", Utils.getRootCause(e));
        }

        try {
            Path outputPath = Paths.get(output);
            Files.deleteIfExists(outputPath);
            try (FileOutputStream fileOutputStream = new FileOutputStream(Files.createFile(outputPath).toFile())) {
                jdepsContent.writeTo(fileOutputStream);
            }
        } catch (Exception e) {
            throw new RuntimeException("error writing out jdeps file", Utils.getRootCause(e));
        }

        return 0;
    }
}
