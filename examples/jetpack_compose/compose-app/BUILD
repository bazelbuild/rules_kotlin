load("@build_bazel_rules_android//android:rules.bzl", "android_binary")

# An app that consumes android-kt deps
android_binary(
    name = "compose_example_app",
    custom_package = "cm.ben.android.bazel.compose.example",
    incremental_dexing = 1,
    manifest = "AndroidManifest.xml",
    multidex = "native",
    visibility = ["//visibility:public"],
    deps = [
        "//compose-ui:lib",
    ],
)
