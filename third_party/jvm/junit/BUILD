load("@rules_java//java:defs.bzl", "java_library")
licenses(["notice"])

java_library(
    name = "junit",
    visibility = [
        "//visibility:public",
    ],
    exports = [
        "//external:jar/io_bazel_rules_kotlin_junit/junit",
    ],
    runtime_deps = [
        "//third_party/jvm/org/hamcrest:hamcrest_core",
    ],
)
