load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_android_library")

kt_android_library(
    name = "lib",
    srcs = ["MainActivity.kt"],
    custom_package = "cm.ben.android.bazel.compose.example.ui",
    manifest = "AndroidManifest.xml",
    plugins = ["//:jetpack_compose_compiler_plugin"],
    visibility = ["//visibility:public"],
    deps = [
        "@maven//:androidx_appcompat_appcompat",
        "@maven//:androidx_compose_material_material",
        "@maven//:androidx_compose_ui_ui",
        "@maven//:androidx_compose_ui_ui_tooling",
        "@maven//:androidx_core_core_ktx",
        "@maven//:com_google_android_material_material",
        "@maven//:org_jetbrains_kotlin_kotlin_stdlib",
    ],
)
