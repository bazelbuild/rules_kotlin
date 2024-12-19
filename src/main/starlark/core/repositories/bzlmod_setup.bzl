"""Definitions for bzlmod module extensions."""

load("@bazel_skylib//lib:modules.bzl", "modules")
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
load(
    "//src/main/starlark/core/repositories:initialize.release.bzl",
    _kotlin_repositories = "kotlin_repositories",
    _kotlinc_version = "kotlinc_version",
    _ksp_version = "ksp_version",
)
load("//src/main/starlark/core/repositories:versions.bzl", _versions = "versions")
load(":bzlmod_impl.bzl", "configure_modules_and_repositories", "tag_classes")

def _rules_kotlin_extensions_impl(mctx):
    configure_modules_and_repositories(
        mctx.modules,
        _kotlin_repositories,
        _kotlinc_version,
        _ksp_version,
    )

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
        ] + [
            # remove js stdlib from toolchain..
            "perl -i -pe 's/Label\\(\"\\/\\/kotlin\\/compiler:kotlin-stdlib-js\"\\),//g' kotlin/internal/toolchains.bzl",
        ],
    )

    return modules.use_all_repos(mctx, reproducible = True)

rules_kotlin_extensions = module_extension(
    implementation = _rules_kotlin_extensions_impl,
    tag_classes = tag_classes,
)
