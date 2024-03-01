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
package io.bazel.kotlin.builder;

import io.bazel.kotlin.builder.toolchain.KotlinToolchain;
import io.bazel.kotlin.model.CompilationTaskInfo;
import io.bazel.kotlin.model.JsCompilationTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

public final class KotlinJsTestBuilder extends KotlinAbstractTestBuilder<JsCompilationTask> {
  private static final List<String> PASSTHROUGH_FLAGS =
      Arrays.asList("-source-map", "-module-kind", "commonjs", "-target", "v5",  "-Xir-produce-klib-dir");
  private static final JsCompilationTask.Builder taskBuilder = JsCompilationTask.newBuilder();
  private static final KotlinBuilderComponent component =
      DaggerKotlinBuilderComponent.builder().toolchain(withReflect(toolchainForTest())).build();
  private static final EnumSet<DirectoryType> ALL_DIRECTORY_TYPES =
      EnumSet.allOf(DirectoryType.class);
  private final TaskBuilder taskBuilderInstance = new TaskBuilder();

  private static KotlinToolchain withReflect(KotlinToolchain toolchain) {
    return toolchain.toolchainWithReflect(
            new File(Deps.Dep.fromLabel("@rules_kotlin//kotlin/compiler:kotlin-reflect").singleCompileJar())
    );
  }

  @Override
  JsCompilationTask buildTask() {
    return taskBuilder.build();
  }

  @Override
  void setupForNext(CompilationTaskInfo.Builder infoBuilder) {
    taskBuilder.clear().setInfo(infoBuilder);
    DirectoryType.createAll(instanceRoot(), ALL_DIRECTORY_TYPES);
    taskBuilder.addAllPassThroughFlags(PASSTHROUGH_FLAGS);
    try {
      taskBuilder.getDirectoriesBuilder().setTemp(
          Files.createDirectories(directory(DirectoryType.TEMP).resolve("working")).toString());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    taskBuilder
        .getOutputsBuilder()
        .setJar(directory(DirectoryType.TEMP).resolve(label() + ".jar").toAbsolutePath().toString())
        .setSrcjar(directory(DirectoryType.TEMP).resolve(label() + "-sources.jar").toAbsolutePath().toString())
        .setJs(directory(DirectoryType.TEMP).resolve(label() + ".js").toAbsolutePath().toString());
  }

  public String runCompilationTask(Consumer<TaskBuilder> setup, Consumer<List<String>> outlines) {
    resetForNext();
    setup.accept(taskBuilderInstance);
    try {
    return runCompileTask(
        (taskContext, task) -> {
          component.jsTaskExecutor().execute(taskContext, task);
          String jsFile = task.getOutputs().getJs();
          assertFilesExist(
              jsFile,
              jsFile + ".map",
              task.getOutputs().getJar(),
              task.getOutputs().getSrcjar());
          return task.getOutputs().getJar();
        });
    } finally{
      outlines.accept(outLines());
    }
  }

  public void runCompilationTask(Consumer<TaskBuilder> setup) {
    runCompilationTask(setup, l -> {});
  }

  public final class TaskBuilder {
    public void addSource(String filename, String... lines) {
      taskBuilder.getInputsBuilder().addKotlinSources(writeSourceFile(filename, lines).toString());
    }

    public void addDependency(String filename) {
      taskBuilder.getInputsBuilder().addLibraries(filename);
    }

    public void addArg(String flag) {
      taskBuilder.addPassThroughFlags(flag );
    }
  }
}
