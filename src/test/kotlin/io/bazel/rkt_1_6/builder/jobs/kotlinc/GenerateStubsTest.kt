package io.bazel.rkt_1_6.builder.jobs.kotlinc

import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import io.bazel.kotlin.builder.utils.BazelRunFiles.resolveVerifiedFromProperty
import io.bazel.kotlin.integration.WriteWorkspace
import io.bazel.rkt_1_6.builder.jobs.kotlinc.CompileConfigurationSubject.Companion.configurations
import io.bazel.kotlin.builder.jobs.kotlinc.configurations.CompileKotlinForJvm
import io.bazel.kotlin.builder.jobs.kotlinc.configurations.GenerateStubs
import org.junit.Test
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.toList

class GenerateStubsTest {

  private val temp = Files.createTempDirectory(GenerateStubsTest::class.qualifiedName)

  data class In(
    override val apiVersion: String = "1.6",
    override val languageVersion: String = "1.6",
    override val jvmTarget: String = "11",
    override val moduleName: String = "test",
    override val reducedClasspathMode: Boolean = false,
    override val depsArtifacts: List<Path> = emptyList(),
    override val classpath: List<Path> = listOf(
      resolveVerifiedFromProperty("auto_value_annotations").toPath(),
      resolveVerifiedFromProperty("auto_value").toPath(),
      resolveVerifiedFromProperty("kotlin_annotations").toPath(),
    ),
    override val fileSystem: FileSystem = FileSystems.getDefault(),
    override val sources: List<Path> = emptyList(),
    override val useIr: Boolean = false,
    override val debug: List<String> = listOf("trace", "debug"),
    override val sourcesFromJars: List<Path> = emptyList(),
    override val jdkHome: Path = resolveVerifiedFromProperty("java.home").toPath(),
    override val passthroughFlags: List<String> = emptyList(),
    override val stubsPluginClassPath: List<Path> = emptyList(),
    override val stubsPluginOptions: List<String> = emptyList(),
    override val processorPath: List<Path> = listOf(
      resolveVerifiedFromProperty("auto_value_annotations").toPath(),
      resolveVerifiedFromProperty("auto_value").toPath(),
      resolveVerifiedFromProperty("kotlin_annotations").toPath(),
    ),
    override val processors: List<String> = listOf(
      "com.google.auto.value.processor.AutoValueProcessor"
    ),
    override val kapt: Path = resolveVerifiedFromProperty("kapt").toPath(),
    override val verbose: Boolean = true,
    override val processorOptions: Map<String, String> = emptyMap(),
  ) : CompileKotlinForJvm.In, GenerateStubs.In

  data class Out(
    override val outputSrcJar: Path? = null,
    override val output: Path? = null,
    override val generatedJavaSrcJar: Path?,
    override val generatedJavaStubJar: Path?,
    override val generatedClassJar: Path?
  ) : CompileKotlinForJvm.Out, GenerateStubs.Out

  val workspace = WriteWorkspace.using<GenerateStubsTest> {
    kotlin("autovalue/A.kt") {
      `package`("autovalue")
      `import`("com.google.auto.value.AutoValue")
      `@`("AutoValue")
      "abstract class A" {
        +"abstract fun name(): String"
        +"fun builder(): Builder = AutoValue_A.Builder()"
        `@`("AutoValue.Builder")
        "abstract class Builder" {
          +"abstract fun setName(name: String): Builder"
          +"abstract fun build(): A"
        }
      }
    }

    java("autovalue/B.kt") {
      `package`("autovalue")
      `import`("com.google.auto.value.AutoValue")
      `import`("autovalue.A")
      `@`("AutoValue")
      "abstract class B" {
        +"abstract String name();"
        +"abstract A a();"
        "static Builder builder()" {
          +"return new AutoValue_B.Builder();"
        }
        `@`("AutoValue.Builder")
        "abstract static class Builder" {
          +"abstract Builder setName(String name);"
          +"abstract Builder setA(A a);"
          +"abstract B build();"
        }
      }
    }

    kotlin("serialize/Data.kt") {
      `package`("serialize")
      `import`("kotlinx.serialization.Serializable")
      `@`("Serializable")
      +"data class Data(val stringValue: String, val intValue: Int)"
    }
  }

  @Test
  fun autoValueforKotlinClass() {
    val source = workspace.resolve("autovalue/A.kt")
    assertAbout(configurations).that(CompileKotlinForJvm(), GenerateStubs(), inDirectory = temp)
      .canCompile(
        In(
          sources = listOf(source)
        ),
        Out(
          generatedJavaSrcJar = temp.resolve("generatedJava.srcjar"),
          generatedJavaStubJar = temp.resolve("stubs.jar"),
          generatedClassJar = temp.resolve("generated.jar")
        )
      ).successfully().and { out ->
        assertThat(out.generatedJavaStubJar.streamPaths().toList()).containsExactly(
          "error",
          "error/NonExistentClass.java",
          "autovalue",
          "autovalue/A.java"
        )
        assertThat(out.generatedClassJar.streamPaths().toList()).containsExactly(
          "autovalue",
          "autovalue/A.class",
          "autovalue/A\$Builder.class",
          "META-INF",
          "META-INF/test.kotlin_module",
          "META-INF/MANIFEST.MF"
        )
        assertThat(out.generatedJavaSrcJar.streamPaths().toList()).isEmpty()
      }
  }

  @Test
  fun autoValueDependencies() {
    val sourceA = workspace.resolve("autovalue/A.kt")
    val sourceB = workspace.resolve("autovalue/B.kt")
    assertAbout(configurations).that(CompileKotlinForJvm(), GenerateStubs(), inDirectory = temp) {
      canCompile(
        In(sources = listOf(sourceA)),
        Out(
          generatedJavaSrcJar = temp.resolve("a-generatedJava.srcjar"),
          generatedJavaStubJar = temp.resolve("a-stubs.jar"),
          generatedClassJar = temp.resolve("a-generated.jar")
        )
      ).successfully().and { dependencies ->
        canCompile(
          In(
            sources = listOf(sourceB),
            classpath = dependencies.generatedClassJar.asListOf()
          ),
          Out(
            generatedJavaSrcJar = temp.resolve("b-generatedJava.srcjar"),
            generatedJavaStubJar = temp.resolve("b-stubs.jar"),
            generatedClassJar = temp.resolve("b-generated.jar")
          )
        ).successfully().and { out ->
          assertThat(out.generatedJavaStubJar.streamPaths().toList()).containsExactly(
            "error",
            "error/NonExistentClass.java",
            "autovalue",
            "autovalue/B.java"
          )
          assertThat(out.generatedClassJar.streamPaths().toList()).containsExactly(
            "autovalue",
            "autovalue/B.class",
            "autovalue/B\$Builder.class",
            "META-INF",
            "META-INF/test.kotlin_module",
            "META-INF/MANIFEST.MF"
          )
          assertThat(out.generatedJavaSrcJar.streamPaths().toList()).isEmpty()
        }
      }
    }
  }

  @Test
  fun withStubPlugins() {
    val sourceAutovalue = workspace.resolve("autovalue/A.kt")
    val sourceData = workspace.resolve("serialize/Data.kt")
    assertAbout(configurations).that(CompileKotlinForJvm(), GenerateStubs(), inDirectory = temp)
      .canCompile(
        In(
          sources = listOf(sourceAutovalue, sourceData),
          classpath = listOf(resolveVerifiedFromProperty("serialization_core").toPath()),
          stubsPluginClassPath = listOf(resolveVerifiedFromProperty("serialization_plugin").toPath()),
          stubsPluginOptions = listOf()
        ),
        Out(
          generatedJavaSrcJar = temp.resolve("generatedJava.srcjar"),
          generatedJavaStubJar = temp.resolve("stubs.jar"),
          generatedClassJar = temp.resolve("generated.jar")
        )
      ).successfully().and { out ->
        assertThat(out.generatedJavaStubJar.streamPaths().toList()).containsExactly(
          "error",
          "error/NonExistentClass.java",
          "autovalue",
          "autovalue/A.java"
        )
        assertThat(out.generatedClassJar.streamPaths().toList()).containsExactly(
          "autovalue",
          "autovalue/A.class",
          "autovalue/A\$Builder.class",
          "META-INF",
          "META-INF/test.kotlin_module",
          "META-INF/MANIFEST.MF"
        )
        assertThat(out.generatedJavaSrcJar.streamPaths().toList()).isEmpty()
      }
  }
}
