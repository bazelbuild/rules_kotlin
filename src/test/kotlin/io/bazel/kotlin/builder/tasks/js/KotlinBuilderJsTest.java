package io.bazel.kotlin.builder.tasks.js;

import io.bazel.kotlin.builder.KotlinBuilderJsTestTask;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public class KotlinBuilderJsTest {
  @Rule public KotlinBuilderJsTestTask ctx = new KotlinBuilderJsTestTask();

  @Test
  public void testSimpleJsCompile() {
    ctx.addSource("AClass.kt", "package something", "class AClass{}");
    ctx.runCompilationTask();
  }

  @Test
  public void testJsErrorRendering() {
    ctx.addSource("AClass.kt", "package something", "class AClass{");
    ctx.runFailingCompileTaskAndValidateOutput(
        ctx::runCompilationTask,
        lines -> assertThat(lines.get(0)).startsWith(ctx.toPlatform("sources/AClass.kt")));
  }
}
