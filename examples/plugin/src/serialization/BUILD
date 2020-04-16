load("//kotlin:kotlin.bzl", "kt_compiler_plugin", "kt_jvm_library", "kt_jvm_test")

kt_compiler_plugin(
    name = "serialization_plugin",
    deps = [
       "@com_github_jetbrains_kotlin//:kotlinx-serialization-compiler-plugin",
    ],
)

kt_jvm_library(
    name = "data",
    srcs = ["Data.kt"],
    plugins = [
        ":serialization_plugin",
    ],
    deps = [
        "@kotlin_rules_maven//:org_jetbrains_kotlinx_kotlinx_serialization_runtime",
    ],
)

kt_jvm_test(
    name = "serialization_test",
    srcs = ["SerializationTest.kt"],
    test_class = "plugin.serialization.SerializationTest",
    deps = [
        ":data",
        "@kotlin_rules_maven//:org_jetbrains_kotlinx_kotlinx_serialization_runtime",
        "@com_github_jetbrains_kotlin//:kotlin-reflect",
        "@kotlin_rules_maven//:junit_junit",
    ],
)