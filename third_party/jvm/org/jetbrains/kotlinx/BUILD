licenses(["notice"])
java_library(
    name = "atomicfu_common",
    exports = [
        "//external:jar/io_bazel_rules_kotlin_org/jetbrains/kotlinx/atomicfu_common"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "kotlinx_coroutines_core",
    exports = [
        "//external:jar/io_bazel_rules_kotlin_org/jetbrains/kotlinx/kotlinx_coroutines_core"
    ],
    runtime_deps = [
        "//third_party/jvm/org/jetbrains/kotlin:kotlin_stdlib",
        ":kotlinx_coroutines_core_common"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "kotlinx_coroutines_core_common",
    exports = [
        "//external:jar/io_bazel_rules_kotlin_org/jetbrains/kotlinx/kotlinx_coroutines_core_common"
    ],
    runtime_deps = [
        "//third_party/jvm/org/jetbrains/kotlin:kotlin_stdlib_common",
        ":atomicfu_common"
    ],
    visibility = [
        "//visibility:public"
    ]
)


