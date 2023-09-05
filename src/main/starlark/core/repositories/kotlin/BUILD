load("//src/main/starlark/release:packager.bzl", "release_archive")

release_archive(
    name = "pkg",
    srcs = [
        "BUILD",
    ] + glob(["capabilities_*.bazel"]),
)
