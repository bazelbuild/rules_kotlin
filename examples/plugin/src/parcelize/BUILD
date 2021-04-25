load("//kotlin:kotlin.bzl", "kt_compiler_plugin", "kt_jvm_library", "kt_jvm_test")

kt_compiler_plugin(
    name = "parcelize_plugin",
    compile_phase = True,
    id = "org.jetbrains.kotlin.parcelize",
    stubs_phase = True,
    deps = [
        "@com_github_jetbrains_kotlin//:parcelize-compiler-plugin",
    ],
)

kt_jvm_library(
    name = "parcelize",
    srcs = [],
    exported_compiler_plugins = [":parcelize_plugin"],
    plugins = [
        ":parcelize_plugin",
    ],
    visibility = ["//visibility:public"],
    exports = [
        "@com_github_jetbrains_kotlin//:parcelize-runtime",
    ],
)

kt_android_library(
    name = "parcelable",
    srcs = [
        "ParcelableObject.kt",
    ],
    deps = [
        ":parcelize",
    ],
)
