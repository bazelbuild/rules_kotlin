"""
Defines kotlin compiler repositories.
"""

load("//src/main/starlark/core/repositories/kotlin:templates.bzl", "GENERATED_OPTS_TEMPLATES")

def _kotlin_capabilities_impl(repository_ctx):
    """Creates the kotlinc capabilities repository."""
    attr = repository_ctx.attr
    repository_ctx.file(
        "WORKSPACE",
        content = """workspace(name = "%s")""" % attr.name,
    )
    repository_ctx.template(
        "BUILD.bazel",
        attr._template,
        executable = False,
    )
    generated_opts_template = _get_template_by_version(
        attr.compiler_version,
        [repository_ctx.path(ct) for ct in attr._generated_opts_templates],
        "generated_opts",
    )
    if generated_opts_template:
        repository_ctx.template(
            "generated_opts.bzl",
            generated_opts_template,
            executable = False,
        )
    else:
        # For older Kotlin versions without generated opts, create an empty stub
        repository_ctx.file(
            "generated_opts.bzl",
            content = "# Generated options not available for this Kotlin version\nGENERATED_KOPTS = {}\n",
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

def _parse_version(basename, prefix):
    if prefix not in basename:
        return None
    version_string = basename[len(prefix + "_"):basename.find(".bzl")]
    return _version(version_string)

def _get_template_by_version(compiler_version, templates, prefix):
    """Get the appropriate template file for a given compiler version.

    Args:
        compiler_version: The kotlin compiler version string
        templates: List of template paths
        prefix: Prefix for template files (e.g., "generated_opts")

    Returns:
        The template path that best matches the compiler version, or None if no templates.
    """
    if not templates:
        return None

    version_index = {}
    target = _version(compiler_version)
    if len(target) > 2:
        target = target[0:2]
    for template in templates:
        version = _parse_version(template.basename, prefix)
        if not version:
            continue

        if target == version:
            return template
        version_index[version] = template

    if not version_index:
        return None

    last_version = sorted(version_index.keys(), reverse = True)[0]

    # After latest version, chosen by major revision
    if target[0] >= last_version[0]:
        return version_index[last_version]

    # Legacy
    legacy_key = (0, 0, 0)
    if legacy_key in version_index:
        return version_index[legacy_key]

    return None

kotlin_capabilities_repository = repository_rule(
    implementation = _kotlin_capabilities_impl,
    attrs = {
        "compiler_version": attr.string(
            doc = "compiler version",
        ),
        "_generated_opts_templates": attr.label_list(
            doc = "List of generated options templates.",
            default = GENERATED_OPTS_TEMPLATES,
        ),
        "_template": attr.label(
            doc = "repository build file template",
            default = ":BUILD.kotlin_capabilities.bazel",
        ),
    },
)
