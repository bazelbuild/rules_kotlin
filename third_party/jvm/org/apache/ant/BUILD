load("@rules_java//java:defs.bzl", "java_library")

licenses(["notice"])

java_library(
    name = "ant",
    visibility = [
        "//third_party/jvm:__subpackages__",
    ],
    exports = [
        "//external:jar/io_bazel_rules_kotlin_org/apache/ant/ant",
    ],
    runtime_deps = [
        ":ant_launcher",
    ],
)

java_library(
    name = "ant_launcher",
    visibility = [
        "//third_party/jvm:__subpackages__",
    ],
    exports = [
        "//external:jar/io_bazel_rules_kotlin_org/apache/ant/ant_launcher",
    ],
)
