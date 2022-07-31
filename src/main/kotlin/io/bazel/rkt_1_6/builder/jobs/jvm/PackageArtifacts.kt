package io.bazel.rkt_1_6.builder.jobs.jvm

import io.bazel.rkt_1_6.builder.jobs.jvm.configurations.CompileKotlin
import io.bazel.rkt_1_6.builder.jobs.jvm.configurations.GenerateAbi
import io.bazel.kotlin.builder.utils.jars.JarCreator

class PackageArtifacts {
  fun <OUTPUTS> run(jobContext: JobContext<Any, OUTPUTS>)
    where OUTPUTS : CompileKotlin.Out,
          OUTPUTS : GenerateAbi.Out {
    jobContext.outputs.output?.let { jar ->
      JarCreator(jar, normalize = true).addDirectory(jobContext.classes)
    }
    jobContext.outputs.outputSrcJar?.let { srcJar ->
      JarCreator(srcJar, normalize = true).addDirectory(jobContext.generatedSources)
    }
  }
}
