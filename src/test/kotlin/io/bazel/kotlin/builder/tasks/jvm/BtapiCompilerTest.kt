package io.bazel.kotlin.builder.tasks.jvm

import com.google.common.truth.Truth.assertThat
import io.bazel.kotlin.builder.Deps
import io.bazel.kotlin.builder.DirectoryType
import io.bazel.kotlin.builder.KotlinJvmTestBuilder
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
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
  }

  @Test
  fun `encodeMapForKapt produces decodable Base64`() {
    // Use reflection to call the private encodeMapForKapt method
    val compiler = BtapiCompiler::class.java
    val method = compiler.getDeclaredMethod(
      "encodeMapForKapt",
      Map::class.java,
    )
    method.isAccessible = true

    // Create a BtapiCompiler instance - we need toolchains for construction, but we only
    // need to call the static-like encode method.
    // Since BtapiCompiler requires toolchains, use a helper to get one.
    val toolchainsCache = io.bazel.kotlin.builder.toolchain.BtapiToolchainsCache()
    val runtimeSpec = io.bazel.kotlin.builder.toolchain.BtapiRuntimeSpec(
      listOf(
        java.nio.file.Path.of(Deps.Dep.fromLabel("@rules_kotlin_maven//:org_jetbrains_kotlin_kotlin_build_tools_impl").singleCompileJar()),
        java.nio.file.Path.of(Deps.Dep.fromLabel("@rules_kotlin_maven//:org_jetbrains_kotlin_kotlin_compiler_embeddable").singleCompileJar()),
        java.nio.file.Path.of(Deps.Dep.fromLabel("@rules_kotlin_maven//:org_jetbrains_kotlin_kotlin_daemon_client").singleCompileJar()),
        java.nio.file.Path.of(Deps.Dep.fromLabel("//kotlin/compiler:kotlin-stdlib").singleCompileJar()),
        java.nio.file.Path.of(Deps.Dep.fromLabel("//kotlin/compiler:kotlin-reflect").singleCompileJar()),
        java.nio.file.Path.of(Deps.Dep.fromLabel("//kotlin/compiler:kotlinx-coroutines-core-jvm").singleCompileJar()),
        java.nio.file.Path.of(Deps.Dep.fromLabel("//kotlin/compiler:annotations").singleCompileJar()),
      ),
    )
    val toolchains = toolchainsCache.get(runtimeSpec)
    val btapiCompiler = BtapiCompiler(toolchains)

    val inputMap = mapOf(
      "-target" to "11",
      "-source" to "11",
      "custom.key" to "custom.value",
    )

    val encoded = method.invoke(btapiCompiler, inputMap) as String

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

    btapiCompiler.close()
  }
}
