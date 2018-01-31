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

import io.bazel.ruleskotlin.workers.compilers.jvm.Context;
import io.bazel.ruleskotlin.workers.compilers.jvm.Flag;
import io.bazel.ruleskotlin.workers.compilers.jvm.Locations;
import io.bazel.ruleskotlin.workers.compilers.jvm.Meta;
import io.bazel.ruleskotlin.workers.compilers.jvm.utils.KotlinCompilerOutputProcessor;
import io.bazel.ruleskotlin.workers.compilers.jvm.utils.KotlinPreloadedCompilerBuilder;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Either compiles to a jar directly or when performing mixed-mode-compilation compiles to a temp directory first.
 */
public final class KotlinMainCompile implements BuildAction {
    public static final KotlinMainCompile INSTANCE = new KotlinMainCompile(KotlinPreloadedCompilerBuilder.build());

    private static final String
            JAVAC_PATH = Locations.JAVA_HOME.resolveVerified("bin", "javac").toString();

    private static final String
            X_COMPILE_JAVA_FLAG = "-Xcompile-java",
            X_JAVAC_ARGUMENTS_FLAG = "-Xjavac-arguments",
            X_USE_JAVAC_FLAG = "-Xuse-javac";

    /**
     * Default fields that are directly mappable to kotlin compiler args.
     */
    private static final Flag[] COMPILE_MAPPED_FLAGS = new Flag[]{
            Flag.OUTPUT_CLASSJAR,
            Flag.CLASSPATH,
            Flag.KOTLIN_API_VERSION,
            Flag.KOTLIN_LANGUAGE_VERSION,
            Flag.KOTLIN_JVM_TARGET
    };

    private final BiFunction<String[], PrintStream, Integer> compiler;

    private KotlinMainCompile(BiFunction<String[], PrintStream, Integer> compiler) {
        this.compiler = compiler;
    }

    /**
     * Evaluate the compilation context and add Metadata to the ctx if needed.
     *
     * @return The args to pass to the kotlin compile class.
     */
    private static String[] setupCompileContext(Context ctx) {
        List<String> args = new ArrayList<>();
        EnumMap<Flag, String> compileMappedFields = ctx.copyOfArgsContaining(COMPILE_MAPPED_FLAGS);
        String[] sources = Flag.SOURCES.get(ctx).split(":");

        for (String source : sources) {
            if (source.endsWith(".java")) {
                try {
                    // Redirect the kotlin and java compilers to a temp directory.
                    File temporaryClassOutputDirectory = Files.createTempDirectory("kotlinCompile").toFile();
                    Meta.COMPILE_TO_DIRECTORY.bind(ctx, temporaryClassOutputDirectory);
                    compileMappedFields.put(Flag.OUTPUT_CLASSJAR, temporaryClassOutputDirectory.toString());
                    Collections.addAll(args,
                            X_COMPILE_JAVA_FLAG,
                            X_USE_JAVAC_FLAG + "=" + JAVAC_PATH,
                            X_JAVAC_ARGUMENTS_FLAG + "=-d=" + temporaryClassOutputDirectory.toString());
                    break;
                } catch (IOException e) {
                    throw new RuntimeException("could not create temp directory for kotlin compile operation", e);
                }
            }
        }
        compileMappedFields.forEach((field, arg) -> Collections.addAll(args, field.kotlinFlag, arg));
        Collections.addAll(args, sources);
        return args.toArray(new String[args.size()]);
    }

    @Override
    public Integer apply(Context ctx) {
        KotlinCompilerOutputProcessor outputProcessor = KotlinCompilerOutputProcessor.delegatingTo(System.out);
        try {
            Integer exitCode = compiler.apply(setupCompileContext(ctx), outputProcessor.getCollector());
            if (exitCode < 2) {
                // 1 is a standard compilation error
                // 2 is an internal error
                // 3 is the script execution error
                return exitCode;
            } else {
                throw new RuntimeException("KotlinMainCompile returned terminal error code: " + exitCode);
            }
        } finally {
            outputProcessor.process();
        }
    }
}
