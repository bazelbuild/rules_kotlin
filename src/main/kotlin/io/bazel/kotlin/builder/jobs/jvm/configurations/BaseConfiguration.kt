package io.bazel.kotlin.builder.jobs.jvm.configurations

import com.google.devtools.build.lib.view.proto.Deps
import com.google.devtools.build.lib.view.proto.Deps.Dependency.Kind.EXPLICIT
import io.bazel.kotlin.builder.jobs.jvm.JobContext
import io.bazel.kotlin.builder.jobs.jvm.flags.FilesSystemInputs
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import java.io.BufferedInputStream
import java.io.InputStream
import java.nio.file.Files.newInputStream
import java.nio.file.Path

class BaseConfiguration(val input: Inputs) : CompilerConfiguration {
  interface Inputs : FilesSystemInputs {
    val apiVersion: String
    val languageVersion: String
    val jvmTarget: String
    val moduleName: String
    val reducedClasspathMode: Boolean
    val depsArtifacts: List<Path>
    val classpath: List<Path>
    val sources: List<Path>
    val jdkHome: Path
    val useIr: Boolean
    val debug: List<String>
  }

  override fun K2JVMCompilerArguments.configure(context: JobContext) {
    classpath = deriveClassPath().joinToString(input.fileSystem.separator)
    apiVersion = input.apiVersion
    languageVersion = input.languageVersion
    jvmTarget = input.jvmTarget
    moduleName = input.moduleName
    freeArgs = freeArgs + input.sources.map { it.toString() }.filter { !it.endsWith(".java") }
    javaSourceRoots += input.sources.map { it.toString() }.filter { it.endsWith(".java") }
    destination = context.classes.toString()
    jdkHome = input.jdkHome.toString()
    verbose = "trace" in input.debug
    reportOutputFiles = true
    reportPerf = "perf" in input.debug
    useIR = input.useIr
  }


  private fun K2JVMCompilerArguments.deriveClassPath(): List<String> {
    val existingClasspath = classpath?.split(input.fileSystem.separator) ?: emptyList()
    if (input.reducedClasspathMode) {
      return existingClasspath +
        input.depsArtifacts
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
    return existingClasspath + input.classpath.map { it.toString() }
  }
}
