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

import io.bazel.ruleskotlin.workers.*;
import io.bazel.ruleskotlin.workers.compilers.jvm.Metas;
import io.bazel.ruleskotlin.workers.compilers.jvm.utils.KotlinCompilerOutputProcessor;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Either compiles to a jar directly or when performing mixed-mode-compilation compiles to a temp directory first.
 * <p>
 * Mixed-Mode:
 * <p>
 * The Kotlin compiler is not suited for javac compilation as of 1.2.21. The errors are not conveyed directly and would need to be preprocessed, also javac
 * invocations Configured via Kotlin use eager analysis in some corner cases this can result in classpath exceptions from the Java Compiler..
 */
public final class KotlinMainCompile implements BuildAction {
    private final KotlinToolchain.KotlinCompiler kotlinCompiler;

    public KotlinMainCompile(KotlinToolchain toolchains) {
        this.kotlinCompiler = toolchains.kotlinCompiler();
    }

    /**
     * Default fields that are directly mappable to kotlin compiler args.
     */
    private static final Flags[] COMPILE_MAPPED_FLAGS = new Flags[]{
            Flags.CLASSPATH,
            Flags.KOTLIN_API_VERSION,
            Flags.KOTLIN_LANGUAGE_VERSION,
            Flags.KOTLIN_JVM_TARGET
    };

    /**
     * Evaluate the compilation context and add Metadata to the ctx if needed.
     *
     * @return The args to pass to the kotlin compile class.
     */
    private static String[] setupCompileContext(Context ctx) {
        List<String> args = new ArrayList<>();
        Collections.addAll(args, "-d", Metas.CLASSES_DIRECTORY.mustGet(ctx).toString());
        ctx.of(COMPILE_MAPPED_FLAGS).forEach((field, arg) -> Collections.addAll(args, field.kotlinFlag, arg));
        args.addAll(Metas.ALL_SOURCES.mustGet(ctx));
        return args.toArray(new String[args.size()]);
    }

    @Override
    public Integer apply(Context ctx) {
        KotlinCompilerOutputProcessor outputProcessor;
        outputProcessor = new KotlinCompilerOutputProcessor.ForKotlinC(System.out);

        final Integer exitCode = kotlinCompiler.apply(setupCompileContext(ctx), outputProcessor.getCollector());
        if (exitCode < 2) {
            // 1 is a standard compilation error
            // 2 is an internal error
            // 3 is the script execution error

            // give javac a chance to process the java sources.
            Metas.KOTLINC_RESULT.bind(ctx, CompileResult.deferred(exitCode, (c) -> {
                outputProcessor.process();
                return exitCode;
            }));
            return 0;
        } else {
            outputProcessor.process();
            throw new RuntimeException("KotlinMainCompile returned terminal error code: " + exitCode);
        }
    }
}
