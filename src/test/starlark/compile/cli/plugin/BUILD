load("//kotlin:jvm.bzl", "kt_jvm_test")
load("//src/main/starlark/core/compile:rules.bzl", "core_kt_jvm_library")
load("//src/main/starlark/core/plugin:rules.bzl", "kt_compiler_plugin")

kt_compiler_plugin(
    name = "serialization_plugin",
    compile_phase = True,
    id = "org.jetbrains.kotlin.serialization",
    stubs_phase = True,
    deps = [
        "//kotlin/compiler:kotlinx-serialization-compiler-plugin",
    ],
)

core_kt_jvm_library(
    name = "data",
    srcs = ["Data.kt"],
    plugins = [
        ":serialization_plugin",
    ],
    deps = [
        "@kotlin_rules_maven//:org_jetbrains_kotlinx_kotlinx_serialization_core_jvm",
    ],
)

kt_jvm_test(
    name = "serialization_test",
    srcs = ["SerializationTest.kt"],
    test_class = "plugin.serialization.SerializationTest",
    deps = [
        ":data",
        "@kotlin_rules_maven//:junit_junit",
    ],
)
