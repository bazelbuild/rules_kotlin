package io.bazel.kotlin.integration

import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Files.createTempDirectory
import java.nio.file.Files.newOutputStream
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE_NEW
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.text.Charsets.UTF_8

class MultiplexTest {

  private val rulesRepo = "rules_kotlin_release"

  private fun writeReleaseArchive(): Path {
    return MultiplexTest::class.java.classLoader.getResourceAsStream("_release.tgz")
      ?.let { stream ->
        createTempDirectory("rules_kotlin_release").resolve("rules_kotlin_release.tgz").also {
          newOutputStream(it, CREATE_NEW).buffered().write(stream.readAllBytes())
        }
      }
      ?: error("Cannot find release repo")
  }

  private fun WriteWorkspace.Workspace.defineArchiveRepository(): Pair<String, String> {
    starlark("archive_repository.bzl") {
      "def _archive_repository_impl"(!"repository_ctx") {
        "repository_ctx.extract"(
          "archive" to !"repository_ctx.attr.path",
        )
      }

      "archive_repository=repository_rule"(
        "implementation" to !"_archive_repository_impl",
        "attrs" to !"{'path': attr.string()}"
      )
    }
    return "@//:archive_repository.bzl" to "archive_repository"
  }

  private fun WriteWorkspace.BuildBazel.loadKtJvmLibrary(): String {
    load("@$rulesRepo//kotlin:jvm.bzl", "kt_jvm_library")
    return "kt_jvm_library"
  }

  private data class BuildResult(
    val code: Int,
    val out: String,
    val err: String,
  )

  private fun Path.build(vararg targets: String): BuildResult {
    val out = Files.createTempFile("out", "txt").toFile().apply {
      deleteOnExit()
    }
    val err = Files.createTempFile("out", "txt").toFile().apply {
      deleteOnExit()
    }

    return ProcessBuilder()
      .command(
        listOf(
          "bazel",
          "build",
          "--experimental_worker_max_multiplex_instances",
          "1"
        ) + targets
      )
      .redirectOutput(out)
      .redirectError(err)
      .directory(this.toFile())
      .start().runCatching {
        if (!waitFor(5, TimeUnit.MINUTES)) {
          error("build took too long:\nout: ${out.readText(UTF_8)}\nerr: ${err.readText(UTF_8)}")
        }
        BuildResult(
          code = exitValue(),
          out = out.readText(UTF_8),
          err = err.readText(UTF_8)
        )
      }
      .recover { exception ->
        BuildResult(
          code = 1,
          out = "",
          err = exception.toString()
        )
      }
      .getOrThrow()
  }

  class PkgName(parts: List<String>) : List<String> by parts {
    constructor(vararg parts: String) : this(parts.toList())

    private val pkg = when {
      size > 1 -> dropLast(1)
      else -> this
    }

    val jvmPackage = pkg.joinToString(".")
    val path = pkg.joinToString("/")
    val target = "//$path:${last()}"
    val className = last().replaceFirstChar(Char::uppercase)
    val classFile = "$className.kt"
    val qualifiedName = "$jvmPackage.$className"

    fun resolve(postfix: String): PkgName {
      return PkgName(plus(postfix))
    }

    fun append(postfix: String): PkgName {
      return PkgName(pkg + (last() + postfix.replaceFirstChar(Char::uppercase)))
    }

    override fun equals(other: Any?): Boolean {
      return (other as? PkgName)
        ?.let { it.toList() == toList() }
        ?: false
    }

    override fun hashCode(): Int {
      return path.hashCode()
    }
  }

  private fun <T> T.writeProjectGraph(graph: DependencyGraph)
    where T : WriteWorkspace.SubPackage<T>,
          T : WriteWorkspace.Package {
    graph.deps.forEach { (name, deps) ->
      lib(name, deps)
    }
  }

  fun <T> T.lib(
    name: PkgName,
    dependencies: Set<PkgName>
  ): WriteWorkspace.Resolve
    where T : WriteWorkspace.SubPackage<T>,
          T : WriteWorkspace.Package {
    return name.path {
      build {
        loadKtJvmLibrary()(
          "name" to name.last(),
          "srcs" to !"glob(['*.kt'])",
          "deps" to !"[${dependencies.joinToString(",") { '"' + it.target + '"' }}]",
          "visibility" to !"['//:__subpackages__']"
        )
      }

      kotlin(name.classFile) {
        `package`(name.jvmPackage)
        dependencies.forEach { dep ->
          `import`(dep.qualifiedName)
        }
        "class ${name.className}"  {
          dependencies.forEach { dep ->
            "val ${dep.last()}" eq dep.className()
          }
        }
      }
    }
  }

  private val pool = arrayOf(
    "boojum", "snark", "chortle", "frabjous", "galumph", "mimsy", "slithy", "vorpal"
  )

  private fun <T> Array<T>.random(): T = get(random.nextInt(0, size))
  private inline fun <reified T> Iterable<T>.random(): T {
    return toList().toTypedArray().random()
  }

  private val random = Random(1)

  private fun branches(minimum: Int = 0): Sequence<String> = sequence {
    repeat(minimum) { yield(pool.random()) }
    while (random.nextBoolean()) {
      yield(pool.random())
    }
  }

  private fun generateTree(root: PkgName): Sequence<PkgName> {
    return branches()
      .map { branch -> root.resolve(branch) }
      .distinct()
      .flatMap { pkg ->
        when (random.nextBoolean()) {
          true -> generateTree(pkg)
          false -> sequenceOf(pkg)
        }
      }
  }

  private data class DependencyGraph(
    val deps: Map<PkgName, Set<PkgName>>,
    val rDeps: Map<PkgName, Set<PkgName>>
  ) : Iterable<PkgName> by deps.keys {

    fun replace(old: PkgName, new: PkgName): DependencyGraph {
      val dependencies: Set<PkgName> = deps[old]!!

      val newDeps: Map<PkgName, Set<PkgName>> =
        deps.minus<PkgName, Set<PkgName>>(old).plus(new to dependencies)

      val newRDeps: Map<PkgName, Set<PkgName>> = rDeps.mapValues { (_, successors) ->
        successors.run {
          if (contains(old)) {
            minus<PkgName>(old).plus<PkgName>(new)
          } else {
            this
          }
        }
      }
      return DependencyGraph(newDeps, newRDeps)
    }
  }

  private fun Sequence<PkgName>.buildDepMap(): DependencyGraph {
    val pool = toList().toTypedArray()
    val backRefs = mutableMapOf<PkgName, MutableSet<PkgName>>()
    return DependencyGraph(
      deps = associate { name ->
        val dependencies = (0..random.nextInt())
          .map {
            pool.random()
          }
          .filter { it != name }
          .filterNot { backRefs[it]?.contains(name) ?: false }
          .toSet()
        // mark back refs to avoid cycles.
        dependencies.forEach { dep ->
          backRefs.compute(dep) { _, value ->
            (value ?: mutableSetOf()).apply { add(name) }
          }
        }
        name to dependencies
      },
      rDeps = backRefs
    )
  }


  @Test
  fun multipleBuilds() {
    var depsMap = branches(1).map { PkgName(it) }.flatMap { generateTree(it) }.buildDepMap()
    val workspace = WriteWorkspace.using<MultiplexTest> {
      build {
        val (define_kt_toolchain) = load("@$rulesRepo//kotlin:core.bzl", "define_kt_toolchain")
        define_kt_toolchain(
          "name" to "multiplex_toolchain",
          "experimental_multiplex_workers" to True
        )
      }

      val (location, archive_repository) = defineArchiveRepository()
      workspace {
        load(location, archive_repository)
        archive_repository(
          "name" to rulesRepo,
          "path" to writeReleaseArchive().toString()
        )
        load("@$rulesRepo//kotlin:repositories.bzl", "kotlin_repositories")
        "kotlin_repositories"()

        "register_toolchains"("//:multiplex_toolchain")
      }
      writeProjectGraph(depsMap)

    }

    (0..50).forEach { next ->
      workspace.build("//...:all").run {
        assertWithMessage("Out:\n$out\nErr:\n$err").that(code).isEqualTo(0)
        println("$next:\n$err\n\n")
      }
      WriteWorkspace.open(workspace) {
        depsMap.random().let { name ->
          val renamed = name.append(pool.random())
          name.path {
            remove(name.classFile)
          }
          depsMap = depsMap.replace(name, renamed)
          writeProjectGraph(depsMap)
        }
      }
    }
  }
}
