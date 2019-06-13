licenses(["notice"])

java_library(
    name = "kotlinx_coroutines_core",
    visibility = [
        "//visibility:public",
    ],
    exports = [
        "//external:jar/io_bazel_rules_kotlin_org/jetbrains/kotlinx/kotlinx_coroutines_core",
    ],
    runtime_deps = [
        ":kotlinx_coroutines_core_common",
        "//third_party/jvm/org/jetbrains/kotlin:kotlin_stdlib",
    ],
)

java_library(
    name = "kotlinx_coroutines_core_common",
    visibility = [
        "//third_party/jvm:__subpackages__",
    ],
    exports = [
        "//external:jar/io_bazel_rules_kotlin_org/jetbrains/kotlinx/kotlinx_coroutines_core_common",
    ],
    runtime_deps = [
        "//third_party/jvm/org/jetbrains/kotlin:kotlin_stdlib_common",
    ],
)
