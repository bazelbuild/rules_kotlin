package io.bazel.kotlin.builder.jobs.jvm.configurations

import io.bazel.kotlin.builder.jobs.jvm.JobContext
import io.bazel.kotlin.builder.jobs.jvm.flags.FilesSystemInputs
import io.bazel.kotlin.builder.utils.jars.JarCreator
import io.bazel.kotlin.builder.utils.jars.SourceJarCreator
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import java.nio.file.Files
import java.nio.file.Path

interface CompileKotlin<IN : CompileKotlin.In, OUT : CompileKotlin.Out> :
  CompilerConfiguration<IN, OUT> {
  interface In : FilesSystemInputs {
    val apiVersion: String
    val classpath: List<Path>
    val debug: List<String>
    val depsArtifacts: List<Path>
    val languageVersion: String
    val moduleName: String
    val passthroughFlags: List<String>
    val sources: List<Path>
    val sourcesFromJars: List<Path>
    val useIr: Boolean
  }

  fun K2JVMCompilerArguments.initializeFor(context: JobContext<IN, OUT>) {
    // apply passthrough flags first, as they will be overridden by known flags afterwards.
    parseCommandLineArguments(context.inputs.passthroughFlags, this)

    classpath = context.inputs.classpath.joinToString(":", transform = Path::toString)
    apiVersion = context.inputs.apiVersion
    languageVersion = context.inputs.languageVersion
    moduleName = context.inputs.moduleName
    destination = context.classes.toString()

    verbose = "trace" in context.inputs.debug
    reportOutputFiles = true
    reportPerf = "perf" in context.inputs.debug
    useIR = context.inputs.useIr
    disableDefaultScriptingPlugin = true
  }

  interface Out {
    val outputSrcJar: Path?
    val output: Path?
  }

  override fun packageArtifacts(context: JobContext<IN, OUT>) {
    context.outputs.output?.let { jar ->
      JarCreator(jar, normalize = true).use { archive ->
        archive.addDirectory(context.classes)
        archive.addDirectory(context.generatedClasses)
        archive.addDirectory(context.generatedResources)
      }
    }
    context.outputs.outputSrcJar?.let { srcJar ->
      SourceJarCreator(srcJar).use { archive ->
        archive.addSources(
          (context.inputs.sources + context.inputs.sourcesFromJars).stream().distinct(),
        )
        archive.addSources(Files.walk(context.generatedSources))
      }
    }
  }
}
