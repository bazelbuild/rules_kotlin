load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_compiler_plugin", "kt_jvm_library", "kt_jvm_test")

kt_compiler_plugin(
    name = "serialization_plugin",
    compile_phase = True,
    id = "org.jetbrains.kotlin.serialization",
    stubs_phase = True,
    deps = [
        "@com_github_jetbrains_kotlin//:kotlinx-serialization-compiler-plugin",
    ],
)

# This you would put in your tree and maybe forbid depending on the raw rule.
# Using square's bazel_maven_repository, you could use a build-snippet with
# kt_jvm_import instead.
kt_jvm_library(
    name = "kotlin_serialization",
    srcs = [],
    exported_compiler_plugins = [":serialization_plugin"],
    exports = [
        "@maven//:org_jetbrains_kotlinx_kotlinx_serialization_core_jvm",
    ],
)

kt_jvm_library(
    name = "data",
    srcs = ["Data.kt"],
    deps = [
        ":kotlin_serialization",
    ],
)

kt_jvm_test(
    name = "serialization_test",
    srcs = ["SerializationTest.kt"],
    test_class = "plugin.serialization.SerializationTest",
    deps = [
        ":data",
        "@com_github_jetbrains_kotlin//:kotlin-reflect",
        "@maven//:junit_junit",
    ],
)
