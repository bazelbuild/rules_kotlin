package io.bazel.rkt_1_6.builder.jobs.jvm

import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import io.bazel.kotlin.builder.jobs.jvm.configurations.CompileKotlin
import io.bazel.kotlin.builder.jobs.jvm.configurations.CompileWithAssociates
import io.bazel.kotlin.integration.WriteWorkspace
import org.junit.Test
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.streams.toList

class CompileWithAssociatesTest {
  companion object {
    val javaHome = System.getProperty("java_home_files")
      .split(" ")
      .asSequence()
      .map(FileSystems.getDefault()::getPath)
      .reduce { acc, path ->
        acc.zip(path)
          .takeWhile { (a, b) -> (a == b) }
          .map { it.first }
          .reduce(Path::resolve)
      }
  }


  class TestIn(
    override val apiVersion: String = "1.6",
    override val languageVersion: String = "1.6",
    override val jvmTarget: String = "11",
    override val moduleName: String = "test",
    override val reducedClasspathMode: Boolean = false,
    override val depsArtifacts: List<Path> = emptyList(),
    override val classpath: List<Path> = emptyList(),
    override val fileSystem: FileSystem = FileSystems.getDefault(),
    override val sources: List<Path> = emptyList(),
    override val jdkHome: Path = javaHome,
    override val useIr: Boolean = false,
    override val debug: List<String> = emptyList(),
    override val sourcesFromJars: List<Path> = emptyList(),
    override val passthroughFlags: List<String> = emptyList(),
    override val associatePaths: List<Path> = emptyList(),
  ) : CompileWithAssociates.In

  class TestOut(override val outputSrcJar: Path?, override val output: Path?) :
    CompileWithAssociates.Out

  private val temp = Files.createTempDirectory(CompileWithAssociatesTest::class.qualifiedName)

  val workspace = WriteWorkspace.using<CompileWithAssociatesTest> {
    kotlin("AnAssociate.kt") {
      `package`("consulting")
      `class`("AnAssociate") {
        "internal fun adviseOn"(!"work:String") {
          "println"(!"work")
        }
      }
    }

    kotlin("Organization.kt") {
      `package`("direct")
      `import`("consulting.AnAssociate")
      `class`("Organization") {
        "val associate" eq !"AnAssociate()"

        "fun directive(work:String)" {
          "associate.adviseOn"(!"work")
        }
      }
    }
  }

  @Test
  fun withAssociates() {
    assertAbout(CompileConfigurationSubject.configurations)
      .that(CompileKotlin(), CompileWithAssociates(), inDirectory = temp) {
        canCompile(
          TestIn(
            sources = listOf(workspace.resolve("AnAssociate.kt")),
            moduleName = "consulting",
          ),
          TestOut(
            output = temp.resolve("associate.jar"),
            outputSrcJar = temp.resolve("associate.srcjar"),
          ),
        ).successfully().and { associateArtifacts ->
          canCompile(
            TestIn(
              sources = listOf(workspace.resolve("Organization.kt")),
              moduleName = "direct",
              classpath = associateArtifacts.output.asListOf(),
              associatePaths = associateArtifacts.output.asListOf(),
            ),
            TestOut(
              output = temp.resolve("org.jar"),
              outputSrcJar = temp.resolve("org.srcjar"),
            ),
          ).successfully().and { organizationArtifacts ->
            assertThat(organizationArtifacts.output.streamPaths().toList()).apply {
              containsAtLeast(
                "direct/Organization.class", "META-INF/direct.kotlin_module",
              )
            }
          }
        }
      }
  }
}
