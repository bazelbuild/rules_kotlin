load("//src/main/starlark/release:packager.bzl", "release_archive")

release_archive(
    name = "pkg",
    srcs = glob([
        "capabilities_*.bazel",
        "artifacts.bzl",
    ]),
    src_map = {
        "BUILD.release.bazel": "BUILD",
    },
)
