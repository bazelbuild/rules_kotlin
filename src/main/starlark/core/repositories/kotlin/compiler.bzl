load("//src/main/starlark/core/repositories:versions.bzl", "versions")

_CAPABILITIES_TEMPLATES = {
    "legacy": "capabilities_legacy.bzl.com_github_jetbrains_kotlin.bazel",  # keep first
    "1.4": "capabilities_1.4.bzl.com_github_jetbrains_kotlin.bazel",
    "1.5": "capabilities_1.5.bzl.com_github_jetbrains_kotlin.bazel",
    "1.6": "capabilities_1.6.bzl.com_github_jetbrains_kotlin.bazel",
    "1.7": "capabilities_1.7.bzl.com_github_jetbrains_kotlin.bazel",
    "1.8": "capabilities_1.8.bzl.com_github_jetbrains_kotlin.bazel",
}

def _kotlin_compiler_impl(repository_ctx):
    """Creates the kotlinc repository."""
    attr = repository_ctx.attr
    repository_ctx.download_and_extract(
        attr.urls,
        sha256 = attr.sha256,
        stripPrefix = "kotlinc",
    )
    repository_ctx.file(
        "WORKSPACE",
        content = """workspace(name = "%s")""" % attr.name,
    )
    repository_ctx.template(
        "BUILD.bazel",
        attr._template,
        substitutions = {
            "{{.KotlinRulesRepository}}": attr.kotlin_rules,
        },
        executable = False,
    )

    repository_ctx.template(
        "capabilities.bzl",
        _get_capability_template(attr.compiler_version, attr._capabilities_templates),
        executable = False,
    )

    repository_ctx.file(
        "version.bzl",
        """
        MAJOR_VERSION="%s"
        """.strip() % versions.get_major(attr.compiler_version),
        executable = False,
    )

def _get_capability_template(compiler_version, templates):
    major_version = versions.get_major(compiler_version)

    for ver, template in zip(_CAPABILITIES_TEMPLATES.keys(), templates):
        if ver == major_version:
            return template

    # After latest version
    if major_version > _CAPABILITIES_TEMPLATES.keys()[-1]:
        templates[-1]

    # Legacy
    return templates[0]

kotlin_compiler_repository = repository_rule(
    implementation = _kotlin_compiler_impl,
    attrs = {
        "urls": attr.string_list(
            doc = "A list of urls for the kotlin compiler",
            mandatory = True,
        ),
        "kotlin_rules": attr.string(
            doc = "target of the kotlin rules.",
            mandatory = True,
        ),
        "sha256": attr.string(
            doc = "sha256 of the compiler archive",
        ),
        "compiler_version": attr.string(
            doc = "compiler version",
        ),
        "_template": attr.label(
            doc = "repository build file template",
            default = "BUILD.com_github_jetbrains_kotlin.bazel",
        ),
        "_capabilities_templates": attr.label_list(
            doc = "compiler capabilities file templates",
            default = _CAPABILITIES_TEMPLATES.values(),
        ),
    },
)
