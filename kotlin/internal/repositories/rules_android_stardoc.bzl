# Defines a repository rule that exports rules_android bzl files and well as wrappers.

BUILD_TEMPLATE = """
load("@bazel_skylib//:bzl_library.bzl", "bzl_library")

bzl_library(
    name = "{name}",
    srcs = glob(["**/*.bzl"]),
    visibility = ["//visibility:public"],
)

"""

def _rules_android_stardoc_repository_impl(repository_ctx):
    attrs = repository_ctx.attr
    repository_ctx.download_and_extract(
        url = attrs.urls,
        sha256 = attrs.sha,
        stripPrefix = attrs.strip_prefix,
    )
    repository_ctx.delete("android/BUILD")
    repository_ctx.file(
        "android/BUILD",
        content = BUILD_TEMPLATE.format(
            name = "android",
        ),
    )

rules_android_stardoc_repository = repository_rule(
    implementation = _rules_android_stardoc_repository_impl,
    attrs = {
        "sha": attr.string(),
        "strip_prefix": attr.string(),
        "urls": attr.string_list(),
    },
)
