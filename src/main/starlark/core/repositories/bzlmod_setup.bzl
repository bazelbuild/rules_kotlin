"""Definitions for bzlmod module extensions."""

load(
    "@bazel_tools//tools/build_defs/repo:http.bzl",
    "http_archive",
)
load(
    "//src/main/starlark/core/repositories:initialize.bzl",
    _kotlin_repositories = "kotlin_repositories",
    _kotlinc_version_impl = "kotlinc_version",
    _ksp_version_impl = "ksp_version",
)
load(
    "//src/main/starlark/core/repositories:versions.bzl",
    _versions = "versions",
)

_kotlinc_version = tag_class(
    attrs = {
        "version": attr.string(
            mandatory = True,
            default = "1.8.21",
        ),
        "url_templates": attr.string_list(
            mandatory = True,
            default = [
                "https://github.com/JetBrains/kotlin/releases/download/v{version}/kotlin-compiler-{version}.zip",
            ],
        ),
        "sha256": attr.string(
            mandatory = True,
            default = "6e43c5569ad067492d04d92c28cdf8095673699d81ce460bd7270443297e8fd7",
        ),
    },
)

_ksp_version = tag_class(
    attrs = {
        "version": attr.string(
            mandatory = True,
            default = "1.8.21-1.0.11",
        ),
        "url_templates": attr.string_list(
            mandatory = True,
            default = [
                "https://github.com/google/ksp/releases/download/{version}/artifacts.zip",
            ],
        ),
        "sha256": attr.string(
            mandatory = True,
            default = "81a609b48fddd4431bac2abe3570e36f79b1266672be30b581a0595c3fb2e615",
        ),
    },
)

def _rules_kotlin_extensions_impl(mctx):
    kotlinc_version = {}
    ksp_version = {}
    for mod in mctx.modules:
        for override in mod.tags.kotlinc_version:
            if kotlinc_version:
                fail("Nope!")
            kotlinc_version["version"] = override.version
            kotlinc_version["url_templates"] = override.url_templates
            kotlinc_version["sha256"] = override.sha256
        for override in mod.tags.ksp_version:
            if ksp_version:
                fail("Nope!")
            ksp_version["version"] = override.version
            ksp_version["url_templates"] = override.url_templates
            ksp_version["sha256"] = override.sha256

    _kotlin_repositories(
        bzlmod = True,
        compiler_release = _kotlinc_version_impl(
            release = kotlinc_version["version"],
            sha256 = kotlinc_version["sha256"],
        ),
    )

    http_archive(
        name = "build_bazel_rules_android",
        sha256 = _versions.ANDROID.SHA,
        strip_prefix = "rules_android-%s" % _versions.ANDROID.VERSION,
        urls = _versions.ANDROID.URLS,
    )

    http_archive(
        name = "buildkite_config",
        urls = _versions.RBE.URLS,
    )

    pass

rules_kotlin_extensions = module_extension(
    implementation = _rules_kotlin_extensions_impl,
    tag_classes = {
        "kotlinc_version": _kotlinc_version,
        "ksp_version": _ksp_version,
    },
)
