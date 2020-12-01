package io.bazel.kotlin.builder.tasks.jvm

import com.google.devtools.build.lib.view.proto.Deps
import io.bazel.kotlin.builder.tasks.CommandLineProgram
import io.bazel.kotlin.builder.utils.ArgMap
import io.bazel.kotlin.builder.utils.ArgMaps
import io.bazel.kotlin.builder.utils.Flag
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Persistent worker capable command line program for merging multiple Jdeps files into a single
 * file.
 */
class JdepsMerger: CommandLineProgram {
  companion object {

    @JvmStatic
    private val FLAGFILE_RE = Regex("""^--flagfile=((.*)-(\d+).params)$""")

    /**
     * Declares the flags used by the java builder.
     */
    enum class JdepsMergerFlags(override val flag: String) : Flag {
      INPUTS("--inputs"),
      OUTPUT("--output"),
      TARGET_LABEL("--target_label"),
    }

    fun merge(
      label: String,
      inputs: List<String>,
      output: String) {

      val rootBuilder = Deps.Dependencies.newBuilder()
      rootBuilder.success = false
      rootBuilder.ruleLabel = label

      val dependencyMap = sortedMapOf<String, Deps.Dependency>()
      inputs.forEach {input ->
        BufferedInputStream(Paths.get(input).toFile().inputStream()).use {
          val deps: Deps.Dependencies = Deps.Dependencies.parseFrom(it)
          deps.getDependencyList().forEach {
            val dependency = dependencyMap.get(it.path)
            // Replace dependency if it has a stronger kind than one we encountered before.
            if (dependency == null || dependency.kind > it.kind) {
              dependencyMap.put(it.path, it)
            }
          }
        }
      }

      rootBuilder.addAllDependency(dependencyMap.values)

      rootBuilder.success = true
      rootBuilder.build().toByteArray()

      BufferedOutputStream(File(output).outputStream()).use {
        it.write(rootBuilder.build().toByteArray())
      }
    }
  }

  private fun getArgs(args: List<String>): ArgMap {
    check(args.isNotEmpty()) { "expected at least a single arg got: ${args.joinToString(" ")}" }
    val lines = FLAGFILE_RE.matchEntire(args[0])?.groups?.get(1)?.let {
      Files.readAllLines(FileSystems.getDefault().getPath(it.value), StandardCharsets.UTF_8)
    } ?: args

    return ArgMaps.from(lines)
  }

  override fun apply(workingDir: Path, args: List<String>): Int {
    var status = 0
    val argMap = getArgs(args)
    val inputs = argMap.mandatory(JdepsMergerFlags.INPUTS)
    val output = argMap.mandatorySingle(JdepsMergerFlags.OUTPUT)
    val label = argMap.mandatorySingle(JdepsMergerFlags.TARGET_LABEL)
    merge(label, inputs, output)
    return status
  }
}
