load("@rules_java//java:defs.bzl", "java_common")
load("//kotlin/internal/utils:windows.bzl", "create_windows_native_launcher_script")
load(":editorconfig.bzl", "get_editorconfig", "is_android_rules_enabled", "is_experimental_rules_enabled")
load(":ktlint_config.bzl", "KtlintConfigInfo")

def _ktlint(ctx, srcs, editorconfig, enabled_android_rules, enabled_experimental_rules):
    """Generates a test action linting the input files.

    To make the tests running on windows as well you have to add the `--enable_runfiles` flag to your `.bazelrc`.
    This requires running under elevated privileges (Admin rights), Windows 10 Creators Update (1703) or later system version, and enabling developer mode.

    ```
    test --enable_runfiles
    ```

    Args:
      ctx: analysis context.
      srcs: list of source files to be checked.
      editorconfig: editorconfig file to use (optional)
      enabled_android_rules: when true the android rules will be enabled in ktlint
      enabled_experimental_rules: when true the experimental rules will be enabled in ktlint

    Returns:
      A script running ktlint on the input files.
    """
    java_runtime_info = ctx.attr._javabase[java_common.JavaRuntimeInfo]
    args = []
    if editorconfig:
        args.append("--editorconfig={file}".format(file = editorconfig.short_path))
    if enabled_android_rules:
        args.append("--android")
    if enabled_experimental_rules:
        args.append("--experimental")
    args.append("--relative")

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
        enabled_android_rules = is_android_rules_enabled(ctx.attr.config),
        enabled_experimental_rules = is_experimental_rules_enabled(ctx.attr.config),
    )

    executable = ctx.actions.declare_file(ctx.attr.name)
    ctx.actions.write(executable, content = script, is_executable = True)
    transitive_files = ctx.attr._javabase[java_common.JavaRuntimeInfo].files
    windows_constraint = ctx.attr._windows_constraint[platform_common.ConstraintValueInfo]

    if ctx.target_platform_has_constraint(windows_constraint):
        launcher = create_windows_native_launcher_script(ctx, executable)
        transitive_files = depset([executable], transitive = [transitive_files])
    else:
        launcher = executable

    files = [ctx.executable._ktlint_tool] + ctx.files.srcs
    if editorconfig:
        files.append(editorconfig)

    return [
        DefaultInfo(
            runfiles = ctx.runfiles(
                files = files,
                transitive_files = transitive_files,
            ).merge(ctx.attr._ktlint_tool[DefaultInfo].default_runfiles),
            executable = launcher,
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
            cfg = "exec",
        ),
        "_javabase": attr.label(
            default = "@bazel_tools//tools/jdk:current_java_runtime",
        ),
        "_windows_constraint": attr.label(default = "@platforms//os:windows"),
    },
    doc = "Lint Kotlin files, and fail if the linter raises errors.",
    test = True,
    toolchains = [
        "@bazel_tools//tools/jdk:toolchain_type",
        "@bazel_tools//tools/sh:toolchain_type",
    ],
)
