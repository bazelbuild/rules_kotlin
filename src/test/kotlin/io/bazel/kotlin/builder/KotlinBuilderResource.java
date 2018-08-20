package io.bazel.kotlin.builder;

import io.bazel.kotlin.builder.toolchain.CompilationException;
import io.bazel.kotlin.builder.toolchain.CompilationStatusException;
import io.bazel.kotlin.builder.utils.CompilationTaskContext;
import io.bazel.kotlin.model.CompilationTaskInfo;
import org.junit.rules.ExternalResource;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertWithMessage;

public abstract class KotlinBuilderResource extends ExternalResource {
  public enum DirectoryType {
    INSTANCE_ROOT("test root", null),
    EXTERNAL("bazel external directory", null),
    /** The rest of the paths are instance relative. */
    SOURCES("sources", Paths.get("sources")),
    CLASSES("compiled classes", Paths.get("classes")),
    GENERATED_CLASSES("generated classes", Paths.get("generated_classes")),
    TEMP("temp directory", Paths.get("temp")),
    SOURCE_GEN("generated sources directory", Paths.get("generated_sources"));

    private static final EnumSet<DirectoryType> INSTANCE_TYPES =
        EnumSet.of(SOURCES, CLASSES, SOURCE_GEN, GENERATED_CLASSES, TEMP);

    final String name;
    private final Path relativePath;

    DirectoryType(String name, Path relativePath) {
      this.name = name;
      this.relativePath = relativePath;
    }
  }

  public abstract static class Dep {
    private Dep() {}

    abstract Stream<Path> compileJars();

    private static class Simple extends Dep {
      private final List<Path> compileJars;

      private Simple(List<Path> compileJars) {
        this.compileJars = compileJars;
      }

      @Override
      Stream<Path> compileJars() {
        return compileJars.stream();
      }
    }

    static Dep merge(Dep... dependencies) {
      return new Simple(
          Stream.of(dependencies).flatMap(Dep::compileJars).collect(Collectors.toList()));
    }

    public static List<String> classpathOf(Dep... dependencies) {
      return merge(dependencies).compileJars().map(Path::toString).collect(Collectors.toList());
    }

    public static Dep simpleOf(String compileJar) {
      return new Simple(Collections.singletonList(toPlatformPath(compileJar).toAbsolutePath()));
    }

    @SuppressWarnings("unused")
    static Dep simpleOf(Path compileJar) {
      return new Simple(Collections.singletonList(compileJar));
    }
  }

  private static final Path
      BAZEL_TEST_DIR = Paths.get(Objects.requireNonNull(System.getenv("TEST_TMPDIR"))),
      EXTERNAL_PATH = Paths.get("external");
  @SuppressWarnings("unused")
  public static Dep
      KOTLIN_ANNOTATIONS =
          Dep.simpleOf("external/com_github_jetbrains_kotlin/lib/annotations-13.0.jar"),
      KOTLIN_STDLIB = Dep.simpleOf("external/com_github_jetbrains_kotlin/lib/kotlin-stdlib.jar"),
      KOTLIN_STDLIB_JDK7 =
          Dep.simpleOf("external/com_github_jetbrains_kotlin/lib/kotlin-stdlib-jdk7.jar"),
      KOTLIN_STDLIB_JDK8 =
          Dep.simpleOf("external/com_github_jetbrains_kotlin/lib/kotlin-stdlib-jdk8.jar");

  private static final AtomicInteger counter = new AtomicInteger(0);
  private Path instanceRoot = null;
  private String label = null;

  KotlinBuilderResource() {}

  abstract CompilationTaskInfo.Builder infoBuilder();

  String label() {
    return Objects.requireNonNull(label);
  }

  Path instanceRoot() {
    return Objects.requireNonNull(instanceRoot);
  }

  public List<String> outLines() {
    return outLines;
  }

  @Override
  protected void before() throws Throwable {
    outLines = null;
    label = "a_test_" + counter.incrementAndGet();
    setTimeout(DEFAULT_TIMEOUT);
    try {
      this.instanceRoot = Files.createDirectory(BAZEL_TEST_DIR.resolve(Paths.get(label)));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    for (DirectoryType instanceType : DirectoryType.INSTANCE_TYPES) {
      try {
        Files.createDirectory(instanceRoot.resolve(instanceType.relativePath));
      } catch (IOException e) {
        throw new RuntimeException("could not create instance directory: " + instanceType.name, e);
      }
    }
  }

  Path directory(DirectoryType type) {
    switch (type) {
      case INSTANCE_ROOT:
        return instanceRoot;
      case EXTERNAL:
        return KotlinBuilderResource.EXTERNAL_PATH;
      case SOURCES:
      case CLASSES:
      case GENERATED_CLASSES:
      case TEMP:
      case SOURCE_GEN:
        return instanceRoot.resolve(type.relativePath);
      default:
        throw new IllegalStateException(type.toString());
    }
  }

  @SuppressWarnings("unused")
  public final void setDebugTags(String... tags) {
    infoBuilder().addAllDebug(Arrays.asList(tags));
  }

  private int DEFAULT_TIMEOUT = 10;
  private int timeoutSeconds = DEFAULT_TIMEOUT;
  private List<String> outLines = null;

  /**
   * sets the timeout for the builder tasks.
   *
   * @param timeoutSeconds a timeout in seconds. For debugging purposes it can be set to <= 0 which
   *     means wait indefinitely.
   */
  @SuppressWarnings("WeakerAccess")
  public final void setTimeout(int timeoutSeconds) {
    this.timeoutSeconds = timeoutSeconds;
  }

  <T, R> R runCompileTask(
      CompilationTaskInfo info, T task, BiFunction<CompilationTaskContext, T, R> operation) {
    String curDir = System.getProperty("user.dir");
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    try (PrintStream outputStream = new PrintStream(byteArrayOutputStream)) {
      System.setProperty("user.dir", instanceRoot().toAbsolutePath().toString());
      CompletableFuture<R> future =
          CompletableFuture.supplyAsync(
              () -> operation.apply(new CompilationTaskContext(info, outputStream), task));
      return timeoutSeconds > 0 ? future.get(timeoutSeconds, TimeUnit.SECONDS) : future.get();
    } catch (ExecutionException e) {
      if (e.getCause() instanceof CompilationStatusException) {
        throw (CompilationStatusException) e.getCause();
      } else if (e.getCause() instanceof CompilationException) {
        throw (CompilationException) e.getCause();
      } else {
        throw new RuntimeException(e.getCause());
      }
    } catch (TimeoutException e) {
      throw new AssertionError("did not complete in: " + timeoutSeconds);
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      System.setProperty("user.dir", curDir);
      outLines =
          Collections.unmodifiableList(
              new BufferedReader(
                      new InputStreamReader(
                          new ByteArrayInputStream(byteArrayOutputStream.toByteArray())))
                  .lines()
                  .peek(System.err::println)
                  .collect(Collectors.toList()));
    }
  }

  public final void assertFilesExist(DirectoryType dir, String... paths) {
    assertFileExistence(resolved(dir, paths), true);
  }

  public void assertFilesExist(String... paths) {
    assertFileExistence(Stream.of(paths).map(Paths::get), true);
  }

  @SuppressWarnings("unused")
  public final void assertFilesDoNotExist(DirectoryType dir, String... filePath) {
    assertFileExistence(resolved(dir, filePath), false);
  }

  private static void assertFileExistence(Stream<Path> pathStream, boolean shouldExist) {
    pathStream.forEach(
        path -> {
          if (shouldExist)
            assertWithMessage("file did not exist: " + path).that(path.toFile().exists()).isTrue();
          else assertWithMessage("file existed: " + path).that(path.toFile().exists()).isFalse();
        });
  }

  private Stream<Path> resolved(DirectoryType dir, String... filePath) {
    Path directory = directory(dir);
    return Stream.of(filePath).map(f -> directory.resolve(toPlatformPath(f)));
  }

  /**
   * Normalize a path string.
   *
   * @param path a path using '/' as the separator.
   * @return a path string suitable for the target platform.
   */
  private static Path toPlatformPath(String path) {
    String[] parts = path.split("/");
    return parts.length == 1
        ? Paths.get(parts[0])
        : Paths.get(parts[0], Arrays.copyOfRange(parts, 1, parts.length));
  }

  @SuppressWarnings("unused")
  private Stream<Path> directoryContents(DirectoryType type) {
    try {
      return Files.walk(directory(type)).map(p -> directory(type).relativize(p));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @SuppressWarnings("unused")
  public final void logDirectoryContents(DirectoryType type) {
    System.out.println(
        directoryContents(type)
            .map(Path::toString)
            .collect(Collectors.joining("\n", "directory " + type.name + " contents:\n", "")));
  }
}
