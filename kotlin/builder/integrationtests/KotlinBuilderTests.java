package io.bazel.kotlin.builder;

import com.google.common.truth.Truth;
import com.google.devtools.build.lib.view.proto.Deps;
import io.bazel.kotlin.builder.utils.CompilationTaskContext;
import io.bazel.kotlin.model.JvmCompilationTask;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class KotlinBuilderTests extends KotlinBuilderTestCase {
  @Test
  public void testSimpleCompile() {
    addSource("AClass.kt", "package something;" + "class AClass{}");
    runCompileTask();
    assertFileExists(DirectoryType.CLASSES, "something/AClass.class");
  }

  @Test
  public void testMixedModeCompile() {
    addSource("AClass.kt", "package something;" + "class AClass{}");
    addSource("AnotherClass.java", "package something;", "", "class AnotherClass{}");
    runCompileTask();
    assertFileExists(DirectoryType.CLASSES, "something/AClass.class");
    assertFileExists(DirectoryType.CLASSES, "something/AnotherClass.class");
    assertFileExists(outputs().getJar());
  }

  private void runCompileTask() {
    JvmCompilationTask command = builderCommand();
    for (DirectoryType directoryType : DirectoryType.values()) {
      try {
        if (directoryType != DirectoryType.ROOT) {
          Files.createDirectories(DirectoryType.select(directoryType, command));
        }
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    int timeoutSeconds = 10;
    try {

      CompletableFuture.runAsync(
              () ->
                  component()
                      .jvmTaskExecutor()
                      .execute(new CompilationTaskContext(command.getInfo(), System.err), command))
          .get(timeoutSeconds, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      throw new AssertionError("did not complete in: " + timeoutSeconds);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    assertFileExists(outputs().getJar());
    assertFileExists(outputs().getJdeps());
    try (FileInputStream fs = new FileInputStream(Paths.get(outputs().getJdeps()).toFile())) {
      Deps.Dependencies dependencies = Deps.Dependencies.parseFrom(fs);
      Truth.assertThat(dependencies.getRuleLabel()).endsWith(label());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
