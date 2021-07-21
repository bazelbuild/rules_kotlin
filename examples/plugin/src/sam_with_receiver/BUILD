load("@io_bazel_rules_kotlin//kotlin:core.bzl", "kt_compiler_plugin")
load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("@rules_java//java:defs.bzl", "java_library")

kt_compiler_plugin(
    name = "sam_with_receiver_plugin",
    compile_phase = True,
    id = "org.jetbrains.kotlin.samWithReceiver",
    options = {
        "annotation": "plugin.sam_with_receiver.SamWithReceiver",
    },
    stubs_phase = True,
    deps = [
        "@com_github_jetbrains_kotlin//:sam-with-receiver-compiler-plugin",
    ],
)

kt_jvm_library(
    name = "sam_with_receiver",
    srcs = ["SamWithReceiver.kt"],
)

java_library(
    name = "runner",
    srcs = ["Runner.java"],
    deps = [":sam_with_receiver"],
)

kt_jvm_library(
    name = "runner_test",
    srcs = ["RunnerTest.kt"],
    plugins = [":sam_with_receiver_plugin"],
    deps = [":runner"],
)
