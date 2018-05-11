licenses(["notice"])
java_library(
    name = "protobuf_java",
    exports = [
        "//external:jar/io_bazel_rules_kotlin_com/google/protobuf/protobuf_java"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "protobuf_java_util",
    exports = [
        "//external:jar/io_bazel_rules_kotlin_com/google/protobuf/protobuf_java_util"
    ],
    runtime_deps = [
        "//third_party/jvm/com/google/code/gson:gson",
        "//third_party/jvm/com/google/guava:guava",
        ":protobuf_java"
    ],
    visibility = [
        "//visibility:public"
    ]
)


