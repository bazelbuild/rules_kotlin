package io.bazel.kotlin.builder.tasks.jvm;

import io.bazel.kotlin.builder.DaggerKotlinBuilderComponent;
import io.bazel.kotlin.builder.KotlinBuilderComponent;
import io.bazel.kotlin.builder.KotlinBuilderJvmTestTask;
import io.bazel.kotlin.builder.KotlinBuilderResource.Dep;
import io.bazel.kotlin.builder.KotlinBuilderResource.DirectoryType;
import io.bazel.kotlin.builder.toolchain.CompilationStatusException;
import io.bazel.kotlin.builder.toolchain.KotlinToolchain;
import io.bazel.kotlin.builder.utils.CompilationTaskContext;
import io.bazel.kotlin.model.AnnotationProcessor;
import io.bazel.kotlin.model.JvmCompilationTask;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.function.Consumer;

import static com.google.common.truth.Truth.assertThat;
import static io.bazel.kotlin.builder.KotlinBuilderResource.KOTLIN_ANNOTATIONS;
import static io.bazel.kotlin.builder.KotlinBuilderResource.KOTLIN_STDLIB;

public class KotlinBuilderJvmTest {
  private static Dep AUTO_VALUE =
      Dep.simpleOf(
          "external/io_bazel_rules_kotlin_com_google_auto_value_auto_value"
              + "/jar/io_bazel_rules_kotlin_com_google_auto_value_auto_value.jar");

  private static final AnnotationProcessor AUTO_VALUE_ANNOTATION_PROCESSOR =
      AnnotationProcessor.newBuilder()
          .setLabel("autovalue")
          .setProcessorClass("com.google.auto.value.processor.AutoValueProcessor")
          .addAllClasspath(Dep.classpathOf(AUTO_VALUE, KOTLIN_ANNOTATIONS))
          .build();

  private static final KotlinBuilderComponent component =
      DaggerKotlinBuilderComponent.builder().toolchain(KotlinToolchain.createToolchain()).build();

  @Rule public KotlinBuilderJvmTestTask ctx = new KotlinBuilderJvmTestTask();

  @Test
  public void testSimpleMixedModeCompile() {
    ctx.addSource("AClass.kt", "package something;" + "class AClass{}");
    ctx.addSource("AnotherClass.java", "package something;", "", "class AnotherClass{}");
    ctx.runCompileTask(this::jvmCompilationTask);
    ctx.assertFilesExist(
        DirectoryType.CLASSES, "something/AClass.class", "something/AnotherClass.class");
  }

  @Test
  public void testMixedBiReferences() {
    ctx.addSource(
        "AClass.java",
        "package a;",
        "",
        "import b.BClass;",
        "",
        "public class AClass {",
        "  static BClass b = new BClass();",
        "}");
    ctx.addSource(
        "BClass.kt",
        "package b",
        "",
        "import a.AClass",
        "",
        "class BClass() {",
        "  val a = AClass()",
        "}");
    ctx.runCompileTask(this::jvmCompilationTask);
    ctx.assertFilesExist(DirectoryType.CLASSES, "a/AClass.class", "b/BClass.class");
  }

  @Test
  public void testKaptKt() {
    ctx.addSource(
        "TestKtValue.kt",
        "package autovalue\n"
            + "\n"
            + "import com.google.auto.value.AutoValue\n"
            + "\n"
            + "@AutoValue\n"
            + "abstract class TestKtValue {\n"
            + "    abstract fun name(): String\n"
            + "    fun builder(): Builder = AutoValue_TestKtValue.Builder()\n"
            + "\n"
            + "    @AutoValue.Builder\n"
            + "    abstract class Builder {\n"
            + "        abstract fun setName(name: String): Builder\n"
            + "        abstract fun build(): TestKtValue\n"
            + "    }\n"
            + "}");
    ctx.addAnnotationProcessors(AUTO_VALUE_ANNOTATION_PROCESSOR);
    ctx.addDirectDependencies(AUTO_VALUE, KOTLIN_STDLIB);
    ctx.runCompileTask(this::jvmCompilationTask);
    ctx.assertFilesExist(
        DirectoryType.CLASSES,
        "autovalue/TestKtValue.class",
        "autovalue/AutoValue_TestKtValue.class");
    ctx.assertFilesExist(DirectoryType.SOURCE_GEN, "autovalue/AutoValue_TestKtValue.java");
  }

  @Test
  public void testMixedKaptBiReferences() {
    ctx.addSource(
        "TestKtValue.kt",
        "package autovalue.a\n"
            + "\n"
            + "import com.google.auto.value.AutoValue\n"
            + "import autovalue.b.TestAutoValue\n"
            + "\n"
            + "@AutoValue\n"
            + "abstract class TestKtValue {\n"
            + "    abstract fun name(): String\n"
            + "    fun builder(): Builder = AutoValue_TestKtValue.Builder()\n"
            + "\n"
            + "    @AutoValue.Builder\n"
            + "    abstract class Builder {\n"
            + "        abstract fun setName(name: String): Builder\n"
            + "        abstract fun build(): TestKtValue\n"
            + "    }\n"
            + "}");
    ctx.addSource(
        "TestAutoValue.java",
        "package autovalue.b;\n"
            + "\n"
            + "import com.google.auto.value.AutoValue;\n"
            + "import autovalue.a.TestKtValue;\n"
            + "\n"
            + "@AutoValue\n"
            + "public abstract class TestAutoValue {\n"
            + "    abstract String name();\n"
            + "\n"
            + "\n"
            + "    static Builder builder() {\n"
            + "        return new AutoValue_TestAutoValue.Builder();\n"
            + "    }\n"
            + "\n"
            + "    @AutoValue.Builder\n"
            + "    abstract static class Builder {\n"
            + "        abstract Builder setName(String name);\n"
            + "        abstract TestAutoValue build();\n"
            + "    }\n"
            + "\n"
            + "}");

    ctx.addAnnotationProcessors(AUTO_VALUE_ANNOTATION_PROCESSOR);
    ctx.addDirectDependencies(AUTO_VALUE, KOTLIN_STDLIB);
    ctx.runCompileTask(this::jvmCompilationTask);
    ctx.assertFilesExist(
        DirectoryType.SOURCE_GEN,
        "autovalue/a/AutoValue_TestKtValue.java",
        "autovalue/b/AutoValue_TestAutoValue.java");
    ctx.assertFilesExist(
        DirectoryType.CLASSES,
        "autovalue/a/AutoValue_TestKtValue.class",
        "autovalue/b/AutoValue_TestAutoValue.class");
  }

  @Test
  public void testKotlinErrorRendering() {
    ctx.addSource("AClass.kt", "package something;" + "class AClass{");
    testExpectingCompileError(lines -> assertThat(lines.get(0)).startsWith("sources/AClass"));
  }

  @Test
  public void testJavaErrorRendering() {
    ctx.addSource("AClass.kt", "package something;" + "class AClass{}");
    ctx.addSource("AnotherClass.java", "package something;", "", "class AnotherClass{");
    testExpectingCompileError(lines -> assertThat(lines.get(0)).startsWith("sources/AnotherClass"));
  }

  @Test
  @Ignore("The Kotlin compiler expects a single kotlin file at least.")
  public void testCompileSingleJavaFile() {
    ctx.addSource("AnotherClass.java", "package something;", "", "class AnotherClass{}");
    ctx.runCompileTask(this::jvmCompilationTask);
  }

  private void jvmCompilationTask(CompilationTaskContext taskContext, JvmCompilationTask task) {
    component.jvmTaskExecutor().execute(taskContext, task);
    ctx.assertFilesExist(task.getOutputs().getJar(), task.getOutputs().getJdeps());
  }

  private void testExpectingCompileError(Consumer<List<String>> validator) {
    try {
      ctx.runCompileTask(this::jvmCompilationTask);
    } catch (CompilationStatusException ex) {
      validator.accept(ctx.outLines());
      return;
    }
    throw new RuntimeException("expected an exception");
  }
}
