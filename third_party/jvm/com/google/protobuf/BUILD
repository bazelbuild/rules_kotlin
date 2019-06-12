licenses(["notice"])

java_library(
    name = "protobuf_java",
    visibility = [
        "//visibility:public",
    ],
    exports = [
        "//external:jar/io_bazel_rules_kotlin_com/google/protobuf/protobuf_java",
    ],
)

java_library(
    name = "protobuf_java_util",
    visibility = [
        "//visibility:public",
    ],
    exports = [
        "//external:jar/io_bazel_rules_kotlin_com/google/protobuf/protobuf_java_util",
    ],
    runtime_deps = [
        ":protobuf_java",
        "//third_party/jvm/com/google/code/gson",
        "//third_party/jvm/com/google/guava",
    ],
)
