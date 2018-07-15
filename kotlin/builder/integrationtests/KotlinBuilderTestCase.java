package io.bazel.kotlin.builder;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import io.bazel.kotlin.builder.toolchain.KotlinToolchain;
import io.bazel.kotlin.model.KotlinModel;
import org.junit.Before;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.truth.Truth.assertWithMessage;

abstract class KotlinBuilderTestCase {
  private static final Path BAZEL_TEST_DIR =
      Paths.get(Preconditions.checkNotNull(System.getenv("TEST_TMPDIR")));
  private static final AtomicInteger counter = new AtomicInteger(0);

  private final KotlinModel.CompilationTask.Builder builder =
      KotlinModel.CompilationTask.newBuilder();
  private final KotlinBuilderComponent component =
      DaggerKotlinBuilderComponent.builder()
          .out(System.err)
          .toolchain(KotlinToolchain.createToolchain())
          .build();

  private String label = null;
  private Path inputSourceDir = null;

  @Before
  public void setupNext() {
    resetTestContext("a_test_" + counter.incrementAndGet());
  }

  protected KotlinModel.CompilationTask.Outputs outputs() {
    return builder.getOutputs();
  }

  protected KotlinModel.CompilationTask.Directories directories() {
    return builder.getDirectories();
  }

  protected String label() {
    return Preconditions.checkNotNull(label);
  }

  private Path classDir() {
    return Paths.get(directories().getClasses());
  }

  protected KotlinModel.CompilationTask builderCommand() {
    return builder.build();
  }

  protected KotlinBuilderComponent component() {
    return component;
  }

  protected void addSource(String filename, String... lines) {
    Path file =
        Preconditions.checkNotNull(inputSourceDir, "initialize test context").resolve(filename);
    try (BufferedWriter writer = com.google.common.io.Files.newWriter(file.toFile(), UTF_8)) {
      writer.write(Joiner.on("\n").join(lines));
      String f = file.toString();
      if (f.endsWith(".kt")) {
        builder.getInputsBuilder().addKotlinSources(f);
      } else if (f.endsWith(".java")) {
        builder.getInputsBuilder().addJavaSources(f);
      } else {
        throw new RuntimeException("unhandled file type: " + f);
      }

    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  protected void resetTestContext(String label) {
    this.label = label;
    Path prefixPath = Paths.get(label);

    createTestOuputDirectory(prefixPath);
    inputSourceDir = Paths.get(createTestOuputDirectory(prefixPath.resolve("input_sources")));

    builder.clear();
    builder
        .getInfoBuilder()
        .setLabel("//some/bogus:" + label)
        .setModuleName("some_bogus_module")
        .setPlatform(KotlinModel.CompilationTask.Info.Platform.JVM)
        .setRuleKind(KotlinModel.CompilationTask.Info.RuleKind.LIBRARY)
        .setToolchainInfo(
            KotlinModel.KotlinToolchainInfo.newBuilder()
                .setCommon(
                    KotlinModel.KotlinToolchainInfo.Common.newBuilder()
                        .setApiVersion("1.2")
                        .setCoroutines("enabled")
                        .setLanguageVersion("1.2"))
                .setJvm(KotlinModel.KotlinToolchainInfo.Jvm.newBuilder().setJvmTarget("1.8")));
    builder
        .getDirectoriesBuilder()
        .setClasses(prefixPath.resolve("classes").toAbsolutePath().toString())
        .setGeneratedSources(prefixPath.resolve("sources").toAbsolutePath().toString())
        .setTemp(prefixPath.resolve("temp").toAbsolutePath().toString())
        .setGeneratedClasses(prefixPath.resolve("generated_classes").toAbsolutePath().toString());
    builder
        .getOutputsBuilder()
        .setJar(prefixPath.resolve("jar_file.jar").toAbsolutePath().toString())
        .setJdeps(prefixPath.resolve("jdeps_file.jdeps").toAbsolutePath().toString())
        .setSrcjar(prefixPath.resolve("jar_file-sources.jar").toAbsolutePath().toString());
  }

  private static String createTestOuputDirectory(Path path) {
    try {
      return Files.createDirectory(BAZEL_TEST_DIR.resolve(path)).toAbsolutePath().toString();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static String createTestOutputFile(Path path) {
    try {
      return Files.createFile(BAZEL_TEST_DIR.resolve(path)).toAbsolutePath().toString();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  protected enum DirectoryType {
    ROOT,
    CLASSES,
    GENERATED_CLASSES,
    TEMP,
    SOURCE_GEN;

    protected static Path select(DirectoryType type, KotlinModel.CompilationTask command) {
      Path ret;
      switch (type) {
        case CLASSES:
          ret = Paths.get(command.getDirectories().getClasses());
          break;
        case GENERATED_CLASSES:
          ret = Paths.get(command.getDirectories().getGeneratedClasses());
          break;
        case TEMP:
          ret = Paths.get(command.getDirectories().getTemp());
          break;
        case SOURCE_GEN:
          ret = Paths.get(command.getDirectories().getGeneratedSources());
          break;
        default:
          throw new RuntimeException("unhandled type: " + type);
      }
      return ret;
    }
  }

  void assertFileExists(DirectoryType dir, String filePath) {
    Path file = DirectoryType.select(dir, builderCommand()).resolve(filePath);
    assertFileExists(file.toString());
  }

  void assertFileDoesNotExist(String filePath) {
    assertWithMessage("file exisst: " + filePath).that(new File(filePath).exists()).isFalse();
  }

  void assertFileExists(String filePath) {
    assertWithMessage("file did not exist: " + filePath).that(new File(filePath).exists()).isTrue();
  }
}
