package io.bazel.kotlin.builder.jobs.jvm.configurations

import com.google.devtools.build.lib.view.proto.Deps
import com.google.devtools.build.lib.view.proto.Deps.Dependency.Kind.EXPLICIT
import io.bazel.kotlin.builder.utils.jars.JarCreator
import io.bazel.kotlin.builder.utils.jars.SourceJarCreator
import io.bazel.kotlin.builder.jobs.jvm.JobContext
import io.bazel.kotlin.builder.jobs.jvm.flags.FilesSystemInputs
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import java.io.BufferedInputStream
import java.io.InputStream
import java.nio.file.Files.newInputStream
import java.nio.file.Files.walk
import java.nio.file.Path


class CompileKotlin<IN : CompileKotlin.In, OUT : CompileKotlin.Out> :
  CompilerConfiguration<IN, OUT> {

  interface In : FilesSystemInputs {
    val apiVersion: String
    val classpath: List<Path>
    val debug: List<String>
    val depsArtifacts: List<Path>
    val jdkHome: Path
    val jvmTarget: String
    val languageVersion: String
    val moduleName: String
    val passthroughFlags: List<String>
    val reducedClasspathMode: Boolean
    val sources: List<Path>
    val sourcesFromJars: List<Path>
    val useIr: Boolean
  }

  interface Out {
    val outputSrcJar: Path?
    val output: Path?
  }

  override fun K2JVMCompilerArguments.configure(context: JobContext<IN, OUT>) {
    // apply passthrough flags first, as they will be overridden by known flags afterwards.
    parseCommandLineArguments(context.inputs.passthroughFlags, this)

    classpath = deriveClassPath(context.inputs).joinToString(":")
    apiVersion = context.inputs.apiVersion
    languageVersion = context.inputs.languageVersion
    jvmTarget = context.inputs.jvmTarget
    moduleName = context.inputs.moduleName
    val allSources = (context.inputs.sources + context.inputs.sourcesFromJars).distinct()
    freeArgs =
      freeArgs + allSources.map { it.toString() }.filter { !it.endsWith(".java") }
    javaSourceRoots += allSources.map { it.toString() }.filter { it.endsWith(".java") }
    destination = context.classes.toString()
    jdkHome = context.inputs.jdkHome.toString()
    verbose = "trace" in context.inputs.debug
    reportOutputFiles = true
    reportPerf = "perf" in context.inputs.debug
    useIR = context.inputs.useIr
    disableDefaultScriptingPlugin = true
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
          (context.inputs.sources + context.inputs.sourcesFromJars).stream().distinct()
        )
        archive.addSources(walk(context.generatedSources))
      }
    }
  }

  private fun K2JVMCompilerArguments.deriveClassPath(inputs: In): List<String> {
    val existingClasspath = classpath?.split(inputs.fileSystem.separator) ?: emptyList()
    if (inputs.reducedClasspathMode) {
      return existingClasspath +
        inputs.depsArtifacts
          .map { jdeps ->
            BufferedInputStream(newInputStream(jdeps)).use { stream: InputStream ->
              Deps.Dependencies.parseFrom(stream)
            }
          }
          .flatMap { dependencies ->
            dependencies.dependencyList.filter { it.kind == EXPLICIT }
          }
          .map { it.path }
          .distinct()
    }
    return existingClasspath + inputs.classpath.map { it.toString() }
  }
}
