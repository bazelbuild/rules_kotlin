load(":editorconfig.bzl", "get_editorconfig")
load(":ktlint_config.bzl", "KtlintConfigInfo")

def _ktlint(ctx, srcs, editorconfig):
    """Generates a test action linting the input files.

    Args:
      ctx: analysis context.
      srcs: list of source files to be checked.
      editorconfig: editorconfig file to use (optional)

    Returns:
      A script running ktlint on the input files.
    """
    java_runtime_info = ctx.attr._javabase[java_common.JavaRuntimeInfo]
    args = []
    if editorconfig:
        args.append("--editorconfig={file}".format(file = editorconfig.short_path))

    for f in srcs:
        args.append(f.path)

    return "PATH=\"{path}/bin:$PATH\" ; {linter} {args}".format(
        path = java_runtime_info.java_home,
        linter = ctx.executable._ktlint_tool.short_path,
        args = " ".join(args),
    )

def _ktlint_test_impl(ctx):
    editorconfig = get_editorconfig(ctx.attr.config)

    script = _ktlint(
        ctx,
        srcs = ctx.files.srcs,
        editorconfig = editorconfig,
    )

    ctx.actions.write(
        output = ctx.outputs.executable,
        content = script,
    )

    files = [ctx.executable._ktlint_tool] + ctx.files.srcs
    if editorconfig:
        files.append(editorconfig)

    return [
        DefaultInfo(
            runfiles = ctx.runfiles(
                files = files,
                transitive_files = ctx.attr._javabase[java_common.JavaRuntimeInfo].files,
            ).merge(ctx.attr._ktlint_tool[DefaultInfo].default_runfiles),
            executable = ctx.outputs.executable,
        ),
    ]

ktlint_test = rule(
    _ktlint_test_impl,
    attrs = {
        "srcs": attr.label_list(
            allow_files = [".kt", ".kts"],
            doc = "Source files to lint",
            mandatory = True,
            allow_empty = False,
        ),
        "config": attr.label(
            doc = "ktlint_config to use",
            providers = [
                [KtlintConfigInfo],
            ],
        ),
        "_ktlint_tool": attr.label(
            default = "@com_github_pinterest_ktlint//file",
            executable = True,
            cfg = "host",
        ),
        "_javabase": attr.label(
            default = "@bazel_tools//tools/jdk:current_java_runtime",
        ),
    },
    doc = "Lint Kotlin files, and fail if the linter raises errors.",
    test = True,
    toolchains = [
        "@bazel_tools//tools/jdk:toolchain_type",
    ],
)
