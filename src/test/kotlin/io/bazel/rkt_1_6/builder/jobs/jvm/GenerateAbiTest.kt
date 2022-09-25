package io.bazel.rkt_1_6.builder.jobs.jvm

import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import io.bazel.kotlin.builder.utils.BazelRunFiles.resolveFromProperty
import io.bazel.kotlin.integration.WriteWorkspace
import io.bazel.rkt_1_6.builder.jobs.jvm.CompileConfigurationSubject.Companion.configurations
import io.bazel.kotlin.builder.jobs.jvm.configurations.CompileKotlin
import io.bazel.kotlin.builder.jobs.jvm.configurations.GenerateAbi
import org.junit.Test
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.toList

class GenerateAbiTest {
  private val temp = Files.createTempDirectory(GenerateAbiTest::class.qualifiedName)
  data class In(
    override val apiVersion: String = "1.6",
    override val languageVersion: String = "1.6",
    override val jvmTarget: String = "11",
    override val moduleName: String = "test",
    override val reducedClasspathMode: Boolean = false,
    override val depsArtifacts: List<Path> = emptyList(),
    override val classpath: List<Path> = emptyList(),
    override val fileSystem: FileSystem = FileSystems.getDefault(),
    override val sources: List<Path> = emptyList(),
    override val useIr: Boolean = false,
    override val debug: List<String> = listOf("trace", "debug"),
    override val sourcesFromJars: List<Path> = emptyList(),
    override val jdkHome: Path = resolveFromProperty("java.home"),
    override val abi: Path = resolveFromProperty("abi_plugin"),
    override val passthroughFlags: List<String> = emptyList(),
  ) : CompileKotlin.In, GenerateAbi.In

  data class Out(
    override val outputSrcJar: Path? = null,
    override val output: Path? = null,
    override val abiJar: Path? = null
  ) : CompileKotlin.Out, GenerateAbi.Out

  val workspace = WriteWorkspace.using<GenerateAbiTest> {
    kotlin("upstream/A.kt") {
      `package`("upstream")
      `class`("A") {
        `fun`("doWork") {
          "innerWork"()
        }
        "private fun innerWork()" {
          "println"("Wocka!")
        }
      }
    }
    kotlin("downstream/B.kt") {
      `package`("downstream")
      `import`("upstream.A")
      `class`("B") {
        "val a" eq "A()"
        `fun`("doWork") {}
      }
    }
  }

  @Test
  fun singleSource() {
    val source = workspace.resolve("upstream/A.kt")
    assertAbout(configurations)
      .that(CompileKotlin(), GenerateAbi(), inDirectory = temp)
      .canCompile(
        In(sources = listOf(source)),
        Out(
          output = temp.resolve("class.jar"),
          abiJar = temp.resolve("abi.jar")
        )
      ).successfully().and { out ->
        out.abiJar.onFiles {
          filter{ it.toString().endsWith("A.class") }
            .forEach { path ->
              assertAboutByteCodeIn(path) {
                thatAsmText().doesNotContain("private final innerWork()")
              }
            }
        }
        out.output.onFiles {
          filter{ it.toString().endsWith("A.class") }
            .forEach { path ->
              assertAboutByteCodeIn(path) {
                thatAsmText().contains("private final innerWork()")
              }
            }
        }
        assertThat(out.abiJar.streamPaths().toList()).containsAtLeast(
          "upstream/A.class",
          "META-INF/test.kotlin_module"
        )
      }
  }

  @Test
  fun dependencies() {
    val sourceA = workspace.resolve("upstream/A.kt")
    val sourceB = workspace.resolve("downstream/B.kt")
    assertAbout(configurations)
      .that(CompileKotlin(), GenerateAbi(), inDirectory = temp) {
        canCompile(
          In(sources = listOf(sourceA)),
          Out(
            abiJar = temp.resolve("a-abi.jar"),
            outputSrcJar = temp.resolve("a.srcjar")
          ),
        ).successfully().and { abiOut ->
          assertWithMessage("abiJar")
            .that(abiOut.abiJar)
            .isNotNull()
          assertWithMessage("outputSrcJar")
            .that(abiOut.outputSrcJar.streamPaths().toList())
            .contains("upstream/A.kt")

          canCompile(
            In(sources = listOf(sourceB), classpath = abiOut.abiJar.asListOf()),
            Out(output = temp.resolve("b-compiled.jar"))
          ).successfully().and { compiledOut ->
            assertThat(compiledOut.output.streamPaths().toList())
              .containsAtLeast(
                "downstream/B.class",
                "META-INF/test.kotlin_module"
              )
          }
        }
      }
  }
}
