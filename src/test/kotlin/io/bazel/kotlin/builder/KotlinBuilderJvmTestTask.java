package io.bazel.kotlin.builder;

import io.bazel.kotlin.builder.utils.CompilationTaskContext;
import io.bazel.kotlin.model.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class KotlinBuilderJvmTestTask extends KotlinBuilderResource {

  private static final JvmCompilationTask.Builder taskBuilder = JvmCompilationTask.newBuilder();

  @Override
  CompilationTaskInfo.Builder infoBuilder() {
    return taskBuilder.getInfoBuilder();
  }

  @Override
  protected void before() throws Throwable {
    super.before();
    taskBuilder.clear();

    taskBuilder
        .getInfoBuilder()
        .setLabel("//some/bogus:" + label())
        .setModuleName("some_bogus_module")
        .setPlatform(Platform.JVM)
        .setRuleKind(RuleKind.LIBRARY)
        .setToolchainInfo(
            KotlinToolchainInfo.newBuilder()
                .setCommon(
                    KotlinToolchainInfo.Common.newBuilder()
                        .setApiVersion("1.2")
                        .setCoroutines("enabled")
                        .setLanguageVersion("1.2"))
                .setJvm(KotlinToolchainInfo.Jvm.newBuilder().setJvmTarget("1.8")));
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
    Path path = directory(DirectoryType.SOURCES).resolve(filename).toAbsolutePath();
    try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
      fos.write(String.join("\n", lines).getBytes(UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    String pathAsString = path.toString();
    if (pathAsString.endsWith(".kt")) {
      taskBuilder.getInputsBuilder().addKotlinSources(pathAsString);
    } else if (pathAsString.endsWith(".java")) {
      taskBuilder.getInputsBuilder().addJavaSources(pathAsString);
    } else {
      throw new RuntimeException("unhandled file type: " + path.toString());
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

  public void runCompileTask(BiConsumer<CompilationTaskContext, JvmCompilationTask> operation) {
    JvmCompilationTask task = taskBuilder.build();
    super.runCompileTask(
        new CompilationTaskContext(task.getInfo(), System.err),
        task,
        (ctx, t) -> {
          operation.accept(ctx, t);
          return null;
        });
  }

  @SuppressWarnings("unused")
  public <R> R runCompileTask(BiFunction<CompilationTaskContext, JvmCompilationTask, R> operation) {
    JvmCompilationTask task = taskBuilder.build();
    return super.runCompileTask(
        new CompilationTaskContext(task.getInfo(), System.err), task, operation);
  }
}
