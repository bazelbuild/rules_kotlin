load("@rules_java//java:defs.bzl", "java_library")
licenses(["notice"])

java_library(
    name = "failureaccess",
    visibility = [
        "//third_party/jvm:__subpackages__",
    ],
    exports = [
        "//external:jar/io_bazel_rules_kotlin_com/google/guava/failureaccess",
    ],
)

java_library(
    name = "guava",
    visibility = [
        "//visibility:public",
    ],
    exports = [
        "//external:jar/io_bazel_rules_kotlin_com/google/guava/guava",
    ],
    runtime_deps = [
        ":failureaccess",
        ":listenablefuture",
        "//third_party/jvm/com/google/code/findbugs:jsr305",
        "//third_party/jvm/com/google/errorprone:error_prone_annotations",
        "//third_party/jvm/com/google/j2objc:j2objc_annotations",
        "//third_party/jvm/org/checkerframework:checker_qual",
        "//third_party/jvm/org/codehaus/mojo:animal_sniffer_annotations",
    ],
)

java_library(
    name = "listenablefuture",
    visibility = [
        "//third_party/jvm:__subpackages__",
    ],
    exports = [
        "//external:jar/io_bazel_rules_kotlin_com/google/guava/listenablefuture",
    ],
)
