load("//kotlin:kotlin.bzl", "kt_compiler_plugin", "kt_jvm_library", "kt_jvm_test")

kt_compiler_plugin(
    name = "open_for_testing_plugin",
    id = "org.jetbrains.kotlin.allopen",
    options = {
        "annotation": "plugin.allopennoarg.OpenForTesting",
    },
    deps = [
        "@com_github_jetbrains_kotlin//:allopen-compiler-plugin",
    ],
)

kt_compiler_plugin(
    name = "no_arg_plugin",
    id = "org.jetbrains.kotlin.noarg",
    options = {
        "annotation": "plugin.allopennoarg.NoArgConstructor",
    },
    deps = [
        "@com_github_jetbrains_kotlin//:noarg-compiler-plugin",
    ],
)

kt_jvm_library(
    name = "no_arg_constructor",
    srcs = ["NoArgConstructor.kt"],
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
        ":no_arg_plugin"
    ],
    deps = [
        ":open_for_testing",
        ":no_arg_constructor"
    ],
)

kt_jvm_library(
    name = "user_is_open_test",
    srcs = ["UserIsOpenTest.kt"],
    deps = [
        ":user",
    ],
)

kt_jvm_test(
    name = "user_has_noarg_constructor_test",
    srcs = ["UserHasNoargConstructorTest.kt"],
    test_class = "plugin.allopennoarg.UserHasNoargConstructorTest",
    deps = [
        ":user",
        "@com_github_jetbrains_kotlin//:kotlin-reflect",
        "@kotlin_rules_maven//:junit_junit",
    ],
)

