package io.bazel.kotlin.builder.tasks.jvm

import com.google.devtools.build.lib.view.proto.Deps as BazelJdeps
import com.google.common.truth.Truth.assertThat
import io.bazel.kotlin.builder.Deps
import io.bazel.kotlin.builder.DirectoryType
import io.bazel.kotlin.builder.KotlinAbstractTestBuilder
import io.bazel.kotlin.builder.KotlinJvmTestBuilder
import io.bazel.kotlin.builder.toolchain.ToolchainSpec
import io.bazel.kotlin.model.JvmCompilationTask
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.SharedApiClassesClassLoader
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPlugin
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayInputStream
import java.io.File
import java.io.ObjectInputStream
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import java.util.function.Consumer
import java.util.jar.JarFile

@OptIn(ExperimentalBuildToolsApi::class)
@RunWith(JUnit4::class)
class BtapiCompilerTest {
  private val ctx = KotlinJvmTestBuilder()

  @Test
  fun `compile succeeds with valid Kotlin source`() {
    ctx.runCompileTask(Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "AClass.kt",
        "package test",
        "class AClass { fun greet() = \"hello\" }",
      )
      c.outputJar()
      c.outputJdeps()
      c.compileKotlin()
    })
    ctx.assertFilesExist(DirectoryType.CLASSES, "test/AClass.class")
  }

  @Test
  fun `compile reports error for invalid Kotlin source`() {
    ctx.runFailingCompileTaskAndValidateOutput(
      {
        ctx.runCompileTask(Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
          c.addSource(
            "Broken.kt",
            "package test",
            "class Broken {",
          )
          c.outputJar()
          c.outputJdeps()
          c.compileKotlin()
        })
      },
    ) { lines: List<String?> ->
      assertThat(lines.any { it?.contains("Broken.kt") == true }).isTrue()
    }
  }

  @Test
  fun `friend paths allow access to internal members`() {
    // First compile a "friend" module with internal members
    val friendDep = ctx.runCompileTask(Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "FriendClass.kt",
        "package friend",
        "internal class FriendInternal { fun secret() = 42 }",
        "class FriendPublic",
      )
      c.outputJar()
      c.outputJdeps()
      c.compileKotlin()
    })

    // Then compile a second module that references the internal member with friend path set.
    // This should succeed because friend paths grant internal visibility.
    ctx.runCompileTask(Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "Consumer.kt",
        "package consumer",
        "import friend.FriendInternal",
        "class Consumer { fun use() = FriendInternal().secret() }",
      )
      c.outputJar()
      c.outputJdeps()
      c.compileKotlin()
      c.addDirectDependencies(friendDep)
      // Use the friend dep's compile jar as a friend path
      c.addFriendPaths(*friendDep.compileJars().toTypedArray())
    })
  }

  @Test
  fun `jdeps plugin produces jdeps output when configured`() {
    ctx.runCompileTask(Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "AClass.kt",
        "package test",
        "class AClass",
      )
      c.outputJar()
      c.outputJdeps()
      c.compileKotlin()
    })
    // outputJdeps() assertion is built into runCompileTask — it asserts the jdeps file exists
  }

  @Test
  fun `abi jar plugin produces abi jar when configured`() {
    val dep = ctx.runCompileTask(Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "AClass.kt",
        "package test",
        "class AClass { fun greet() = \"hello\" }",
      )
      c.outputJar()
      c.outputAbiJar()
      c.outputJdeps()
      c.compileKotlin()
    })

    // Verify ABI jar exists and contains class entries
    val abiJarPath = dep.compileJars().first { it.endsWith("abi.jar") }
    JarFile(abiJarPath).use { jar ->
      val classEntries = jar.entries().asSequence().filter { it.name.endsWith(".class") }.toList()
      assertThat(classEntries).isNotEmpty()
      assertThat(classEntries.any { it.name.contains("AClass") }).isTrue()
    }
  }

  @Test
  fun `compile with multiple sources produces all class files`() {
    ctx.runCompileTask(Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "First.kt",
        "package multi",
        "class First",
      )
      c.addSource(
        "Second.kt",
        "package multi",
        "class Second",
      )
      c.outputJar()
      c.outputJdeps()
      c.compileKotlin()
    })
    ctx.assertFilesExist(DirectoryType.CLASSES, "multi/First.class", "multi/Second.class")
  }

  @Test
  fun `compile with dependency on previous compilation`() {
    val depA = ctx.runCompileTask(Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "Base.kt",
        "package base",
        "open class Base { open fun value() = 1 }",
      )
      c.outputJar()
      c.outputJdeps()
      c.compileKotlin()
    })

    ctx.runCompileTask(Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "Derived.kt",
        "package derived",
        "import base.Base",
        "class Derived : Base() { override fun value() = 2 }",
      )
      c.addDirectDependencies(depA)
      c.outputJar()
      c.outputJdeps()
      c.compileKotlin()
    })
    ctx.assertFilesExist(DirectoryType.CLASSES, "derived/Derived.class")
  }

  @Test
  fun `compileKapt runs annotation processing`() {
    val autoValueAnnotations = Deps.Dep.fromLabel("auto_value_annotations")
    val autoValue = Deps.Dep.fromLabel("auto_value")
    val kotlinAnnotations = KotlinJvmTestBuilder.KOTLIN_ANNOTATIONS
    val kotlinStdlib = KotlinJvmTestBuilder.KOTLIN_STDLIB

    val annotationProcessor = Deps.AnnotationProcessor.builder()
      .processClass("com.google.auto.value.processor.AutoValueProcessor")
      .processorPath(
        Deps.Dep.classpathOf(autoValueAnnotations, autoValue, kotlinAnnotations)
          .collect(java.util.stream.Collectors.toSet()),
      )
      .build()

    ctx.runCompileTask(
      Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
        c.addAnnotationProcessors(annotationProcessor)
        c.addDirectDependencies(autoValueAnnotations, kotlinAnnotations, kotlinStdlib)
      },
      Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
        c.addSource(
          "TestValue.kt",
          """
            package autovalue

            import com.google.auto.value.AutoValue

            @AutoValue
            abstract class TestValue {
                abstract fun name(): String
                fun builder(): Builder = AutoValue_TestValue.Builder()

                @AutoValue.Builder
                abstract class Builder {
                    abstract fun setName(name: String): Builder
                    abstract fun build(): TestValue
                }
            }
          """,
        )
        c.generatedSourceJar()
        c.ktStubsJar()
        c.incrementalData()
      },
    )
    ctx.assertFilesExist(DirectoryType.JAVA_SOURCE_GEN, "autovalue/AutoValue_TestValue.java")
    ctx.assertFilesExist(DirectoryType.GENERATED_STUBS, "autovalue/TestValue.java")
    ctx.assertFilesExist(DirectoryType.INCREMENTAL_DATA, "autovalue/TestValue.class")
  }

  @Test
  fun `expands legacy plugin option templates for btapi plugins`() {
    val tempDir = Files.createTempDirectory("btapi-plugin-options")
    val stubsDir = tempDir.resolve("stubs")
    val directories =
      JvmCompilationTask.Directories.newBuilder()
        .setGeneratedClasses(tempDir.resolve("generated-classes").toString())
        .setGeneratedSources(tempDir.resolve("generated-sources").toString())
        .setGeneratedStubClasses(stubsDir.toString())
        .setTemp(tempDir.toString())
        .build()
    val task =
      JvmCompilationTask.newBuilder()
        .setDirectories(directories)
        .build()
    val classpath = listOf("/plugins/first.jar", "/plugins/second.jar")
    val rawOptions =
      listOf(
        "expanded=" + listOf(
          "classes={generatedClasses}",
          "sources={generatedSources}",
          "stubs={stubs}",
          "temp={temp}",
          "classpath={classpath}",
        ).joinToString(";"),
      )

    val btapiPlugin =
      withBtapiCompiler { btapiCompiler ->
        btapiCompiler.toBtapiPlugin(task, "example.plugin", classpath, rawOptions)
      }
    val expectedValue =
      listOf(
        "classes=${tempDir.resolve("generated-classes")}",
        "sources=${tempDir.resolve("generated-sources")}",
        "stubs=$stubsDir",
        "temp=$tempDir",
        "classpath=/plugins/first.jar${File.pathSeparator}/plugins/second.jar",
      ).joinToString(";")

    assertThat(BtapiPluginArguments.toArgumentStrings(listOf(btapiPlugin))).containsExactly(
      "-Xplugin=/plugins/first.jar,/plugins/second.jar",
      "-P",
      "plugin:example.plugin:expanded=$expectedValue",
    ).inOrder()
    assertThat(Files.isDirectory(stubsDir)).isTrue()
  }

  @Test
  fun `compileKapt and stubs phase plugins share the canonical stubs dir`() {
    val tempDir = Files.createTempDirectory("btapi-kapt-stubs")
    val stubsDir = tempDir.resolve("stubs")
    val task =
      minimalTaskBuilder(tempDir)
        .apply {
          inputsBuilder.addStubsPlugins("example.stubs")
          inputsBuilder.addStubsPluginClasspath("/plugins/example-stubs.jar")
          inputsBuilder.addStubsPluginOptions("example.stubs:dir={stubs}")
          directoriesBuilder.generatedStubClasses = stubsDir.toString()
        }.build()

    withBtapiCompiler { btapiCompiler ->
      val kaptPlugin =
        btapiCompiler.buildKaptCompilerPlugin(task, toolchainSpec(), "stubsAndApt", false)
      val stubsPlugins =
        btapiCompiler.buildStubsPlugins(task, toolchainSpec())

      assertThat(kaptPlugin.rawArguments.single { it.key == "stubs" }.value).isEqualTo(stubsDir.toString())
      assertThat(stubsPlugins).hasSize(1)
      assertThat(stubsPlugins.single().rawArguments.single().value).isEqualTo(stubsDir.toString())
    }

    assertThat(Files.isDirectory(stubsDir)).isTrue()
  }

  @Test
  fun `stubs plugins preserve no-option plugin ids without requiring marker options`() {
    val tempDir = Files.createTempDirectory("btapi-stubs-no-options")
    val task =
      minimalTaskBuilder(tempDir)
        .apply {
          inputsBuilder.addStubsPlugins("example.noop")
          inputsBuilder.addStubsPluginClasspath("/plugins/example-noop.jar")
        }.build()

    withBtapiCompiler { btapiCompiler ->
      val stubsPlugins = btapiCompiler.buildStubsPlugins(task, toolchainSpec())

      assertThat(stubsPlugins).hasSize(1)
      assertThat(stubsPlugins.single().pluginId).isEqualTo("example.noop")
      assertThat(stubsPlugins.single().rawArguments).isEmpty()
    }
  }

  @Test
  fun `reduced classpath prefers friend jars and explicit deps only`() {
    val tempDir = Files.createTempDirectory("btapi-reduced-classpath")
    val generatedClasses = tempDir.resolve("generated-classes").toString()
    val jdepsFile = tempDir.resolve("deps.jdeps")
    Files.newOutputStream(jdepsFile).use { out ->
      BazelJdeps.Dependencies.newBuilder()
        .addDependency(
          BazelJdeps.Dependency.newBuilder()
            .setPath("/deps/explicit.jar")
            .setKind(BazelJdeps.Dependency.Kind.EXPLICIT)
            .build(),
        ).addDependency(
          BazelJdeps.Dependency.newBuilder()
            .setPath("/deps/implicit.jar")
            .setKind(BazelJdeps.Dependency.Kind.IMPLICIT)
            .build(),
        ).build()
        .writeTo(out)
    }
    val task =
      minimalTaskBuilder(tempDir)
        .apply {
          infoBuilder.reducedClasspathMode = "KOTLINBUILDER_REDUCED"
          infoBuilder.addFriendPaths("/deps/friend.jar")
          inputsBuilder
            .addClasspath("/deps/ignored.jar")
            .addDirectDependencies("/deps/friend.jar")
            .addDirectDependencies("/deps/direct.jar")
            .addDepsArtifacts(jdepsFile.toString())
        }.build()

    val classpath =
      withBtapiCompiler { btapiCompiler ->
        btapiCompiler.computeClasspath(task)
      }

    assertThat(classpath).containsExactly(
      "/deps/friend.jar",
      "/deps/direct.jar",
      "/deps/explicit.jar",
      generatedClasses,
    ).inOrder()
  }

  @Test
  fun `configureCompilerArguments gives typed settings precedence over valid passthrough flags`() {
    val task =
      minimalTaskBuilder()
        .apply {
          infoBuilder.addAllPassthroughFlags(
            listOf(
              "-module-name",
              "from_passthrough",
              "-jvm-target",
              "1.8",
              "-language-version",
              "1.9",
              "-api-version",
              "1.9",
            ),
          )
        }.build()

    val arguments =
      withBtapiCompiler { btapiCompiler ->
        configureAndBuildArguments(btapiCompiler, task)
      }

    assertThat(argumentValue(arguments, "-module-name")).isEqualTo("test_module")
    assertThat(argumentValue(arguments, "-jvm-target")).isEqualTo("11")
    assertThat(argumentValue(arguments, "-language-version")).isEqualTo("2.0")
    assertThat(argumentValue(arguments, "-api-version")).isEqualTo("2.0")
  }

  @Test
  fun `configureCompilerArguments rejects invalid passthrough values`() {
    val task =
      minimalTaskBuilder()
        .apply {
          infoBuilder.addAllPassthroughFlags(
            listOf(
              "-jvm-target",
              "8",
            ),
          )
        }.build()

    val thrown =
      assertThrows(IllegalArgumentException::class.java) {
        withBtapiCompiler { btapiCompiler ->
          configureAndBuildArguments(btapiCompiler, task)
        }
      }

    assertThat(thrown).hasMessageThat().contains("Invalid passthrough flag")
  }

  @Test
  fun `configureCompilerArguments rejects unsupported jvm target`() {
    val task =
      minimalTaskBuilder()
        .apply {
          infoBuilder.toolchainInfoBuilder.jvmBuilder.jvmTarget = "999"
        }.build()

    val thrown =
      assertThrows(IllegalArgumentException::class.java) {
        withBtapiCompiler { btapiCompiler ->
          configureAndBuildArguments(btapiCompiler, task)
        }
      }

    assertThat(thrown).hasMessageThat().contains("Unsupported kotlin_jvm_target '999'")
  }

  @Test
  fun `configureCompilerArguments rejects unsupported kotlin versions`() {
    val task =
      minimalTaskBuilder()
        .apply {
          infoBuilder.toolchainInfoBuilder.commonBuilder.apiVersion = "999"
        }.build()

    val thrown =
      assertThrows(IllegalArgumentException::class.java) {
        withBtapiCompiler { btapiCompiler ->
          configureAndBuildArguments(btapiCompiler, task)
        }
      }

    assertThat(thrown).hasMessageThat().contains("Unsupported kotlin_api_version '999'")
  }

  @Test
  fun `encodeMapForKapt produces decodable Base64`() {
    val inputMap = mapOf(
      "-target" to "11",
      "-source" to "11",
      "custom.key" to "custom.value",
    )

    val encoded =
      withBtapiCompiler { btapiCompiler ->
        btapiCompiler.encodeMapForKapt(inputMap)
      }

    // Decode and verify round-trip fidelity
    val decoded = Base64.getDecoder().decode(encoded)
    val ois = ObjectInputStream(ByteArrayInputStream(decoded))
    val size = ois.readInt()
    assertThat(size).isEqualTo(3)

    val resultMap = mutableMapOf<String, String>()
    repeat(size) {
      val key = ois.readUTF()
      val value = ois.readUTF()
      resultMap[key] = value
    }

    assertThat(resultMap).isEqualTo(inputMap)
  }

  private fun minimalTaskBuilder(tempDir: Path = Files.createTempDirectory("btapi-task")) =
    JvmCompilationTask.newBuilder().apply {
      val tempPath = tempDir.resolve("temp")
      infoBuilder.apply {
        moduleName = "test_module"
        toolchainInfoBuilder.jvmBuilder.jvmTarget = "11"
        toolchainInfoBuilder.commonBuilder.apiVersion = "2.0"
        toolchainInfoBuilder.commonBuilder.languageVersion = "2.0"
      }
      directoriesBuilder.apply {
        classes = tempDir.resolve("classes").toString()
        generatedClasses = tempDir.resolve("generated-classes").toString()
        generatedSources = tempDir.resolve("generated-sources").toString()
        generatedJavaSources = tempDir.resolve("generated-java-sources").toString()
        generatedStubClasses = tempPath.resolve("stubs").toString()
        temp = tempPath.toString()
      }
    }

  private inline fun <T> withBtapiCompiler(block: (BtapiCompiler) -> T): T {
    val spec = KotlinAbstractTestBuilder.toolchainSpecForTest()
    val urls = spec.btapiClasspath.map { it.toUri().toURL() }.toTypedArray()
    val classLoader = URLClassLoader(urls, SharedApiClassesClassLoader())
    val toolchains = KotlinToolchains.loadImplementation(classLoader)
    return BtapiCompiler(toolchains).use { block(it) }
  }

  private fun toolchainSpec() = KotlinAbstractTestBuilder.toolchainSpecForTest()

  private fun configureAndBuildArguments(
    btapiCompiler: BtapiCompiler,
    task: JvmCompilationTask,
  ): List<String> {
    val outputDir = Files.createTempDirectory("btapi-configure-output")
    val argsBuilder =
      btapiCompiler.toolchains.jvm
        .jvmCompilationOperationBuilder(emptyList<Path>(), outputDir)
        .compilerArguments
    btapiCompiler.configureCompilerArguments(argsBuilder, task)
    return argsBuilder.build().toArgumentStrings()
  }

  private fun argumentValue(arguments: List<String>, flag: String): String? {
    arguments.forEachIndexed { index, argument ->
      if (argument == flag) {
        return arguments.getOrNull(index + 1)
      }
      if (argument.startsWith("$flag=")) {
        return argument.substringAfter('=')
      }
    }
    return null
  }
}
