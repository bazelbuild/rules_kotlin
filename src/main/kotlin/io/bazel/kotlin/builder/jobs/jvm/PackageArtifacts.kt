package io.bazel.kotlin.builder.jobs.jvm

import io.bazel.kotlin.builder.utils.jars.JarCreator

class PackageArtifacts {
  fun run(jobContext: JobContext, outputs: CompilationOutputs) {
    outputs.output?.let { jar ->
      JarCreator(jar.path, normalize = true).addDirectory(jobContext.classes)
    }
    outputs.outputSrcJar?.let { srcJar ->
      JarCreator(srcJar.path, normalize = true).addDirectory(jobContext.generatedSources)
    }
  }
}
