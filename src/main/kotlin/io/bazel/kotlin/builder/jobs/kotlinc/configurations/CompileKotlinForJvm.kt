package io.bazel.kotlin.builder.jobs.kotlinc.configurations

import com.google.devtools.build.lib.view.proto.Deps
import com.google.devtools.build.lib.view.proto.Deps.Dependency.Kind.EXPLICIT
import io.bazel.kotlin.builder.jobs.kotlinc.JobContext
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import java.io.BufferedInputStream
import java.io.InputStream
import java.nio.file.Files.newInputStream
import java.nio.file.Path


class CompileKotlinForJvm<IN : CompileKotlinForJvm.In, OUT : CompileKotlinForJvm.Out> :
  CompileKotlin<IN, OUT> {

  interface In : CompileKotlin.In {
    val jdkHome: Path
    val jvmTarget: String
    val reducedClasspathMode: Boolean
  }

  interface Out : CompileKotlin.Out

  override fun K2JVMCompilerArguments.configure(context: JobContext<IN, OUT>) {
    initializeFor(context)

    classpath = deriveClassPath(context.inputs).joinToString(":")

    val allSources = (context.inputs.sources + context.inputs.sourcesFromJars).distinct()

    jvmTarget = context.inputs.jvmTarget
    jdkHome = context.inputs.jdkHome.toString()

    freeArgs =
      freeArgs + allSources.map { it.toString() }.filter { !it.endsWith(".java") }
    javaSourceRoots += allSources.map { it.toString() }.filter { it.endsWith(".java") }

  }

  private fun K2JVMCompilerArguments.deriveClassPath(inputs: In): List<String> {
    val existingClasspath = classpath?.split(inputs.fileSystem.separator) ?: emptyList()
    if (inputs.reducedClasspathMode) {
      return inputs.depsArtifacts
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
