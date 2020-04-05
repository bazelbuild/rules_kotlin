load("//kotlin:kotlin.bzl", "kt_compiler_plugin", "kt_jvm_library", "kt_jvm_test")

kt_compiler_plugin(
    name = "no_arg_plugin",
    id = "org.jetbrains.kotlin.noarg",
    options = {
        "annotation": "plugin.noarg.NoArgConstructor",
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
    name = "user",
    srcs = ["User.kt"],
    plugins = [":no_arg_plugin"],
    deps = [
        ":no_arg_constructor",
    ],
)

# The no-arg constructor that is generated cannot be compiled against, but should be discoverable at runtime.
kt_jvm_test(
    name = "user_has_noarg_constructor_test",
    srcs = ["UserHasNoargConstructorTest.kt"],
    test_class = "plugin.noarg.UserHasNoargConstructorTest",
    deps = [
        ":user",
        "@com_github_jetbrains_kotlin//:kotlin-reflect",
        "@kotlin_rules_maven//:junit_junit",
    ],
)
