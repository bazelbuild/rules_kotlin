load("//src/main/starlark/core/release:packager.bzl", "release_archive")

release_archive(
    name = "pkg",
    srcs = glob(
        ["*.bzl"],
    ),
    src_map = {
        "BUILD.release.bazel": "BUILD.bazel",
    },
    deps = [
        "//src/main/starlark/core/repositories:pkg",
    ],
)
