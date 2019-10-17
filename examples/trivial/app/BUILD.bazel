load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "app_lib",
    srcs = glob(["**/*.kt"]),
    deps = [
        "//:java_deps",
    ],
)

java_binary(
    name = "myapp",
    main_class = "app.MyApp",
    runtime_deps = [":app_lib"],
)