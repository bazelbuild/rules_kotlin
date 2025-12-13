/*
 * Copyright 2025 The Bazel Authors. All rights reserved.
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

import io.bazel.kotlin.builder.KotlinJvmTestBuilder;
import io.bazel.kotlin.model.JvmCompilationTask;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests for incremental compilation directory calculation.
 * Verifies that incremental directories are properly derived from output jar paths
 * and placed inside Bazel's output tree.
 *
 * These tests only verify task configuration, they don't actually run compilation.
 */
@RunWith(JUnit4.class)
public class KotlinBuilderIncrementalDirTest {
    private static final KotlinJvmTestBuilder ctx = new KotlinJvmTestBuilder();

    @Test
    public void testIncrementalBaseDirIsSetWhenIncrementalCompilationEnabled() {
        ctx.resetForNext();
        // Set up task configuration without running compilation
        ctx.setupTask(
                c -> {
                    c.compileKotlin();
                    c.addSource("AClass.kt", "package something;\nclass AClass{}");
                    c.outputJar();
                    c.outputJdeps();
                    c.incrementalCompilation();
                });
        JvmCompilationTask task = ctx.buildTask();

        // Verify incremental base dir is set
        assertThat(task.getDirectories().getIncrementalBaseDir()).isNotEmpty();
        assertThat(task.getDirectories().getIncrementalBaseDir()).contains("_kotlin_incremental");
    }

    @Test
    public void testIncrementalBaseDirIsDerivedFromOutputJar() {
        ctx.resetForNext();
        // Set up task configuration without running compilation
        ctx.setupTask(
                c -> {
                    c.compileKotlin();
                    c.addSource("AClass.kt", "package something;\nclass AClass{}");
                    c.outputJar();
                    c.outputJdeps();
                    c.incrementalCompilation();
                });
        JvmCompilationTask task = ctx.buildTask();

        String incrementalBaseDir = task.getDirectories().getIncrementalBaseDir();
        String outputJar = task.getOutputs().getJar();

        // The incremental base dir should be a sibling directory to the output jar
        String jarDir = outputJar.substring(0, outputJar.lastIndexOf('/'));
        assertThat(incrementalBaseDir).startsWith(jarDir);

        // Should contain the jar name (without extension) in the path
        assertThat(incrementalBaseDir).contains("jar_file");
    }

    @Test
    public void testIncrementalBaseDirNotSetWhenIncrementalCompilationDisabled() {
        ctx.resetForNext();
        // Set up task configuration without running compilation
        ctx.setupTask(
                c -> {
                    c.compileKotlin();
                    c.addSource("AClass.kt", "package something;\nclass AClass{}");
                    c.outputJar();
                    c.outputJdeps();
                    // Don't enable incremental compilation
                });
        JvmCompilationTask task = ctx.buildTask();

        // Verify incremental base dir is empty when not enabled
        assertThat(task.getDirectories().getIncrementalBaseDir()).isEmpty();
    }

    @Test
    public void testIncrementalCompilationEnabledAlsoEnablesBuildToolsApi() {
        ctx.resetForNext();
        // Set up task configuration without running compilation
        ctx.setupTask(
                c -> {
                    c.compileKotlin();
                    c.addSource("AClass.kt", "package something;\nclass AClass{}");
                    c.outputJar();
                    c.outputJdeps();
                    c.incrementalCompilation();
                });
        JvmCompilationTask task = ctx.buildTask();

        // Verify build tools API is enabled when incremental compilation is enabled
        assertThat(task.getInfo().getBuildToolsApi()).isTrue();
        assertThat(task.getInfo().getIncrementalCompilation()).isTrue();
    }
}
