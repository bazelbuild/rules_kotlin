"""Definitions for bzlmod module extensions."""

load(
    "@bazel_tools//tools/build_defs/repo:http.bzl",
    "http_archive",
)
load(
    "//src/main/starlark/core/repositories:initialize.release.bzl",
    _kotlin_repositories = "kotlin_repositories",
    _kotlinc_version = "kotlinc_version",
    _ksp_version = "ksp_version",
)
load(
    "//src/main/starlark/core/repositories:versions.bzl",
    _versions = "versions",
)

_version_tag = tag_class(
    attrs = {
        "version": attr.string(mandatory = True),
        "sha256": attr.string(mandatory = True),
    },
)

def _extra_repositories():
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

    _versions.use_repository(
        name = "released_rules_kotlin",
        rule = http_archive,
        version = _versions.RULES_KOTLIN,
        patch_cmds = [
            # without repo mapping, force remap the internal dependencies to use the correct version of kotlin
            "grep -rl '\"@*{repo}' src kotlin | xargs -I F perl -i -pe 's/\"(@*)({repo})/\"\\1released_\\2/g' F".format(
                repo = repo,
            )
            for repo in ["com_github_jetbrains_kotlin", "com_github_google_ksp"]
        ],
    )

    _extra_repositories()

rules_kotlin_extensions = module_extension(
    implementation = _rules_kotlin_extensions_impl,
    tag_classes = {
        "kotlinc_version": _version_tag,
        "ksp_version": _version_tag,
    },
)
