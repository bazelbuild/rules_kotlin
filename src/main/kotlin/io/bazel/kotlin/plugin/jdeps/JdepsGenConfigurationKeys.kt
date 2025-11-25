package io.bazel.kotlin.plugin.jdeps

import org.jetbrains.kotlin.config.CompilerConfigurationKey

object JdepsGenConfigurationKeys {
  /**
   * Output path of generated Jdeps proto file.
   */
  val OUTPUT_JDEPS: CompilerConfigurationKey<String> =
    CompilerConfigurationKey.create(
      JdepsGenCommandLineProcessor.OUTPUT_JDEPS_FILE_OPTION.description,
    )

  /**
   * Label of the Bazel target being analyzed.
   */
  val TARGET_LABEL: CompilerConfigurationKey<String> =
    CompilerConfigurationKey.create(JdepsGenCommandLineProcessor.TARGET_LABEL_OPTION.description)

  /**
   * Label of the Bazel target being analyzed.
   */
  val STRICT_KOTLIN_DEPS: CompilerConfigurationKey<String> =
    CompilerConfigurationKey.create(
      JdepsGenCommandLineProcessor.STRICT_KOTLIN_DEPS_OPTION.description,
    )

  /**
   * List of direct dependencies of the target.
   */
  val DIRECT_DEPENDENCIES: CompilerConfigurationKey<List<String>> =
    CompilerConfigurationKey.create(
      JdepsGenCommandLineProcessor.DIRECT_DEPENDENCIES_OPTION.description,
    )

  /**
   * Full classpath used for compilation (includes all transitive dependencies and stdlib).
   * Used for mapping canonical filesystem paths back to original classpath paths.
   */
  val FULL_CLASSPATH: CompilerConfigurationKey<List<String>> =
    CompilerConfigurationKey.create(
      JdepsGenCommandLineProcessor.FULL_CLASSPATH_OPTION.description,
    )
}
