package io.bazel.kotlin.builder;

import io.bazel.kotlin.model.AnnotationProcessor;
import io.bazel.kotlin.model.CompilationTaskInfo;
import io.bazel.kotlin.model.JvmCompilationTask;

import java.util.Arrays;

public final class KotlinBuilderJvmTestTask extends KotlinBuilderResource<JvmCompilationTask> {
  private static final JvmCompilationTask.Builder taskBuilder = JvmCompilationTask.newBuilder();

  @Override
  CompilationTaskInfo.Builder infoBuilder() {
    return taskBuilder.getInfoBuilder();
  }

  @Override
  JvmCompilationTask buildTask() {
    return taskBuilder.build();
  }

  @Override
  protected void before() throws Throwable {
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
    taskBuilder
        .getInfoBuilder()
        .getPluginsBuilder()
        .addAllAnnotationProcessors(Arrays.asList(annotationProcessors));
  }

  public void addDirectDependencies(Dep... dependencies) {
    Dep.merge(dependencies)
        .compileJars()
        .forEach(
            (dependency) -> {
              assert dependency.toFile().exists();
              String depString = dependency.toString();
              taskBuilder.getInputsBuilder().addClasspath(depString);
            });
  }
}
