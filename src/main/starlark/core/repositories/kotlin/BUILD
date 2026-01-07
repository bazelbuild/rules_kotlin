# RELEASE-CONTENT-START
load("@bazel_skylib//:bzl_library.bzl", "bzl_library")

bzl_library(
    name = "kotlin",
    srcs = glob([
        "*.bzl",
    ]),
    visibility = [
        "//visibility:public",
    ],
)
# RELEASE-CONTENT-END

# Dev-only: packaging rule
load("//src/main/starlark/release:packager.bzl", "release_archive")

release_archive(
    name = "pkg",
    srcs = glob([
        "capabilities_*.bazel",
        "artifacts.bzl",
        "templates.bzl",
    ]),
    src_map = {
        "BUILD": "BUILD",
    },
)
