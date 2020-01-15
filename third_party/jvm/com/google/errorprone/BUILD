load("@rules_java//java:defs.bzl", "java_library")
licenses(["notice"])

java_library(
    name = "error_prone_annotations",
    visibility = [
        "//third_party/jvm:__subpackages__",
    ],
    exports = [
        "//external:jar/io_bazel_rules_kotlin_com/google/errorprone/error_prone_annotations",
    ],
)

java_library(
    name = "javac_shaded",
    visibility = [
        "//third_party/jvm:__subpackages__",
    ],
    exports = [
        "//external:jar/io_bazel_rules_kotlin_com/google/errorprone/javac_shaded",
    ],
)
