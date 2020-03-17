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
                ctx.addSource("AClass.kt", "package something;\n" + "class AClass{}");
                ctx.addSource("BClass.kt", "package something;\n" + "class BClass{}");
                ctx.outputJar().outputSrcJar();
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
                    c.addSource("AClass.kt", "package something;" + "class AClass{}");
                    c.addSource("AnotherClass.java", "package something;", "", "class AnotherClass{}");
                    c.outputJar().outputSrcJar();
                });
        ctx.assertFilesExist(
                DirectoryType.CLASSES, "something/AClass.class", "something/AnotherClass.class");
    }

    @Test
    public void testGeneratesJDeps() {
        ctx.runCompileTask(
                c -> {
                    c.addSource("AClass.kt", "package something;" + "class AClass{}");
                    c.addSource("AnotherClass.java", "package something;", "", "class AnotherClass{}");
                    // declaring outputJdeps also asserts existance after compile.
                    c.outputJar().outputSrcJar().outputJdeps();
                });
    }

    @Test
    public void testKotlinErrorRendering() {
        ctx.runFailingCompileTaskAndValidateOutput(
                () ->
                        ctx.runCompileTask(
                                c -> {
                                    c.addSource("AClass.kt", "package something;" + "class AClass{");
                                    c.outputJar().outputSrcJar();
                                }),
                lines -> assertThat(lines.get(0)).startsWith(ctx.toPlatform("sources/AClass")));
    }

    @Test
    public void testMixedBiReferences() {
        ctx.runCompileTask(
                ctx -> {
                    ctx.addSource(
                            "AClass.java",
                            "package a;",
                            "",
                            "import b.BClass;",
                            "",
                            "public class AClass {",
                            "  static BClass b = new BClass();",
                            "}");
                    ctx.addSource(
                            "BClass.kt",
                            "package b",
                            "",
                            "import a.AClass",
                            "",
                            "class BClass() {",
                            "  val a = AClass()",
                            "}");
                    ctx.outputSrcJar()
                            .outputJar();
                });
        ctx.assertFilesExist(DirectoryType.CLASSES, "a/AClass.class", "b/BClass.class");
    }

    @Test
    public void testCompileSingleJavaFile() {
        ctx.runCompileTask(
                (ctx) -> {
                    ctx.addSource("AnotherClass.java", "package something;", "", "class AnotherClass{}");
                    ctx.outputSrcJar()
                            .outputJar();
                });
    }

    @Test
    public void testJavaErrorRendering() {
        ctx.runFailingCompileTaskAndValidateOutput(
                () ->
                        ctx.runCompileTask(
                                c -> {
                                    c.addSource("AClass.kt", "package something;" + "class AClass{}");
                                    c.addSource("AnotherClass.java", "package something;", "", "class AnotherClass{");
                                    c.outputJar().outputSrcJar();
                                }),
                lines -> assertThat(lines.get(0)).startsWith(ctx.toPlatform("sources/AnotherClass")));
    }

    @Test
    public void testCompiledJarIsNormalized() {
        Deps.Dep previous = ctx.runCompileTask(SETUP_NORMALIZATION_TEST_SOURCES);
        Deps.Dep recompiled =
                ctx.runCompileTask(ctx -> ctx.setLabel(previous.label()), SETUP_NORMALIZATION_TEST_SOURCES);
        assertThat(previous.label()).isEqualTo(recompiled.label());
        assertThat(hashDep(previous.singleCompileJar()))
                .isEqualTo(hashDep(recompiled.singleCompileJar()));
        assertThat(previous.sourceJar()).isNotEmpty();
        assertThat(hashDep(previous.sourceJar())).isEqualTo(hashDep(recompiled.sourceJar()));
    }

    @Test
    public void testSourceJarIsNormalized() {
        Deps.Dep previous = ctx.runCompileTask(SETUP_NORMALIZATION_TEST_SOURCES);
        Deps.Dep recompiled =
                ctx.runCompileTask(ctx -> ctx.setLabel(previous.label()), SETUP_NORMALIZATION_TEST_SOURCES);
        assertThat(previous.sourceJar()).isNotEmpty();
        assertThat(hashDep(previous.sourceJar())).isEqualTo(hashDep(recompiled.sourceJar()));
    }
}
