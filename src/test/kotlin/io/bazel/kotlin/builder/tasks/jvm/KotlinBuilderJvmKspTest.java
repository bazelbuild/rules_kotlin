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

import io.bazel.kotlin.builder.Deps.AnnotationProcessor;
import io.bazel.kotlin.builder.Deps.Dep;
import io.bazel.kotlin.builder.DirectoryType;
import io.bazel.kotlin.builder.KotlinJvmTestBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.bazel.kotlin.builder.KotlinJvmTestBuilder.KOTLIN_ANNOTATIONS;
import static io.bazel.kotlin.builder.KotlinJvmTestBuilder.KOTLIN_STDLIB;

@RunWith(JUnit4.class)
public class KotlinBuilderJvmKspTest {

    private static final Dep MOSHI =
            Dep.importJar(
                    "moshi",
                    System.getProperty("moshi")
                            .replaceFirst("external" + File.separator, ""));
    private static final Dep MOSHI_KOTLIN =
            Dep.importJar(
                    "moshi_kotlin",
                    System.getProperty("moshi_kotlin")
                            .replaceFirst("external" + File.separator, ""));
    private static final Dep MOSHI_KOTLIN_CODEGEN =
            Dep.importJar(
                    "moshi_kotlin_codegen",
                    System.getProperty("moshi_kotlin_codegen")
                            .replaceFirst("external" + File.separator, ""));
    private static final Dep KOTLIN_POET =
            Dep.importJar(
                    "kotlin_poet",
                    System.getProperty("kotlin_poet")
                            .replaceFirst("external" + File.separator, ""));
    private static final Dep ASM =
            Dep.importJar(
                    "asm",
                    System.getProperty("asm")
                            .replaceFirst("external" + File.separator, ""));
    private static final AnnotationProcessor MOSHI_ANNOTATION_PROCESSOR =
            AnnotationProcessor.builder()
                    .processClass("com.squareup.moshi.kotlin.codegen.ksp.JsonClassSymbolProcessor")
                    .processorPath(
                            Dep.classpathOf(MOSHI, MOSHI_KOTLIN, MOSHI_KOTLIN_CODEGEN, KOTLIN_POET,
                                    ASM, KOTLIN_ANNOTATIONS, KOTLIN_STDLIB)
                                    .collect(Collectors.toSet()))
                    .build();

    private static final KotlinJvmTestBuilder ctx = new KotlinJvmTestBuilder();

    private static final Consumer<KotlinJvmTestBuilder.TaskBuilder> ADD_MOSHI_PLUGIN =
            (c) -> {
                c.addAnnotationProcessors(MOSHI_ANNOTATION_PROCESSOR);
                c.addDirectDependencies(MOSHI, KOTLIN_POET, ASM, MOSHI_KOTLIN, KOTLIN_ANNOTATIONS, KOTLIN_STDLIB);
            };

    @Test
    public void testKspDoesRunAnnotationProcessorWithCompile() {
        ctx.runCompileTask(
                ADD_MOSHI_PLUGIN,
                c -> {
                    c.compileKotlin();
                    c.addSource(
                            "TestKtValue.kt",
                            "package moshi\n"
                                    + "\n"
                                    + "import com.squareup.moshi.JsonClass\n"
                                    + "\n"
                                    + "@JsonClass(generateAdapter = true)\n"
                                    + "data class TestKtValue(\n"
                                    + "     val id: String\n"
                                    + ")");
                    c.addSource(
                            "TestKtValueUsingAdapter.kt",
                            "package moshi\n"
                                    + "\n"
                                    + "class TestKtValueUsingAdapter(val adapter: TestKtValueJsonAdapter)");
                    c.outputJar();
                    c.generatedSourceJar();
                    c.generatedKspSourceJar();
                    c.incrementalData();
                }
        );

        ctx.assertFilesExist(
                DirectoryType.CLASSES,
                "moshi/TestKtValue.class", "moshi/TestKtValueJsonAdapter.class", "moshi/TestKtValueUsingAdapter.class");
        ctx.assertFilesExist(DirectoryType.SOURCE_GEN,
                "moshi/TestKtValueJsonAdapter.kt", "META-INF/proguard/moshi-moshi.TestKtValue.pro");
    }
}
