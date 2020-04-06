load("//kotlin:kotlin.bzl", "kt_compiler_plugin", "kt_jvm_library")

kt_compiler_plugin(
    name = "open_for_testing_plugin",
    id = "org.jetbrains.kotlin.allopen",
    options = {
        "annotation": "plugin.allopen.OpenForTesting",
    },
    deps = [
        "@com_github_jetbrains_kotlin//:allopen-compiler-plugin",
    ],
)

kt_jvm_library(
    name = "open_for_testing",
    srcs = ["OpenForTesting.kt"],
)

kt_jvm_library(
    name = "user",
    srcs = ["User.kt"],
    plugins = [
        ":open_for_testing_plugin",
    ],
    deps = [
        ":open_for_testing",
    ],
)

kt_jvm_library(
    name = "user_is_open_test",
    srcs = ["UserIsOpenTest.kt"],
    deps = [
        ":user",
    ],
)
