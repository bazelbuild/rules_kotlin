load("@rules_kotlin//kotlin:core.bzl", "define_kt_toolchain")

# Define a Kotlin toolchain with multiplex sandboxing enabled.
# Multiplex workers are on by default; this enables sandbox isolation per work request.
# Requires Bazel to be run with --experimental_worker_multiplex_sandboxing.
define_kt_toolchain(
    name = "kotlin_toolchain",
    experimental_multiplex_sandboxing = True,
)
