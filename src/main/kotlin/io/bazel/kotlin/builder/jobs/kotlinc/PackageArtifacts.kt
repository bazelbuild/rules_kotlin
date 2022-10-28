package io.bazel.kotlin.builder.jobs.kotlinc

import io.bazel.kotlin.builder.jobs.kotlinc.configurations.CompileKotlinForJvm
import io.bazel.kotlin.builder.jobs.kotlinc.configurations.GenerateAbi
import io.bazel.kotlin.builder.utils.jars.JarCreator

class PackageArtifacts {
  fun <OUTPUTS> run(jobContext: JobContext<Any, OUTPUTS>)
    where OUTPUTS : CompileKotlinForJvm.Out,
          OUTPUTS : GenerateAbi.Out {
    jobContext.outputs.output?.let { jar ->
      JarCreator(jar, normalize = true).addDirectory(jobContext.classes)
    }
    jobContext.outputs.outputSrcJar?.let { srcJar ->
      JarCreator(srcJar, normalize = true).addDirectory(jobContext.generatedSources)
    }
  }
}
