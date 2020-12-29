/*
 * Copyright 2020 The Bazel Authors. All rights reserved.
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
package io.bazel.kotlin.builder.tasks.jvm;

import io.bazel.kotlin.builder.Deps;
import io.bazel.kotlin.builder.DirectoryType;
import io.bazel.kotlin.builder.KotlinJvmTestBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Consumer;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public class KotlinBuilderJvmBasicTest {
    private static final KotlinJvmTestBuilder ctx = new KotlinJvmTestBuilder();

    private static final Consumer<KotlinJvmTestBuilder.TaskBuilder> SETUP_NORMALIZATION_TEST_SOURCES =
            ctx -> {
                ctx.compileKotlin();
                ctx.addSource("AClass.kt", "package something;\n" + "class AClass{}");
                ctx.addSource("BClass.kt", "package something;\n" + "class BClass{}");
                ctx.outputJar();
                ctx.outputJdeps();
            };

    private static String hashDep(String path) {
        try {
            //noinspection UnstableApiUsage
            return com.google.common.hash.Hashing.sha256()
                    .hashBytes(Files.readAllBytes(Paths.get(path)))
                    .toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    public void testSimpleMixedModeCompile() {
        ctx.runCompileTask(
                c -> {
          c.compileKotlin();
          c.compileJava();
          c.addSource(
              "AClass.kt",
              "package something;" + "class AClass{}"
          );
          c.addSource(
              "AnotherClass.java",
              "package something;",
              "",
              "class AnotherClass{}"
          );
          c.outputJar();
          c.outputJdeps();
          c.outputJavaJdeps();
        });
        ctx.assertFilesExist(DirectoryType.CLASSES, "something/AClass.class");
        ctx.assertFilesExist(DirectoryType.JAVA_CLASSES, "something/AnotherClass.class");
    }

    @Test
    public void testGeneratesJDeps() {
    ctx.runCompileTask(
        c -> {
          c.addSource("AClass.kt", "package something;" + "class AClass{}");
          c.addSource("AnotherClass.java", "package something;", "", "class AnotherClass{}");
          // declaring outputJdeps also asserts existance after compile.
          c.outputJar().outputJdeps().outputJavaJdeps().compileKotlin().compileJava();
        });
  }

    @Test
    public void testKotlinErrorRendering() {
        ctx.runFailingCompileTaskAndValidateOutput(
                () ->
                        ctx.runCompileTask(
                                c -> {
                                    c.compileKotlin();
                                    c.addSource("AClass.kt", "package something;" + "class AClass{");
                                    c.outputJar();
                                    c.outputJdeps();
                                }),
                lines -> assertThat(lines.get(0)).startsWith(ctx.toPlatform("sources/AClass")));
    }

    @Test
    public void testCompiledJarIsNormalized() {
        Deps.Dep previous = ctx.runCompileTask(SETUP_NORMALIZATION_TEST_SOURCES);
        Deps.Dep recompiled =
                ctx.runCompileTask(ctx -> ctx.setLabel(previous.label()), SETUP_NORMALIZATION_TEST_SOURCES);
        assertThat(previous.label()).isEqualTo(recompiled.label());
        assertThat(hashDep(previous.singleCompileJar()))
                .isEqualTo(hashDep(recompiled.singleCompileJar()));
    }
}
