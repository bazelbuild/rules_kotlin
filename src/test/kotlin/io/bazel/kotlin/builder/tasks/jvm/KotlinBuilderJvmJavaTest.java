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

import io.bazel.kotlin.builder.DirectoryType;
import io.bazel.kotlin.builder.KotlinJvmTestBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public class KotlinBuilderJvmJavaTest {
  private static final KotlinJvmTestBuilder ctx = new KotlinJvmTestBuilder();

  @Test
  public void testSimpleMixedModeCompile() {
    ctx.runCompileTask(
        c -> {
          c.compileJava();
          c.compileKotlin();
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
          c.compileJava();
          c.compileKotlin();
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
          // declaring outputJdeps also asserts existence after compile.
          c.outputJar();
        });
  }

  @Test
  public void testMixedBiReferences() {
    ctx.runCompileTask(
        ctx -> {
          ctx.compileJava();
          ctx.compileKotlin();
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
          ctx.outputJar();
          ctx.outputJdeps();
          ctx.outputJavaJdeps();
          ctx.compileJava();
          ctx.compileKotlin();
        });
    ctx.assertFilesExist(DirectoryType.JAVA_CLASSES, "a/AClass.class");
    ctx.assertFilesExist(DirectoryType.CLASSES, "b/BClass.class");
  }

  @Test
  public void testCompileSingleJavaFile() {
    ctx.runCompileTask(
        (ctx) -> {
          ctx.compileJava();
          ctx.addSource("AnotherClass.java", "package something;", "", "class AnotherClass{}");
          ctx.outputJar();
        });
  }

  @Test
  public void testJavaErrorRendering() {
    ctx.runFailingCompileTaskAndValidateOutput(
        () ->
            ctx.runCompileTask(
                c -> {
                  c.compileJava();
                  c.addSource("AClass.kt", "package something;" + "class AClass{}");
                  c.addSource("AnotherClass.java", "package something;", "", "class AnotherClass{");
                  c.outputJar().outputSrcJar();
                }),
        lines -> assertThat(lines.get(0)).startsWith(ctx.toPlatform("sources/AnotherClass")));
  }
}
