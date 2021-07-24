# Defines a repository rule that exports rules_android bzl files and well as wrappers.

LOAD = """
load("@bazel_skylib//:bzl_library.bzl", "bzl_library")
"""

RULE_TEMPLATE = """
bzl_library(
    name = "{name}",
    srcs = glob(["**/*.bzl"]),
    visibility = ["//visibility:public"],
)
"""

def _rules_stardoc_repository_impl(repository_ctx):
    attrs = repository_ctx.attr
    repository_ctx.download_and_extract(
        url = attrs.urls,
        sha256 = attrs.sha256,
        stripPrefix = attrs.strip_prefix,
    )
    print("%s prepping" % attrs.name)
    for src in attrs.starlark_packages:
        path = repository_ctx.path(src)
        build = path.get_child("BUILD")
        contents = ""
        if build.exists:
            contents = repository_ctx.read(build)
        print("%s prepping %s" % (attrs.name, contents))
        print("%s", path.readdir())
        repository_ctx.file(
            "%s/BUILD" % path,
            content = LOAD + contents + RULE_TEMPLATE.format(
                name = path.basename,
            ),
        )

rules_stardoc_repository = repository_rule(
    implementation = _rules_stardoc_repository_impl,
    attrs = {
        "sha256": attr.string(),
        "strip_prefix": attr.string(),
        "urls": attr.string_list(),
        "starlark_packages": attr.string_list(),
    },
)
