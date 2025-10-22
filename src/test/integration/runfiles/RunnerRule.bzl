"""
Rule that executes a kt_jvm_binary as a tool in an action.
This reproduces the scenario from issue #1332 where kt_jvm_binary
fails to find the Java runtime when executed from another rule.
"""

def _runner_impl(ctx):
    # Run the kt binary as a tool in an action (sandboxed).
    # This is the scenario that was failing before the fix.
    out = ctx.actions.declare_file(ctx.label.name + ".out")
    ctx.actions.run_shell(
        inputs = [],
        tools = [ctx.executable.tool_bin],  # ensures the tool's runfiles are staged
        command = "{} > {}".format(ctx.executable.tool_bin.path, out.path),
        progress_message = "Running kt_jvm_binary as tool",
        outputs = [out],
    )
    return DefaultInfo(files = depset([out]))

runner = rule(
    implementation = _runner_impl,
    attrs = {
        "tool_bin": attr.label(
            executable = True,
            cfg = "exec",
            doc = "The kt_jvm_binary to execute as a tool",
        ),
    },
)
