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
package io.bazel.ruleskotlin.workers;

import io.bazel.ruleskotlin.workers.compilers.jvm.Locations;
import org.jetbrains.kotlin.preloading.ClassPreloadingUtils;
import org.jetbrains.kotlin.preloading.Preloader;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.function.BiFunction;

public final class KotlinToolchain {
    private static final Object[] NO_ARGS = new Object[]{};
    private final ClassLoader classLoader;

    public KotlinToolchain() throws IOException {
        this.classLoader = ClassPreloadingUtils.preloadClasses(
                Locations.KOTLIN_REPO.verifiedRelativeFiles(
                        Paths.get("lib", "kotlin-compiler.jar")
                ),
                Preloader.DEFAULT_CLASS_NUMBER_ESTIMATE,
                Thread.currentThread().getContextClassLoader(),
                null
        );
    }

    public interface KotlinCompiler extends BiFunction<String[], PrintStream, Integer> {
    }

    /**
     * Load the Kotlin compiler and the javac tools.jar into a Preloading classLoader. The Kotlin compiler is invoked reflectively to eventually allow
     * toolchain replacement.
     */
    public KotlinCompiler kotlinCompiler() {
        try {
            Class<?> compilerClass = classLoader.loadClass("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler");
            Class<?> exitCodeClass = classLoader.loadClass("org.jetbrains.kotlin.cli.common.ExitCode");

            Object compiler = compilerClass.newInstance();
            Method execMethod = compilerClass.getMethod("exec", PrintStream.class, String[].class);
            Method getCodeMethod = exitCodeClass.getMethod("getCode");

            return (args, stream) -> {
                final Object exitCodeInstance;
                try {
                    exitCodeInstance = execMethod.invoke(compiler, stream, args);
                    return (Integer) getCodeMethod.invoke(exitCodeInstance, NO_ARGS);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
