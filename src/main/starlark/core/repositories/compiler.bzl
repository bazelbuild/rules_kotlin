"""
Defines kotlin compiler repositories.
"""

load("//src/main/starlark/core/repositories/kotlin:templates.bzl", "ARTIFACT_TEMPLATES", "TEMPLATES")

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

    # Select the appropriate artifact template based on compiler version
    artifact_template = _get_artifact_template(
        attr.compiler_version,
        [repository_ctx.path(at) for at in attr._artifact_templates],
    )
    repository_ctx.template(
        "artifacts.bzl",
        artifact_template,
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

def _coerce_int(string_value):
    digits = "".join([
        string_value[i]
        for i in range(len(string_value))
        if string_value[i].isdigit()
    ])
    return 0 if not digits else int(digits)

def _version(version_string):
    return tuple([
        _coerce_int(segment)
        for segment in version_string.split(".", 3)
    ])

def _parse_version(basename):
    if "capabilities" not in basename:
        return None
    version_string = basename[len("capabilities_"):basename.find(".bzl")]
    return _version(version_string)

def _get_capability_template(compiler_version, templates):
    version_index = {}
    target = _version(compiler_version)
    if len(target) > 2:
        target = target[0:2]
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

def _get_artifact_template(compiler_version, templates):
    """Select artifact template based on compiler version."""
    version_index = {}
    target = _version(compiler_version)
    if len(target) > 2:
        target = target[0:2]

    for template in templates:
        # Extract version from artifacts_X.Y.bzl format
        basename = template.basename
        if not basename.startswith("artifacts_"):
            continue
        version_str = basename[len("artifacts_"):basename.find(".bzl")]
        version = _version(version_str)
        if len(version) > 2:
            version = version[0:2]

        if target == version:
            return template
        version_index[version] = template

    # If no exact match, find the highest version that is <= target
    # If target is higher than all available, use the latest
    if version_index:
        sorted_versions = sorted(version_index.keys())

        # Find highest version <= target
        best_match = None
        for version in sorted_versions:
            if version <= target:
                best_match = version
            else:
                break

        # If no version <= target, use the latest (for newer compiler versions)
        if best_match:
            return version_index[best_match]
        else:
            return version_index[sorted_versions[-1]]

    # Default to first available template
    return templates[0] if templates else None

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
        "_artifact_templates": attr.label_list(
            doc = "List of artifact definition templates.",
            default = ARTIFACT_TEMPLATES,
        ),
        "_template": attr.label(
            doc = "repository build file template",
            default = ":BUILD.kotlin_capabilities.bazel",
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
