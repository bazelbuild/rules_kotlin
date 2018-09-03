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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import io.bazel.kotlin.builder.toolchain.KotlinToolchain;
import io.bazel.kotlin.model.CompilationTaskInfo;
import io.bazel.kotlin.model.JvmCompilationTask;

import java.util.HashSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.bazel.kotlin.builder.Deps.*;

public final class KotlinBuilderJvmTestTask extends KotlinBuilderResource<JvmCompilationTask> {
  @SuppressWarnings({"unused", "WeakerAccess"})
  public static Dep
      KOTLIN_ANNOTATIONS =
          Dep.importJar(
              "kotlin-annotations",
              "external/com_github_jetbrains_kotlin/lib/annotations-13.0.jar"),
      KOTLIN_STDLIB =
          Dep.importJar(
              "kotlin-stdlib", "external/com_github_jetbrains_kotlin/lib/kotlin-stdlib.jar"),
      KOTLIN_STDLIB_JDK7 =
          Dep.importJar(
              "kotlin-stdlib-jdk7",
              "external/com_github_jetbrains_kotlin/lib/kotlin-stdlib-jdk7.jar"),
      KOTLIN_STDLIB_JDK8 =
          Dep.importJar(
              "kotlin-stdlib-jdk8",
              "external/com_github_jetbrains_kotlin/lib/kotlin-stdlib-jdk8.jar");

  private static final JvmCompilationTask.Builder taskBuilder = JvmCompilationTask.newBuilder();
  private static final KotlinBuilderComponent component =
      DaggerKotlinBuilderComponent.builder().toolchain(KotlinToolchain.createToolchain()).build();

  @Override
  CompilationTaskInfo.Builder infoBuilder() {
    return taskBuilder.getInfoBuilder();
  }

  @Override
  JvmCompilationTask buildTask() {
    return taskBuilder.build();
  }

  @Override
  protected final void before() throws Throwable {
    taskBuilder.clear();
    super.before();

    taskBuilder
        .getDirectoriesBuilder()
        .setClasses(directory(DirectoryType.CLASSES).toAbsolutePath().toString())
        .setGeneratedSources(directory(DirectoryType.SOURCE_GEN).toAbsolutePath().toString())
        .setTemp(directory(DirectoryType.TEMP).toAbsolutePath().toString())
        .setGeneratedClasses(
            directory(DirectoryType.GENERATED_CLASSES).toAbsolutePath().toString());
    taskBuilder
        .getOutputsBuilder()
        .setJar(instanceRoot().resolve("jar_file.jar").toAbsolutePath().toString())
        .setJdeps(instanceRoot().resolve("jdeps_file.jdeps").toAbsolutePath().toString())
        .setSrcjar(instanceRoot().resolve("jar_file-sources.jar").toAbsolutePath().toString());
  }

  private void resetForNext() {
    try {
      before();
    } catch (Throwable throwable) {
      throw new RuntimeException(throwable);
    }
  }

  public void addSource(String filename, String... lines) {
    String pathAsString = super.writeSourceFile(filename, lines).toString();
    if (pathAsString.endsWith(".kt")) {
      taskBuilder.getInputsBuilder().addKotlinSources(pathAsString);
    } else if (pathAsString.endsWith(".java")) {
      taskBuilder.getInputsBuilder().addJavaSources(pathAsString);
    } else {
      throw new RuntimeException("unhandled file type: " + pathAsString);
    }
  }

  public void addAnnotationProcessors(AnnotationProcessor... annotationProcessors) {
    Preconditions.checkState(
        taskBuilder.getInputs().getProcessorsList().isEmpty(), "processors already set");
    HashSet<String> processorClasses = new HashSet<>();
    taskBuilder
        .getInputsBuilder()
        .addAllProcessorpaths(
            Stream.of(annotationProcessors)
                .peek(it -> processorClasses.add(it.processClass()))
                .flatMap(it -> it.processorPath().stream())
                .distinct()
                .collect(Collectors.toList()))
        .addAllProcessors(processorClasses);
  }

  public void addDirectDependencies(Dep... dependencies) {
    Dep.classpathOf(dependencies)
        .forEach((dependency) -> taskBuilder.getInputsBuilder().addClasspath(dependency));
  }

  private Dep currentDep() {
    return Dep.builder()
        .label(label())
        .compileJars(ImmutableSet.of(taskBuilder.getOutputs().getJar()))
        .runtimeDeps(ImmutableSet.copyOf(taskBuilder.getInputs().getClasspathList()))
        .build();
  }

  @SafeVarargs
  public final Dep runCompileTask(Consumer<KotlinBuilderJvmTestTask>... setup) {
    Stream.of(setup).forEach(it -> it.accept(this));
    return runCompileTask(
        (taskContext, task) -> {
          component.jvmTaskExecutor().execute(taskContext, task);
          assertFilesExist(task.getOutputs().getJar(), task.getOutputs().getJdeps());
          return currentDep();
        });
  }

  /** Run a single compile task returning a dep and resetting the context. */
  @SafeVarargs
  public final Dep supplyDepTask(Consumer<KotlinBuilderJvmTestTask>... setup) {
    Dep dep = runCompileTask(setup);
    resetForNext();
    return dep;
  }
}
