load("@bazel_skylib//:skylark_library.bzl", "skylark_library")
load("@io_bazel_skydoc//skylark:skylark.bzl", "skylark_doc" )

skylark_library(
    name = "doc_public",
    srcs = ["//kotlin:kotlin.bzl"],
    visibility=["//visibility:private"]
)

skylark_library(
    name = "doc_toolchains",
    srcs = ["//kotlin:toolchains.bzl"],
    visibility=["//visibility:private"]
)

skylark_doc(
    name = "docs",
    srcs = [
        ":doc_public",
        ":doc_toolchains"
    ],
    format = "html",
    site_root = select({
        ":local": "/tmp/rules_kotlin",
        "//conditions:default": "https://bazelbuild.github.io/rules_kotlin"
    }),
    visibility = ["//visibility:public"]
)

config_setting(
    name = "local",
    values = {
        "define": "local=1",
    }
)