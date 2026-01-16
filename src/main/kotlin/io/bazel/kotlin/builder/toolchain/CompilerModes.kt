package io.bazel.kotlin.builder.toolchain

/**
 * Constants for build tools API mode values
 */
object CompilerModes {
  /**
   * Use the Kotlin K2 compiler
   */
  const val K2 = "K2"

  /**
   * Use the BuildTools API compiler (default)
   */
  const val BUILD_TOOLS = "BUILD_TOOLS"

  /**
   * Use incremental compilation with BuildTools
   */
  const val INCREMENTAL = "INCREMENTAL"
}
