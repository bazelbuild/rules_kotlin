load("@rules_kotlin//kotlin:android.bzl", "kt_android_library")

kt_android_library(
    name = "lib",
    srcs = ["MainActivity.kt"],
    custom_package = "cm.ben.android.bazel.compose.example.ui",
    manifest = "AndroidManifest.xml",
    plugins = ["//:jetpack_compose_compiler_plugin"],
    visibility = ["//visibility:public"],
    deps = [
        "@maven_rules_kotlin_example//:androidx_activity_activity_compose",
        "@maven_rules_kotlin_example//:androidx_appcompat_appcompat",
        "@maven_rules_kotlin_example//:androidx_compose_material_material",
        "@maven_rules_kotlin_example//:androidx_compose_runtime_runtime",
        "@maven_rules_kotlin_example//:androidx_compose_ui_ui",
        "@maven_rules_kotlin_example//:androidx_compose_ui_ui_tooling",
        "@maven_rules_kotlin_example//:androidx_core_core_ktx",
    ],
)
