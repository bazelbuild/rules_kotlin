_JARS_INSIDE_REPO = [
    "symbol-processing-aa",
    "symbol-processing-common-deps",
    "symbol-processing-api",
]

def _ksp_compiler_plugin_repository_impl(repository_ctx):
    """Creates the KSP repository."""
    attr = repository_ctx.attr
    repository_ctx.download_and_extract(
        attr.urls,
        sha256 = attr.sha256,
        # Move the jars to the top level and remove verison information.
        rename_files = {
            "com/google/devtools/ksp/{jar}/{version}/{jar}-{version}.jar".format(
                jar = jar,
                version = attr.strip_version,
            ): "{jar}.jar".format(jar = jar)
            for jar in _JARS_INSIDE_REPO
        },
    )

    # Remove unused .pom and checksum files files.
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

    # Bazel <8.3.0 lacks repository_ctx.repo_metadata
    if not hasattr(repository_ctx, "repo_metadata"):
        return None

    return repository_ctx.repo_metadata(
        reproducible = attr.sha256 != "",
    )

ksp_compiler_plugin_repository = repository_rule(
    implementation = _ksp_compiler_plugin_repository_impl,
    attrs = {
        "sha256": attr.string(
            doc = "sha256 of the compiler archive",
        ),
        "strip_version": attr.string(
            doc = "version to strip from the path.",
            mandatory = True,
        ),
        "urls": attr.string_list(
            doc = "A list of urls for the kotlin compiler",
            mandatory = True,
        ),
        "_template": attr.label(
            doc = "repository build file template",
            default = "//src/main/starlark/core/repositories:BUILD.com_github_google_ksp.bazel",
        ),
    },
)
