package io.bazel.kotlin.builder;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.inject.Injector;
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

  private final KotlinModel.BuilderCommand.Builder builder =
      KotlinModel.BuilderCommand.newBuilder();
  private final Injector injector = KotlinToolchain.createInjector(() -> System.out, null);
  private String label = null;
  private Path inputSourceDir = null;

  @Before
  public void setupNext() {
    resetTestContext("a_test_" + counter.incrementAndGet());
  }

  protected KotlinModel.BuilderCommand.Outputs outputs() {
    return builder.getOutputs();
  }

  protected String label() {
    return Preconditions.checkNotNull(label);
  }

  private Path classDir() {
    return Paths.get(outputs().getClassDirectory());
  }

  protected KotlinModel.BuilderCommand builderCommand() {
    return builder.build();
  }

  protected <T> T instance(Class<T> clazz) {
    return injector.getInstance(clazz);
  }

  protected void addSource(String filename, String... lines) {
    Path file = Preconditions.checkNotNull(inputSourceDir, "initialize test context").resolve(filename);
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
    builder.clear();
    KotlinModel.BuilderCommand.Outputs.Builder testOuputs = setupTestOutputs(label);
    builder.setInfo(
        KotlinModel.BuilderCommand.Info.newBuilder()
            .setLabel("//some/bogus:" + label)
            .setKotlinModuleName("some_bogus_module")
            .setRuleKind("kt_jvm_library")
            .setToolchainInfo(
                KotlinModel.KotlinToolchainInfo.newBuilder()
                    .setCommon(
                        KotlinModel.KotlinToolchainInfo.Common.newBuilder()
                            .setApiVersion("1.2")
                            .setCoroutines("enabled")
                            .setLanguageVersion("1.2"))
                    .setJvm(KotlinModel.KotlinToolchainInfo.Jvm.newBuilder().setJvmTarget("1.8"))));
    builder.setOutputs(testOuputs);
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

  private KotlinModel.BuilderCommand.Outputs.Builder setupTestOutputs(String label) {
    Path prefixPath = Paths.get(label);
    createTestOuputDirectory(prefixPath);
    inputSourceDir = Paths.get(createTestOuputDirectory(prefixPath.resolve("input_sources")));
    return KotlinModel.BuilderCommand.Outputs.newBuilder()
        .setClassDirectory(prefixPath.resolve("classes").toAbsolutePath().toString())
        .setSourceGenDir(prefixPath.resolve("sources").toAbsolutePath().toString())
        .setTempDirectory(prefixPath.resolve("temp").toAbsolutePath().toString())
        .setOutputJdeps(prefixPath.resolve("jdeps_files.jdeps").toAbsolutePath().toString())
        .setOutput(prefixPath.resolve("jar_file.jar").toAbsolutePath().toString());
  }

  protected enum DirectoryType {
    ROOT,
    CLASSES,
    TEMP,
    SOURCE_GEN;

    protected static Path select(DirectoryType type, KotlinModel.BuilderCommand command) {
      Path ret;
      switch (type) {
        case CLASSES:
          ret = Paths.get(command.getOutputs().getClassDirectory());
          break;
        case TEMP:
          ret = Paths.get(command.getOutputs().getTempDirectory());
          break;
        case SOURCE_GEN:
          ret = Paths.get(command.getOutputs().getSourceGenDir());
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
