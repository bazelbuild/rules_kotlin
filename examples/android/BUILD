load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_android_library")

kt_android_library(
    name = "activities",
    srcs = glob([
        "src/main/java/com/google/bazel/example/android/activities/*.kt",
    ]),
    custom_package = "com.google.bazel.example.android.activities",
    manifest = "src/main/java/com/google/bazel/example/android/activities/AndroidManifest.xml",
    idl_srcs = glob(["src/main/aidl/**/*.aidl"]),
    resource_files = glob(["src/main/java/com/google/bazel/example/android/activities/res/**"]),
)

android_binary(
    name = "android",
    custom_package = "com.google.bazel.example.android",
    manifest = "src/main/java/com/google/bazel/example/android/AndroidManifest.xml",
    resource_files = glob(["src/main/java/com/google/bazel/example/android/res/**"]),
    deps = [":activities"]
)