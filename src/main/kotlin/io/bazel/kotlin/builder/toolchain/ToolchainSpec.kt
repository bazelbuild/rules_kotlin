package io.bazel.kotlin.builder.toolchain

import java.nio.file.Path

data class ToolchainSpec(
  val btapiClasspath: List<Path>,
  val plugins: Map<String, InternalCompilerPlugin>,
) {
  companion object {
    const val JVM_ABI_GEN = "jvm_abi_gen"
    const val SKIP_CODE_GEN = "skip_code_gen"
    const val KAPT = "kapt"
    const val JDEPS = "jdeps"
  }

  fun requirePlugin(name: String): InternalCompilerPlugin =
    plugins[name] ?: throw IllegalArgumentException(
      "Required plugin '$name' not found. Available: ${plugins.keys}",
    )
}
