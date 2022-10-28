package io.bazel.kotlin.builder.jobs.kotlinc

import io.bazel.kotlin.builder.utils.BazelRunFiles


/**
 * Repository locations.
 */
// TODO(): Replace with command line arguments.
object RepositoryLocations {
  val RULES_REPOSITORY_NAME =
    System.getenv("TEST_WORKSPACE")?.takeIf { it.isNotBlank() }
      ?: System.getenv("REPOSITORY_NAME")?.takeIf { it.isNotBlank() }
      ?: error("Unable to determine rules_kotlin repository name.\nenv:${System.getenv()}\nproperties:${System.getProperties()}")

  val DEFAULT_JVM_ABI_PATH = BazelRunFiles.resolveVerified(
    "external", "com_github_jetbrains_kotlin", "lib", "jvm-abi-gen.jar"
  ).toPath()

  val COMPILER = BazelRunFiles.resolveVerified(
    RULES_REPOSITORY_NAME,
    "src", "main", "kotlin", "io", "bazel", "kotlin", "compiler",
    "compiler.jar").toPath()

  val SKIP_CODE_GEN_PLUGIN = BazelRunFiles.resolveVerified(
    RULES_REPOSITORY_NAME,
    "src", "main", "kotlin",
    "skip-code-gen.jar").toPath()

  val JDEPS_GEN_PLUGIN = BazelRunFiles.resolveVerified(
    RULES_REPOSITORY_NAME,
    "src", "main", "kotlin",
    "jdeps-gen.jar").toPath()

  val KAPT = BazelRunFiles.resolveVerified(
    RULES_REPOSITORY_NAME,
    "external", "com_github_jetbrains_kotlin", "lib", "kotlin-annotation-processing.jar").toPath()
}
