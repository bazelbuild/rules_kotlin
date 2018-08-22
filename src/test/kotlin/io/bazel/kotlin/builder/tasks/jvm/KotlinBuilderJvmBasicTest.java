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

import io.bazel.kotlin.builder.KotlinBuilderJvmTestTask;
import io.bazel.kotlin.builder.KotlinBuilderResource.DirectoryType;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public class KotlinBuilderJvmBasicTest {
  @Rule public KotlinBuilderJvmTestTask ctx = new KotlinBuilderJvmTestTask();

  @Test
  public void testSimpleMixedModeCompile() {
    ctx.addSource("AClass.kt", "package something;" + "class AClass{}");
    ctx.addSource("AnotherClass.java", "package something;", "", "class AnotherClass{}");
    ctx.runCompileTask();
    ctx.assertFilesExist(
        DirectoryType.CLASSES, "something/AClass.class", "something/AnotherClass.class");
  }

  @Test
  public void testMixedBiReferences() {
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
    ctx.runCompileTask();
    ctx.assertFilesExist(DirectoryType.CLASSES, "a/AClass.class", "b/BClass.class");
  }

  @Test
  public void testKotlinErrorRendering() {
    ctx.addSource("AClass.kt", "package something;" + "class AClass{");
    ctx.runFailingCompileTaskAndValidateOutput(
        ctx::runCompileTask,
        lines -> assertThat(lines.get(0)).startsWith(ctx.toPlatform("sources/AClass")));
  }

  @Test
  public void testJavaErrorRendering() {
    ctx.addSource("AClass.kt", "package something;" + "class AClass{}");
    ctx.addSource("AnotherClass.java", "package something;", "", "class AnotherClass{");
    ctx.runFailingCompileTaskAndValidateOutput(
        ctx::runCompileTask,
        lines -> assertThat(lines.get(0)).startsWith(ctx.toPlatform("sources/AnotherClass")));
  }

  @Test
  @Ignore("The Kotlin compiler expects a single kotlin file at least.")
  public void testCompileSingleJavaFile() {
    ctx.runCompileTask(
        (c) -> c.addSource("AnotherClass.java", "package something;", "", "class AnotherClass{}"));
  }
}
