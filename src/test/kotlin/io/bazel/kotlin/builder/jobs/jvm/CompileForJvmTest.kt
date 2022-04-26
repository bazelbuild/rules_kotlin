package io.bazel.kotlin.builder.jobs.jvm

import com.google.common.truth.Truth.assertThat
import io.bazel.kotlin.builder.jobs.jvm.configurations.BaseConfiguration
import io.bazel.worker.ContextLog.FileScope
import io.bazel.worker.ContextLog.Logging
import io.bazel.worker.Status
import org.junit.Test
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Files.walk
import java.nio.file.Path
import java.util.logging.Level
import kotlin.io.path.createDirectories
import kotlin.io.path.writeLines
import kotlin.streams.toList

class CompileForJvmTest {

  private val temp = Files.createTempDirectory(CompileForJvmTest::class.qualifiedName)

  data class TestInputs(
    override val apiVersion: String = "1.6",
    override val languageVersion: String = "1.6",
    override val jvmTarget: String = "11",
    override val moduleName: String = "test",
    override val reducedClasspathMode: Boolean = false,
    override val depsArtifacts: List<Artifact> = emptyList(),
    override val classpath: List<Artifact> = emptyList(),
    override val fileSystem: FileSystem = FileSystems.getDefault(),
    override val sources: List<Artifact> = emptyList(),
    override val jdkHome: Artifact = Artifact(
      FileSystems.getDefault().getPath(System.getProperty("java.home"))
    )
  ) : BaseConfiguration.Inputs

  data class TestScope(override val directory: Path) : Logging, FileScope {

    val logs = mutableMapOf<Level, MutableList<String>>()

    override fun debug(msg: () -> String) {
      logs.getOrPut(Level.FINE, ::mutableListOf).add(msg())
    }

    override fun info(msg: () -> String) {
      logs.getOrPut(Level.INFO, ::mutableListOf).add(msg())
    }

    override fun warning(msg: () -> String) {
      logs.getOrPut(Level.WARNING, ::mutableListOf).add(msg())
    }

    override fun error(t: Throwable, msg: () -> String) {
      logs.getOrPut(Level.SEVERE, ::mutableListOf).add(msg() + t)
    }

    override fun error(msg: () -> String) {
      logs.getOrPut(Level.SEVERE, ::mutableListOf).add(msg())
    }
  }

  @Test
  fun simpleSource() {
    val source = temp.resolve("Simple.kt").writeLines(
      listOf("class Simple")
    )

    val scope = TestScope(temp.resolve("working").createDirectories())
    val job = JobContext.of(scope)

    KotlinCompile().run(
      job, listOf(
        BaseConfiguration(
          TestInputs(
            sources = listOf(Artifact(source))
          )
        )
      )
    )

    assertThat(
      walk(job.classes).map(job.classes::relativize).map(Path::toString).toList()
    ).containsAtLeast("Simple.class", "META-INF/test.kotlin_module")
  }


  @Test
  fun syntaxError() {
    val source = temp.resolve("Simple.kt").writeLines(
      listOf("class Simple --")
    )

    val scope = TestScope(temp.resolve("working").createDirectories())
    val job = JobContext.of(scope)

    assertThat(
      KotlinCompile().run(
        job, listOf(
          BaseConfiguration(
            TestInputs(
              sources = listOf(Artifact(source))
            )
          )
        )
      )
    ).isEqualTo(Status.ERROR)

    assertThat(scope.logs[Level.SEVERE]).contains(
      "$source:1:14: error: expecting a top level declaration" +
        "\nclass Simple --" +
        "\n             ^"
    )
  }

  @Test
  fun withJavaSource() {
    val java = temp.resolve("HasJava.java").writeLines(
      listOf("interface HasJava {}")
    )
    val kotlin = temp.resolve("KotlinClass.kt").writeLines(
      listOf("class KotlinClass : HasJava")
    )

    val scope = TestScope(temp.resolve("working").createDirectories())
    val job = JobContext.of(scope)

    assertThat(
      KotlinCompile().run(
        job, listOf(
          BaseConfiguration(
            TestInputs(
              sources = listOf(Artifact(kotlin), Artifact(java))
            )
          )
        )
      )
    ).isEqualTo(Status.SUCCESS)

    assertThat(
      walk(job.classes).map(job.classes::relativize).map(Path::toString).toList()
    ).apply{
      containsAtLeast("KotlinClass.class", "META-INF/test.kotlin_module")
      doesNotContain("HasJava.class")
    }
  }
}
