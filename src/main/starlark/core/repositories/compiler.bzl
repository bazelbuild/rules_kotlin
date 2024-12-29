"""
Defines kotlin compiler repositories.
"""

load("@bazel_skylib//lib:versions.bzl", "versions")
load("//src/main/starlark/core/repositories/kotlin:templates.bzl", "TEMPLATES")

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
    template = _get_capability_template(
        attr.compiler_version,
        [repository_ctx.path(ct) for ct in attr._capability_templates],
    )
    repository_ctx.template(
        "capabilities.bzl",
        template,
        executable = False,
    )

def _parse_version(basename):
    if "capabilities" not in basename:
        return None
    version_string = basename[len("capabilities_"):basename.find(".bzl")]
    if version_string == "legacy":
        return (0, 0, 0)
    return versions.parse(version_string)

def _get_capability_template(compiler_version, templates):
    version_index = {}
    target = versions.parse(compiler_version)
    for template in templates:
        version = _parse_version(template.basename)
        if not version:
            continue

        if target == version:
            return template
        version_index[version] = template

    last_version = sorted(version_index.keys(), reverse = True)[0]

    # After latest version, chosen by major revision
    if target[0] >= last_version[0]:
        return version_index[last_version]

    # Legacy
    return version_index[(0, 0, 0)]

kotlin_capabilities_repository = repository_rule(
    implementation = _kotlin_capabilities_impl,
    attrs = {
        "git_repository_name": attr.string(
            doc = "Name of the repository containing kotlin compiler libraries",
        ),
        "compiler_version": attr.string(
            doc = "compiler version",
        ),
        "_capability_templates": attr.label_list(
            doc = "List of compiler capability templates.",
            default = TEMPLATES,
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
