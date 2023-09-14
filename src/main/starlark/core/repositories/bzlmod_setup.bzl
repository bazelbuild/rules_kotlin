"""Definitions for bzlmod module extensions."""

load(
    "@bazel_tools//tools/build_defs/repo:http.bzl",
    "http_archive",
)
load(
    "//src/main/starlark/core/repositories:initialize.bzl",
    _kotlin_repositories = "kotlin_repositories",
    _kotlinc_version = "kotlinc_version",
    _ksp_version = "ksp_version",
)
load(
    "//src/main/starlark/core/repositories:versions.bzl",
    _versions = "versions",
)

_kotlinc_version_tag = tag_class(
    attrs = {
        "version": attr.string(
            mandatory = True,
            default = _versions.KOTLIN_CURRENT_COMPILER_RELEASE.version,
        ),
        "sha256": attr.string(
            mandatory = True,
            default = _versions.KOTLIN_CURRENT_COMPILER_RELEASE.sha256,
        ),
    },
)

_ksp_version_tag = tag_class(
    attrs = {
        "version": attr.string(
            mandatory = True,
            default = _versions.KSP_CURRENT_COMPILER_PLUGIN_RELEASE.version,
        ),
        "sha256": attr.string(
            mandatory = True,
            default = _versions.KSP_CURRENT_COMPILER_PLUGIN_RELEASE.sha256,
        ),
    },
)

def _extra_repositories():
    # TODO(bencodes) This really needs to be a development dependency
    # This tarball intentionally does not have a SHA256 because the upstream URL can change without notice
    # For more context: https://github.com/bazelbuild/bazel-toolchains/blob/0c1f7c3c5f9e63f1e0ee91738b964937eea2d3e0/WORKSPACE#L28-L32
    http_archive(
        name = "buildkite_config",
        urls = _versions.RBE.URLS,
    )

def _rules_kotlin_extensions_impl(mctx):
    kotlinc_version = None
    ksp_version = None
    for mod in mctx.modules:
        for override in mod.tags.kotlinc_version:
            if kotlinc_version:
                fail("Only one kotlinc_version is supported right now!")
            kotlinc_version = _kotlinc_version(release = override.version, sha256 = override.sha256)
        for override in mod.tags.ksp_version:
            if ksp_version:
                fail("Only one ksp_version is supported right now!")
            ksp_version = _ksp_version(release = override.version, sha256 = override.sha256)

    _kotlin_repositories_args = dict(is_bzlmod = True)
    if kotlinc_version:
        _kotlin_repositories_args["compiler_release"] = kotlinc_version
    if ksp_version:
        _kotlin_repositories_args["ksp_compiler_release"] = ksp_version
    _kotlin_repositories(**_kotlin_repositories_args)

    _extra_repositories()

rules_kotlin_extensions = module_extension(
    implementation = _rules_kotlin_extensions_impl,
    tag_classes = {
        "kotlinc_version": _kotlinc_version_tag,
        "ksp_version": _ksp_version_tag,
    },
)
