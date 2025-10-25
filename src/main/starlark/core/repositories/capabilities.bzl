load("//src/main/starlark/core/repositories/kotlin:templates.bzl", "TEMPLATES")

def _kotlin_capabilities_impl(repository_ctx):
    """Creates the kotlinc or kotlin-native repository."""
    attr = repository_ctx.attr
    repository_ctx.file(
        "WORKSPACE",
        content = """workspace(name = "%s")""" % attr.name,
    )
    repository_ctx.template(
        "BUILD.bazel",
        attr.template,
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
        "template": attr.label(
            doc = "repository build file template",
            default = ":BUILD.kotlin_capabilities.bazel",
        ),
        "_artifacts_template": attr.label(
            doc = "kotlinc artifacts template",
            default = "//src/main/starlark/core/repositories/kotlin:artifacts.bzl",
        ),
    },
)
