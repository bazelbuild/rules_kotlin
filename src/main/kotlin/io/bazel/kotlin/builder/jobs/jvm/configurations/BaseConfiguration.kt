package io.bazel.kotlin.builder.jobs.jvm.configurations

import com.google.devtools.build.lib.view.proto.Deps
import com.google.devtools.build.lib.view.proto.Deps.Dependency.Kind.EXPLICIT
import io.bazel.kotlin.builder.jobs.jvm.Artifact
import io.bazel.kotlin.builder.jobs.jvm.JobContext
import io.bazel.kotlin.builder.jobs.jvm.flags.FilesSystemInputs
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import java.io.BufferedInputStream
import java.io.InputStream
import java.nio.file.Files.newInputStream

class BaseConfiguration(val input: Inputs) : CompilerConfiguration {
  interface Inputs : FilesSystemInputs {
    val apiVersion: String
    val languageVersion: String
    val jvmTarget: String
    val moduleName: String
    val reducedClasspathMode: Boolean
    val depsArtifacts: List<Artifact>
    val classpath: List<Artifact>
    val sources: List<Artifact>
    val jdkHome: Artifact
    val verbose: Boolean
    val useIr : Boolean
  }

  override fun K2JVMCompilerArguments.configure(context: JobContext) {
    classpath = deriveClassPath().joinToString(input.fileSystem.separator)
    apiVersion = input.apiVersion
    languageVersion = input.languageVersion
    jvmTarget = input.jvmTarget
    moduleName = input.moduleName
    freeArgs = freeArgs + input.sources.map { it.path.toString() }.filter { !it.endsWith(".java") }
    javaSourceRoots += input.sources.map { it.path.toString() }.filter { it.endsWith(".java") }
    destination = context.classes.toString()
    jdkHome = input.jdkHome.path.toString()
    verbose = input.verbose
    reportOutputFiles = true
    reportPerf = input.verbose
    useIR = input.useIr
  }


  private fun K2JVMCompilerArguments.deriveClassPath(): List<String> {
    val existingClasspath = classpath?.split(input.fileSystem.separator) ?: emptyList()
    if (input.reducedClasspathMode) {
      return existingClasspath +
        input.depsArtifacts
          .map { jdeps ->
            BufferedInputStream(newInputStream(jdeps.path)).use { stream: InputStream ->
              Deps.Dependencies.parseFrom(stream)
            }
          }
          .flatMap { dependencies ->
            dependencies.dependencyList.filter { it.kind == EXPLICIT }
          }
          .map { it.path }
          .distinct()
    }
    return existingClasspath + input.classpath.map { it.path.toString() }
  }
}
