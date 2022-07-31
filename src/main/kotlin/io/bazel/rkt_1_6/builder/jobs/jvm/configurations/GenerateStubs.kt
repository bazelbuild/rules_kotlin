package io.bazel.rkt_1_6.builder.jobs.jvm.configurations

import io.bazel.kotlin.builder.utils.jars.JarCreator
import io.bazel.kotlin.builder.utils.jars.SourceJarCreator
import io.bazel.rkt_1_6.builder.jobs.jvm.JobContext
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import java.nio.file.Path
import kotlin.streams.toList

class GenerateStubs<IN : GenerateStubs.In, OUT : GenerateStubs.Out> : KaptConfiguration<IN, OUT> {

  interface In : KaptConfiguration.In {
    val stubsPluginClassPath: List<Path>
    val stubsPluginOptions: List<String>
  }

  interface Out : KaptConfiguration.Out {
    val generatedJavaSrcJar: Path?
    val generatedJavaStubJar: Path?
    val generatedClassJar: Path?
  }

  override fun K2JVMCompilerArguments.configure(context: JobContext<IN, OUT>) {
    with(context.inputs) {
      if (stubsPluginClassPath.isEmpty() && processors.isEmpty()) {
        context.info { "Skipping Stub Generation : no plugins or processors" }
        return
      }
      // run all stub plugins
      plugins(stubsPluginClassPath, stubsPluginOptions, context)
      // run kapt which will exit after stub generation
      configure("stubs", context)
    }
  }

  override fun packageArtifacts(context: JobContext<IN, OUT>) {
    context.outputs.generatedJavaSrcJar?.let { jar ->
      SourceJarCreator(jar).use {
        it.addSources(
          context.generatedSources.walk {
            filter { file -> file.fileName.extension == "java" }.toList()
          }.stream()
        )
      }
    }

    context.outputs.generatedJavaStubJar?.let { jar ->
      SourceJarCreator(jar).use {
        it.addSources(
          context.stubs.walk {
            filter { file -> file.fileName.extension == "java" }.toList()
          }.stream()
        )
      }
    }

    context.outputs.generatedClassJar?.let { jar ->
      JarCreator(jar, true).use {
        it.addDirectory(context.generatedClasses)
        it.addDirectory(context.generatedResources)
      }
    }
  }
}
