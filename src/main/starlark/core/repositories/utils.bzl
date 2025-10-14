"""Local replacement for @bazel_tools//tools/build_defs/repo

Those tools have been deprecated in favor of language specific implementations. Given the current state of
the documentation, it's easier to re-implement here.
"""

def maybe(repo_rule, name, **kwargs):
    """Utility function for only adding a repository if it's not already present.

    This is to implement safe repositories.bzl macro documented in
    https://bazel.build/rules/deploying#dependencies.

    Args:
        repo_rule: repository rule function.
        name: name of the repository to create.
        **kwargs: remaining arguments that are passed to the repo_rule function.

    Returns:
        Nothing, defines the repository when needed as a side-effect.
    """
    if not native.existing_rule(name):
        repo_rule(name = name, **kwargs)

_HTTP_FILE_BUILD = """\
package(default_visibility = ["//visibility:public"])

filegroup(
    name = "file",
    srcs = glob(["{file_name}"]),
)
"""

_HTTP_JAR_BUILD = """\
load("@rules_java//java:java_import.bzl", "java_import")
%s
java_import(
  name = "jar",
  jars = [":file"],
)
""" % _HTTP_FILE_BUILD

_HTTP_ARCHIVE_BUILD = """\
package(default_visibility = ["//visibility:public"])

filegroup(
  name = "files",
  srcs = glob(["**/*", "*"]),
)
"""

def _resolve_type(type, r_ctx):
    if type == "archive":
        def download_and_extract(*args, **kwargs):
            return r_ctx.download_and_extract(
                strip_prefix = r_ctx.attr.strip_prefix,
                *args,
                **kwargs
            )

        return (_HTTP_ARCHIVE_BUILD, r_ctx.attr.path or "", download_and_extract)

    if type == "jar":
        template = _HTTP_JAR_BUILD
        path = "%s/%s" % (type, r_ctx.attr.path or "downloaded.jar")
    else:
        template = _HTTP_FILE_BUILD
        path = "%s/%s" % (type, r_ctx.attr.path or "downloaded")

    def download(*args, **kwargs):
        if r_ctx.attr.executable:
            kwargs["executable"] = r_ctx.attr.executable
        return r_ctx.download(*args, **kwargs)

    return (template, path, download)

def _http_repository(r_ctx):
    (default_template, path, download) = _resolve_type(r_ctx.attr.type, r_ctx)
    download_info = download(
        r_ctx.attr.urls,
        output = path,
        sha256 = r_ctx.attr.sha256,
        canonical_id = " ".join(r_ctx.attr.urls) + r_ctx.attr.sha256,
    )

    def write_if_absent(file, contents):
        file_path = r_ctx.path(path).dirname.get_child(file)
        if file_path.exists:
            return
        r_ctx.file(file_path, contents)

    write_if_absent("WORKSPACE", "workspace(name = \"{name}\")".format(name = r_ctx.name))
    write_if_absent("BUILD", default_template.format(file_name = r_ctx.path(path).basename))

    return r_ctx.repo_metadata(
        reproducible = r_ctx.attr.sha256 != "",
        attrs_for_reproducibility = {"integrity": download_info.integrity} if r_ctx.attr.sha256 == "" else {},
    ) if hasattr(r_ctx, "repo_metadata") else {
        "name": r_ctx.attr.name,
        "type": r_ctx.attr.type,
        "urls": r_ctx.attr.urls,
        "sha256": download_info.sha256,
    }

http_repository = repository_rule(
    implementation = _http_repository,
    attrs = {
        "type": attr.string(
            doc = "Type of remote dependency: file, archive, jar",
            values = ["file", "archive", "jar"],
            mandatory = True,
        ),
        "path": attr.string(
            doc = "Path in the repository to place the file or uncompress the archive.",
        ),
        "executable": attr.bool(
            doc = "Whether the file will be executable. Only applies to type=file.",
            default = False,
        ),
        "strip_prefix": attr.string(doc = "Prefix to remove. Only applies to type=archive."),
        "build_file_contents": attr.string(
            doc = "Build file contents.",
        ),
        "sha256": attr.string(
            doc = """The expected SHA-256 of the file downloaded.

        This must match the SHA-256 of the file downloaded. _It is a security risk
        to omit the SHA-256 as remote files can change._ At best omitting this
        field will make your build non-hermetic. It is optional to make development
        easier but should be set before shipping.""",
        ),
        "urls": attr.string_list(
            doc = "List of urls to download from.",
            mandatory = True,
        ),
    },
)
