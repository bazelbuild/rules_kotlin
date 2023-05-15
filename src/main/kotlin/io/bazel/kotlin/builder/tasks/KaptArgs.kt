package io.bazel.kotlin.builder.tasks

data class KaptArgs @JvmOverloads constructor(
  val correctErrorTypes: String?,
  val aptMode: String = "stubsAndApt",
) {
  companion object {
    private const val CORRECT_ERROR_TYPES = "correct_error_types"

    fun create(args: List<String>): KaptArgs {
      return KaptArgs(args.contains(CORRECT_ERROR_TYPES).toString(), aptMode = aptMode(args))
    }

    private fun aptMode(args: List<String>): String {
      return args.firstOrNull { it.startsWith("apt_mode=") }?.split("=")?.last() ?: "stubsAndApt"
    }
  }
}
