licenses(["notice"])

java_library(
    name = "auto_service",
    visibility = [
        "//visibility:public",
    ],
    exports = [
        ":auto_service_annotations",
        "//external:jar/io_bazel_rules_kotlin_com/google/auto/service/auto_service",
    ],
    runtime_deps = [
        ":auto_service_annotations",
        "//third_party/jvm/com/google/auto:auto_common",
        "//third_party/jvm/com/google/guava",
    ],
)

java_library(
    name = "auto_service_annotations",
    visibility = [
        "//visibility:public",
    ],
    exports = [
        "//external:jar/io_bazel_rules_kotlin_com/google/auto/service/auto_service_annotations",
    ],
)
