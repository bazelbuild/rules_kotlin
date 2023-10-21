load("@rules_kotlin//kotlin:android.bzl", "kt_android_library")
load("@rules_kotlin//kotlin:core.bzl", "kt_compiler_plugin")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_compiler_plugin(
    name = "parcelize_plugin",
    compile_phase = True,
    id = "org.jetbrains.kotlin.parcelize",
    stubs_phase = True,
    deps = [
        "@rules_kotlin//kotlin/compiler:parcelize-compiler-plugin",
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
        "@rules_kotlin//kotlin/compiler:parcelize-runtime",
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
