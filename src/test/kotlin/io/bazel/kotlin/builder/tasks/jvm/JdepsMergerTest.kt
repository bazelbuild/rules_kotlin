package io.bazel.kotlin.builder.tasks.jvm

import com.google.common.truth.Truth.assertThat
import com.google.devtools.build.lib.view.proto.Deps
import com.google.devtools.build.lib.view.proto.Deps.Dependency
import io.bazel.kotlin.builder.DaggerJdepsMergerTestComponent
import io.bazel.kotlin.builder.tasks.InvocationWorker
import io.bazel.kotlin.builder.tasks.WorkerIO
import io.bazel.kotlin.builder.utils.Flag
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.nio.file.Files
import java.nio.file.Path

@RunWith(JUnit4::class)
class JdepsMergerTest {

  private val wrkDir = Files.createTempDirectory("JdepsMergerEnvironmentTest")

  private fun jdeps(path: String, buildDeps: (Deps.Dependencies.Builder.() -> Deps.Dependencies.Builder)): Path {
    val jdepsPath = Files.createDirectories(wrkDir.resolve("jdeps")).resolve(path)

    BufferedOutputStream(Files.newOutputStream(jdepsPath)).use {
      Deps.Dependencies.newBuilder().buildDeps().build().writeTo(it)
    }

    return jdepsPath
  }

  private fun out(name: String): Path {
    return Files.createDirectories(wrkDir.resolve("out")).resolve(name)
  }

  @Test
  fun `merge all deps`() {
    val merger = DaggerJdepsMergerTestComponent.builder().build().jdepsMerger()

    val kotlinJdeps = jdeps("kt.jdeps") {
      addDependency(with(Dependency.newBuilder()) {
        kind = Dependency.Kind.EXPLICIT
        path = "/path/to/kt_dep.jar"
        build()
      })
    }

    val javaJdeps = jdeps("java.jdeps") {
      addDependency(with(Dependency.newBuilder()) {
        kind = Dependency.Kind.EXPLICIT
        path = "/path/to/java_dep.jar"
        build()
      })
    }

    val mergedJdeps = out("merged.jdeps")

    WorkerIO.open().use { io ->
      val worker = InvocationWorker(io, merger)
      assertThat(worker.run(args {
        flag(JdepsMerger.Companion.JdepsMergerFlags.TARGET_LABEL, "//foo/bar:baz")
        input(kotlinJdeps)
        input(javaJdeps)
        flag(JdepsMerger.Companion.JdepsMergerFlags.OUTPUT, mergedJdeps)
      })).isEqualTo(0)
    }

    val depsProto = depsProto(mergedJdeps)
    assertThat(depsProto.ruleLabel).isEqualTo("//foo/bar:baz")
    assertThat(depsProto.dependencyList.map { it.path }).containsExactly("/path/to/kt_dep.jar", "/path/to/java_dep.jar")
  }

  @Test
  fun `merge conflicting deps`() {
    val merger = DaggerJdepsMergerTestComponent.builder().build().jdepsMerger()

    val kotlinJdeps = jdeps("kt.jdeps") {
      addDependency(with(Dependency.newBuilder()) {
        kind = Dependency.Kind.UNUSED
        path = "/path/to/shared_dep.jar"
        build()
      })
    }

    val javaJdeps = jdeps("java.jdeps") {
      addDependency(with(Dependency.newBuilder()) {
        kind = Dependency.Kind.EXPLICIT
        path = "/path/to/shared_dep.jar"
        build()
      })
    }

    val mergedJdeps = out("merged.jdeps")

    WorkerIO.open().use { io ->
      val worker = InvocationWorker(io, merger)
      assertThat(worker.run(args {
        flag(JdepsMerger.Companion.JdepsMergerFlags.TARGET_LABEL, "//foo/bar:baz")
        input(kotlinJdeps)
        input(javaJdeps)
        flag(JdepsMerger.Companion.JdepsMergerFlags.OUTPUT, mergedJdeps)
      })).isEqualTo(0)
    }

    val depsProto = depsProto(mergedJdeps)
    assertThat(depsProto.dependencyList.map { it.path }).containsExactly("/path/to/shared_dep.jar")
    assertThat(depsProto.dependencyList.map { it.kind }).containsExactly(Dependency.Kind.EXPLICIT)
  }


  private fun depsProto(mergedJdeps: Path) =
    Deps.Dependencies.parseFrom(BufferedInputStream(Files.newInputStream(mergedJdeps)))

  private fun args(init: ArgsBuilder.() -> Unit) = with(ArgsBuilder()) {
    init()
    list()
  }

  class ArgsBuilder(val args: MutableMap<Flag, MutableList<String>> = mutableMapOf()) {
    fun flag(flag: Flag, value: String) {
      args[flag] = (args[flag] ?: mutableListOf()).also {
        it.add(value)
      }
    }

    fun flag(flag: Flag, p: Path) {
      flag(flag, p.toString())
    }

    fun input(src: Path) {
      flag(JdepsMerger.Companion.JdepsMergerFlags.INPUTS, src.toString())
    }

    fun list(): List<String> {
      return args.flatMap { entry ->
        entry.value.flatMap { value ->
          listOf(entry.key.flag, value)
        }
      }
    }
  }

}
