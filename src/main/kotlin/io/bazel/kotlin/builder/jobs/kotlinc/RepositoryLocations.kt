package io.bazel.kotlin.builder.jobs.kotlinc

import io.bazel.kotlin.builder.utils.BazelRunFiles


/**
 * Repository locations.
 */
// TODO(): Replace with command line arguments.
object RepositoryLocations {
  val JVM_ABI_PLUGIN by lazy {
    BazelRunFiles.resolveVerifiedFromProperty(
      "@com_github_jetbrains_kotlin...jvm-abi-gen",
    ).toPath()
  }

  val KAPT_PLUGIN by lazy {
    BazelRunFiles.resolveVerifiedFromProperty(
      "@com_github_jetbrains_kotlin...kapt",
    ).toPath()
  }

  val COMPILER by lazy {
    BazelRunFiles.resolveVerifiedFromProperty(
      "@rules_kotlin...compiler",
    ).toPath()
  }

  val SKIP_CODE_GEN_PLUGIN by lazy {
    BazelRunFiles.resolveVerifiedFromProperty(
      "@rules_kotlin...skip-code-gen",
    ).toPath()
  }

  val JDEPS_GEN_PLUGIN by lazy {
    BazelRunFiles.resolveVerifiedFromProperty(
      "@rules_kotlin...jdeps-gen",
    ).toPath()
  }

  val KOTLINC by lazy {
    BazelRunFiles.resolveVerifiedFromProperty(
      "@com_github_jetbrains_kotlin...kotlin-compiler",
    ).toPath()
  }

  val KSP_SYMBOL_PROCESSING_API by lazy {
    BazelRunFiles.resolveVerifiedFromProperty(
      "@com_github_google_ksp...symbol-processing-api",
    ).toPath()
  }

  val KSP_SYMBOL_PROCESSING_CMDLINE by lazy {
    BazelRunFiles.resolveVerifiedFromProperty(
      "@com_github_google_ksp...symbol-processing-cmdline",
    ).toPath()
  }
}
