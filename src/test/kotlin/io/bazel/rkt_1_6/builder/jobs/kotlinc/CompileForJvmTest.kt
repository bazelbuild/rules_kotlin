package io.bazel.rkt_1_6.builder.jobs.jvm

import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import io.bazel.kotlin.builder.jobs.kotlinc.configurations.CompileKotlinForJvm
import io.bazel.kotlin.builder.jobs.kotlinc.configurations.CompileKotlinForJvm.In
import io.bazel.kotlin.builder.jobs.kotlinc.configurations.CompileKotlinForJvm.Out
import io.bazel.kotlin.integration.WriteWorkspace
import io.bazel.rkt_1_6.builder.jobs.jvm.CompileConfigurationSubject.Companion.configurations
import org.junit.Test
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.logging.Level
import kotlin.io.path.writeLines
import kotlin.streams.toList

class CompileForJvmTest {

  private val temp = Files.createTempDirectory(CompileForJvmTest::class.qualifiedName)

  data class TestIn(
    override val apiVersion: String = "1.6",
    override val languageVersion: String = "1.6",
    override val jvmTarget: String = "11",
    override val moduleName: String = "test",
    override val reducedClasspathMode: Boolean = false,
    override val depsArtifacts: List<Path> = emptyList(),
    override val classpath: List<Path> = emptyList(),
    override val fileSystem: FileSystem = FileSystems.getDefault(),
    override val sources: List<Path> = emptyList(),
    override val jdkHome: Path = FileSystems.getDefault().getPath(System.getProperty("java.home")),
    override val useIr: Boolean = false,
    override val debug: List<String> = emptyList(),
    override val sourcesFromJars: List<Path> = emptyList(),
    override val passthroughFlags: List<String> = emptyList(),
  ) : In

  data class TestOut(
    override val outputSrcJar: Path? = null,
    override val output: Path? = null,
  ) : Out

  val workspace = WriteWorkspace.using<CompileForJvmTest> {
    kotlin("Simple.kt") {
      `class`("Simple") {}
    }
  }

  @Test
  fun simpleSource() {
    assertAbout(configurations).that(CompileKotlinForJvm(), inDirectory = temp) {
      canCompile(
        TestIn(sources = listOf(workspace.resolve("Simple.kt"))),
        TestOut(
          output = temp.resolve("compiled.jar"),
          outputSrcJar = temp.resolve("compiled.srcjar"),
        ),
      ).successfully().and {
        assertThat(out.output.streamPaths().toList()).containsAtLeast(
          "Simple.class", "META-INF/test.kotlin_module",
        )
        assertThat(out.outputSrcJar.streamPaths().toList()).contains(
          "Simple.kt",
        )
      }
    }
  }


  @Test
  fun syntaxError() {
    val source = temp.resolve("Simple.kt").writeLines(
      listOf("class Simple --"),
    )

    assertAbout(configurations).that(CompileKotlinForJvm(), inDirectory = temp) {
      canCompile(
        TestIn(sources = listOf(source)),
        TestOut(
          output = temp.resolve("compiled.jar"),
        ),
      ).isError().and {
        assertAboutLogs().containsAtLeast(
          Level.SEVERE,
          listOf(
            "$source:1:14: error: expecting a top level declaration" +
              "\nclass Simple --" +
              "\n             ^",
          ),
        )
      }
    }
  }

  @Test
  fun withJavaSource() {
    val java = temp.resolve("HasJava.java").writeLines(
      listOf("interface HasJava {}"),
    )
    val kotlin = temp.resolve("KotlinClass.kt").writeLines(
      listOf("class KotlinClass : HasJava"),
    )

    assertAbout(configurations).that(CompileKotlinForJvm(), inDirectory = temp) {
      canCompile(
        TestIn(sources = listOf(kotlin, java)),
        TestOut(
          output = temp.resolve("compiled.jar"),
          outputSrcJar = temp.resolve("compiled.srcjar"),
        ),
      ).successfully().and {
        assertThat(out.output.streamPaths().toList()).apply {
          containsAtLeast(
            "KotlinClass.class", "META-INF/test.kotlin_module",
          )
          doesNotContain("HasJava.class")
        }
        assertThat(out.outputSrcJar.streamPaths().toList()).containsAtLeast(
          kotlin.fileName.toString(), java.fileName.toString(),
        )
      }
    }
  }
}
