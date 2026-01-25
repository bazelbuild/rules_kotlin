"""Rule to expose the shell toolchain's bash binary path."""

def _bash_path_impl(ctx):
    toolchain = ctx.toolchains["@bazel_tools//tools/sh:toolchain_type"]
    bash_path = toolchain.path

    # Write the path to a file that can be read at runtime
    path_file = ctx.actions.declare_file(ctx.label.name + ".path")
    ctx.actions.write(
        output = path_file,
        content = bash_path,
    )

    return [DefaultInfo(files = depset([path_file]))]

bash_path = rule(
    implementation = _bash_path_impl,
    toolchains = ["@bazel_tools//tools/sh:toolchain_type"],
)
