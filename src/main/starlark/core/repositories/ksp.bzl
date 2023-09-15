_JARS_INSIDE_REPO = [
    "symbol-processing",
    "symbol-processing-api",
    "symbol-processing-cmdline",
]

def _ksp_compiler_plugin_repository_impl(repository_ctx):
    """Creates the KSP repository."""
    attr = repository_ctx.attr
    repository_ctx.download_and_extract(
        attr.urls,
        sha256 = attr.sha256,
    )

    # Move the jars that we need into the root of the workspace and strip the
    # version specific information out of the file name.
    for jar in _JARS_INSIDE_REPO:
        args = [
            "mv",
            "com/google/devtools/ksp/{jar}/{version}/{jar}-{version}.jar".format(jar = jar, version = attr.strip_version),
            "{jar}.jar".format(jar = jar),
        ]
        repository_ctx.execute(args, quiet = False)
    repository_ctx.delete("com")

    repository_ctx.file(
        "WORKSPACE",
        content = """workspace(name = "%s")""" % attr.name,
    )
    repository_ctx.template(
        "BUILD.bazel",
        attr._template,
        executable = False,
    )

ksp_compiler_plugin_repository = repository_rule(
    implementation = _ksp_compiler_plugin_repository_impl,
    attrs = {
        "urls": attr.string_list(
            doc = "A list of urls for the kotlin compiler",
            mandatory = True,
        ),
        "sha256": attr.string(
            doc = "sha256 of the compiler archive",
        ),
        "strip_version": attr.string(
            doc = "version to strip from the path.",
            mandatory = True,
        ),
        "_template": attr.label(
            doc = "repository build file template",
            default = "//src/main/starlark/core/repositories:BUILD.com_github_google_ksp.bazel",
        ),
    },
)
