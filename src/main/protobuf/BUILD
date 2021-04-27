load("@rules_proto//proto:defs.bzl", "proto_library")
load("@rules_java//java:defs.bzl", "java_library", "java_proto_library")

package(default_visibility = ["//visibility:public"])

alias(
    name = "deps_java_proto",
    actual = "@deps//:java_proto",
)

alias(
    name = "worker_protocol_java_proto",
    actual = "@worker_protocol//:java_proto",
)

proto_library(
    name = "kotlin_model_proto",
    srcs = [":kotlin_model.proto"],
    visibility = ["//visibility:public"],
)

java_proto_library(
    name = "kotlin_model_java_proto",
    deps = [":kotlin_model_proto"],
)

java_library(
    name = "protobuf",
    exports = [
        ":deps_java_proto",
        ":kotlin_model_java_proto",
        ":worker_protocol_java_proto",
    ],
)
