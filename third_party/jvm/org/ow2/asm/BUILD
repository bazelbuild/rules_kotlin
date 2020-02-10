load("@rules_java//java:defs.bzl", "java_library")
licenses(["notice"])

java_library(
    name = "asm",
    visibility = [
        "//third_party/jvm:__subpackages__",
    ],
    exports = [
        "//external:jar/io_bazel_rules_kotlin_org/ow2/asm/asm",
    ],
)

java_library(
    name = "asm_analysis",
    visibility = [
        "//third_party/jvm:__subpackages__",
    ],
    exports = [
        "//external:jar/io_bazel_rules_kotlin_org/ow2/asm/asm_analysis",
    ],
    runtime_deps = [
        ":asm_tree",
    ],
)

java_library(
    name = "asm_commons",
    visibility = [
        "//third_party/jvm:__subpackages__",
    ],
    exports = [
        "//external:jar/io_bazel_rules_kotlin_org/ow2/asm/asm_commons",
    ],
    runtime_deps = [
        ":asm",
        ":asm_analysis",
        ":asm_tree",
    ],
)

java_library(
    name = "asm_tree",
    visibility = [
        "//third_party/jvm:__subpackages__",
    ],
    exports = [
        "//external:jar/io_bazel_rules_kotlin_org/ow2/asm/asm_tree",
    ],
    runtime_deps = [
        ":asm",
    ],
)
