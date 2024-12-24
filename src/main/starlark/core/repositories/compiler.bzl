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
    template = _get_capability_template(
        attr.compiler_version,
        repository_ctx.path(attr._capability_templates_dir).readdir(),
    )
    repository_ctx.template(
        "capabilities.bzl",
        template,
        executable = False,
    )

def _parse_version(basename):
    if "capabilities" not in basename:
        return None
    return basename.split(".")[0].split("_")[1]

def _get_capability_template(compiler_version, templates):
    version_index = {}

    for template in templates:
        version = _parse_version(template.basename)
        if not version:
            continue
        if compiler_version.startswith(version):
            return template
        version_index[version] = template

    last_version = sorted(version_index.keys(), reverse = True)[0]

    # After latest version, chosen by major revision
    if int(compiler_version.split(".")[0]) >= int(last_version):
        return version_index[last_version]

    # Legacy
    return templates["legacy"]

kotlin_capabilities_repository = repository_rule(
    implementation = _kotlin_capabilities_impl,
    attrs = {
        "git_repository_name": attr.string(
            doc = "Name of the repository containing kotlin compiler libraries",
        ),
        "compiler_version": attr.string(
            doc = "compiler version",
        ),
        "_capability_templates_dir": attr.label(
            doc = "Compiler capability templates location. " +
                  "Since repository rules do not resolve Label to a " +
                  "Target, and glob is not available, this is a Label that " +
                  "can be resolved to a directory via repository_ctx.path.",
            default = "//src/main/starlark/core/repositories:kotlin",
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
