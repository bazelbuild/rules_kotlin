package io.bazel.kotlin.builder.tasks.js;

import io.bazel.kotlin.builder.DaggerKotlinBuilderComponent;
import io.bazel.kotlin.builder.KotlinBuilderComponent;
import io.bazel.kotlin.builder.KotlinBuilderJsTestTask;
import io.bazel.kotlin.builder.toolchain.KotlinToolchain;
import io.bazel.kotlin.builder.utils.CompilationTaskContext;
import io.bazel.kotlin.model.JsCompilationTask;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public class KotlinBuilderJsTest {
  @Rule public KotlinBuilderJsTestTask ctx = new KotlinBuilderJsTestTask();

  private static final KotlinBuilderComponent component =
      DaggerKotlinBuilderComponent.builder().toolchain(KotlinToolchain.createToolchain()).build();

  @Test
  public void testSimpleJsCompile() {
    ctx.addSource("AClass.kt", "package something", "class AClass{}");
    ctx.runCompileTask(this::jsCompilationTask);
  }

  @Test
  public void testJsErrorRendering() {
    ctx.addSource("AClass.kt", "package something", "class AClass{");
    ctx.runFailingCompileTaskAndValidateOutput(
        this::jsCompilationTask, lines -> assertThat(lines.get(0)).startsWith("sources/AClass.kt"));
  }

  private void jsCompilationTask(CompilationTaskContext taskContext, JsCompilationTask task) {
    component.jsTaskExecutor().execute(taskContext, task);
    String jsFile = task.getOutputs().getJs();
    ctx.assertFilesExist(
        jsFile,
        jsFile + ".map",
        jsFile.substring(0, jsFile.length() - 3) + ".meta.js",
        task.getOutputs().getJar(),
        task.getOutputs().getSrcjar());
  }
}
