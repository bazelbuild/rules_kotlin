load("@rules_java//java:defs.bzl", "java_library")

licenses(["notice"])

java_library(
    name = "google_java_format",
    visibility = [
        "//third_party/jvm:__subpackages__",
    ],
    exports = [
        "//external:jar/io_bazel_rules_kotlin_com/google/googlejavaformat/google_java_format",
    ],
    runtime_deps = [
        "//third_party/jvm/com/google/errorprone:javac_shaded",
        "//third_party/jvm/com/google/guava",
    ],
)
