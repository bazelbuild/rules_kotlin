package io.bazel.kotlin.builder.tasks.js;

import io.bazel.kotlin.builder.Deps.Dep;
import io.bazel.kotlin.builder.KotlinJsTestBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public class KotlinBuilderJsTest {
  private static final KotlinJsTestBuilder builder = new KotlinJsTestBuilder();

  private Dep stdLib = Dep.fromLabel("@com_github_jetbrains_kotlin//:kotlin-stdlib-js");

  @Test
  public void testSimpleJsCompile() {
    builder.runCompilationTask(
        it -> {
          it.addDependency(stdLib.singleCompileJar());
          it.addArg("-Xreport-output-files");
          it.addArg("-Xreport-perf");
          it.addArg("-verbose");
          it.addSource(
              "AClass.kt",
              "package something",
              "class AClass{",
              "  fun foo() : String {",
              "    return \"bar\"",
              "  }",
              "}");
        },
        lines -> {
          lines.forEach(System.out::println);
        });
  }

  @Test
  public void testFuncJsCompile() {

    builder.runCompilationTask(
        it -> {
          it.addDependency(stdLib.singleCompileJar());
          it.addArg("-Xreport-output-files");
          it.addArg("-Xreport-perf");
          it.addArg("-verbose");
          it.addSource(
              "auth/Auth.kt",
              "package express.something",
              "fun isAuthenticated(user: String): Boolean {",
                  "    return user != \"bob\"",
                  "}");
        },
        lines -> {
          lines.forEach(System.out::println);
        });
  }

  @Test
  public void testJsErrorRendering() {
    builder.runFailingCompileTaskAndValidateOutput(
        () ->
            builder.runCompilationTask(
                it -> {
                    it.addSource("AClass.kt", "package something", "class AClass{");
                    it.addDependency(stdLib.singleCompileJar());
                }),
        lines ->
            assertThat(lines)
                .contains("sources/AClass.kt:2:14: error: missing '}"));
  }
}
