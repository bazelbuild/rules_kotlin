load("@rules_kotlin//kotlin:core.bzl", "define_kt_toolchain")

# Define a Kotlin toolchain with path mapping enabled.
# Path mapping rewrites action inputs/outputs to shorter paths, reducing cache misses
# when moving between configurations. It requires multiplex sandboxing.
# Requires Bazel flags:
#   --experimental_worker_multiplex_sandboxing
#   --experimental_output_paths=strip
define_kt_toolchain(
    name = "kotlin_toolchain",
    experimental_multiplex_sandboxing = True,
    supports_path_mapping = True,
)
