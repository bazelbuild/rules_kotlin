"""
Defines kotlin compiler repositories.
"""

def _kotlin_compiler_impl(repository_ctx):
    attr = repository_ctx.attr
    repository_ctx.download_and_extract(
        attr.urls,
        sha256 = attr.sha256,
        stripPrefix = "kotlinc",
    )
    repository_ctx.template(
        "BUILD.bazel",
        attr._template,
        executable = False,
    )

def _kotlin_capabilities_impl(repository_ctx):
    """Creates the kotlinc repository."""
    attr = repository_ctx.attr
    repository_ctx.file(
        "WORKSPACE",
        content = """workspace(name = "%s")""" % attr.name,
    )
    repository_ctx.template(
        "BUILD.bazel",
        attr._template,
        executable = False,
        substitutions = {
            "$git_repo$": attr.git_repository_name,
        },
    )
    repository_ctx.template(
        "artifacts.bzl",
        attr._artifacts_template,
        executable = False,
    )
    repository_ctx.template(
        "capabilities.bzl",
        _get_capability_template(attr.compiler_version, attr._capabilities_templates),
        executable = False,
    )

def _get_capability_template(compiler_version, templates):
    for ver, template in zip(_CAPABILITIES_TEMPLATES.keys(), templates):
        if compiler_version.startswith(ver):
            return template

    # After latest version
    if compiler_version > _CAPABILITIES_TEMPLATES.keys()[-1]:
        templates[-1]

    # Legacy
    return templates[0]

_CAPABILITIES_TEMPLATES = {
    "legacy": "//src/main/starlark/core/repositories/kotlin:capabilities_legacy.bzl.com_github_jetbrains_kotlin.bazel",  # keep first
    "1.4": "//src/main/starlark/core/repositories/kotlin:capabilities_1.4.bzl.com_github_jetbrains_kotlin.bazel",
    "1.5": "//src/main/starlark/core/repositories/kotlin:capabilities_1.5.bzl.com_github_jetbrains_kotlin.bazel",
    "1.6": "//src/main/starlark/core/repositories/kotlin:capabilities_1.6.bzl.com_github_jetbrains_kotlin.bazel",
    "1.7": "//src/main/starlark/core/repositories/kotlin:capabilities_1.7.bzl.com_github_jetbrains_kotlin.bazel",
    "1.8": "//src/main/starlark/core/repositories/kotlin:capabilities_1.8.bzl.com_github_jetbrains_kotlin.bazel",
    "1.9": "//src/main/starlark/core/repositories/kotlin:capabilities_1.9.bzl.com_github_jetbrains_kotlin.bazel",
    "2.0": "//src/main/starlark/core/repositories/kotlin:capabilities_2.0.bzl.com_github_jetbrains_kotlin.bazel",
}

kotlin_capabilities_repository = repository_rule(
    implementation = _kotlin_capabilities_impl,
    attrs = {
        "git_repository_name": attr.string(
            doc = "Name of the repository containing kotlin compiler libraries",
        ),
        "compiler_version": attr.string(
            doc = "compiler version",
        ),
        "_capabilities_templates": attr.label_list(
            doc = "compiler capabilities file templates",
            default = _CAPABILITIES_TEMPLATES.values(),
        ),
        "_template": attr.label(
            doc = "repository build file template",
            default = ":BUILD.kotlin_capabilities.bazel",
        ),
        "_artifacts_template": attr.label(
            doc = "kotlinc artifacts template",
            default = "//src/main/starlark/core/repositories/kotlin:artifacts.bzl",
        ),
    },
)

kotlin_compiler_git_repository = repository_rule(
    implementation = _kotlin_compiler_impl,
    attrs = {
        "urls": attr.string_list(
            doc = "A list of urls for the kotlin compiler",
            mandatory = True,
        ),
        "sha256": attr.string(
            doc = "sha256 of the compiler archive",
        ),
        "_template": attr.label(
            doc = "repository build file template",
            default = ":BUILD.com_github_jetbrains_kotlin.bazel",
        ),
    },
)

def kotlin_compiler_repository(name, urls, sha256, compiler_version):
    """
    Creates two repositories, necessary for lazily loading the kotlin compiler binaries for git.
    """
    git_repo = name + "_git"
    kotlin_compiler_git_repository(
        name = git_repo,
        urls = urls,
        sha256 = sha256,
    )
    kotlin_capabilities_repository(
        name = name,
        git_repository_name = git_repo,
        compiler_version = compiler_version,
    )
