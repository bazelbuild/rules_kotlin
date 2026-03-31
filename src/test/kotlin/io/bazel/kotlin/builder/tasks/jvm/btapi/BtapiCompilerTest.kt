package io.bazel.kotlin.builder.tasks.jvm.btapi

import com.google.common.truth.Truth.assertThat
import io.bazel.kotlin.builder.KotlinAbstractTestBuilder
import io.bazel.kotlin.model.CompilationTaskInfo
import io.bazel.kotlin.model.JvmCompilationTask
import io.bazel.kotlin.model.KotlinToolchainInfo
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.nio.file.Files
import java.nio.file.Path

@RunWith(JUnit4::class)
class BtapiCompilerTest {
  @Test
  fun `kapt and legacy stubs expansion use temp stubs directory`() {
    val root = Files.createTempDirectory("btapi-stubs-test")
    val cache = BtapiCompilerCache()
    val toolchainSpec = KotlinAbstractTestBuilder.toolchainSpecForTest()

    try {
      val expectedStubsDir = root.resolve("temp").resolve("stubs")
      val task =
        task(root)
          .toBuilder()
          .setInputs(
            JvmCompilationTask.Inputs
              .newBuilder()
              .addStubsPlugins("plugin.id")
              .addStubsPluginClasspath(toolchainSpec.btapiClasspath.first().toString())
              .addStubsPluginOptions("plugin.id:path={stubs}")
              .build(),
          ).build()

      val compiler = cache[toolchainSpec]
      val kaptPlugin = compiler.buildKaptCompilerPlugin(task, aptMode = "stubsAndApt", verbose = false)
      val legacyPlugin = compiler.buildStubsPlugins(task).single()

      assertThat(task.directories.generatedStubClasses).isEqualTo(root.resolve("builder-stubs").toString())
      assertThat(kaptPlugin.rawArguments.single { it.key == "stubs" }.value).isEqualTo(expectedStubsDir.toString())
      assertThat(legacyPlugin.rawArguments.single { it.key == "path" }.value).isEqualTo(expectedStubsDir.toString())
      assertThat(Files.isDirectory(expectedStubsDir)).isTrue()
    } finally {
      cache.close()
      root.toFile().deleteRecursively()
    }
  }

  private fun task(root: Path): JvmCompilationTask =
    JvmCompilationTask
      .newBuilder()
      .setInfo(
        CompilationTaskInfo
          .newBuilder()
          .setLabel("//test:btapi")
          .setModuleName("btapi")
          .setToolchainInfo(
            KotlinToolchainInfo
              .newBuilder()
              .setCommon(
                KotlinToolchainInfo.Common
                  .newBuilder()
                  .setApiVersion("2.0")
                  .setLanguageVersion("2.0")
                  .build(),
              )
              .setJvm(
                KotlinToolchainInfo.Jvm
                  .newBuilder()
                  .setJvmTarget("11")
                  .build(),
              )
              .build(),
          )
          .build(),
      )
      .setDirectories(
        JvmCompilationTask.Directories
          .newBuilder()
          .setGeneratedClasses(root.resolve("generated-classes").toString())
          .setGeneratedJavaSources(root.resolve("generated-java-sources").toString())
          .setGeneratedSources(root.resolve("generated-sources").toString())
          .setGeneratedStubClasses(root.resolve("builder-stubs").toString())
          .setTemp(root.resolve("temp").toString())
          .build(),
      )
      .build()
}
