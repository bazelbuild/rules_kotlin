load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_import")
licenses(["notice"])
kt_jvm_import(
    name = "atomicfu_common",
    jars = [
        "//external:jar/org/jetbrains/kotlinx/atomicfu_common"
    ],
    visibility = [
        "//visibility:public"
    ]
)



kt_jvm_import(
    name = "kotlinx_coroutines_core",
    exports = [
        "//third_party/jvm/org/jetbrains/kotlin:kotlin_stdlib",
        ":kotlinx_coroutines_core_common"
    ],
    jars = [
        "//external:jar/org/jetbrains/kotlinx/kotlinx_coroutines_core"
    ],
    visibility = [
        "//visibility:public"
    ]
)



kt_jvm_import(
    name = "kotlinx_coroutines_core_common",
    exports = [
        ":atomicfu_common"
    ],
    jars = [
        "//external:jar/org/jetbrains/kotlinx/kotlinx_coroutines_core_common"
    ],
    visibility = [
        "//visibility:public"
    ]
)



kt_jvm_import(
    name = "kotlinx_coroutines_jdk8",
    exports = [
        "//third_party/jvm/org/jetbrains/kotlin:kotlin_stdlib",
        ":kotlinx_coroutines_core"
    ],
    jars = [
        "//external:jar/org/jetbrains/kotlinx/kotlinx_coroutines_jdk8"
    ],
    visibility = [
        "//visibility:public"
    ]
)


