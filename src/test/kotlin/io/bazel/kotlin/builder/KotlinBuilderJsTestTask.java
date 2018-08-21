package io.bazel.kotlin.builder;

import io.bazel.kotlin.model.CompilationTaskInfo;
import io.bazel.kotlin.model.JsCompilationTask;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public final class KotlinBuilderJsTestTask extends KotlinBuilderResource<JsCompilationTask> {
  private static final List<String> PASSTHROUGH_FLAGS =
      Arrays.asList("-source-map", "-meta-info", "-module-kind", "commonjs", "-target", "v5");
  private static final JsCompilationTask.Builder taskBuilder = JsCompilationTask.newBuilder();

  @Override
  CompilationTaskInfo.Builder infoBuilder() {
    return taskBuilder.getInfoBuilder();
  }

  @Override
  JsCompilationTask buildTask() {
    return taskBuilder.build();
  }

  @Override
  protected final void before() throws Throwable {
    taskBuilder.clear();
    super.before();

    taskBuilder.addAllPassThroughFlags(PASSTHROUGH_FLAGS);
    taskBuilder
        .getOutputsBuilder()
        .setJar(instanceRoot().resolve(label() + ".jar").toAbsolutePath().toString())
        .setSrcjar(instanceRoot().resolve(label() + "-sources.jar").toAbsolutePath().toString())
        .setJs(instanceRoot().resolve(label() + ".js").toAbsolutePath().toString());
  }

  public void addSource(String filename, String... lines) {
    Path sourcePath = super.writeSourceFile(filename, lines);
    taskBuilder.getInputsBuilder().addKotlinSources(sourcePath.toString());
  }
}
