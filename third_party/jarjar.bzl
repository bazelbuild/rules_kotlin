def _jar_jar_impl(ctx):
    ctx.actions.run(
        inputs = [ctx.file.rules, ctx.file.input_jar],
        outputs = [ctx.outputs.jar],
        executable = ctx.executable.jarjar_runner,
        progress_message = "jarjar %s" % ctx.label,
        arguments = ["process", ctx.file.rules.path, ctx.file.input_jar.path, ctx.outputs.jar.path],
    )

    return [
        JavaInfo(
            output_jar = ctx.outputs.jar,
            compile_jar = ctx.outputs.jar,
        ),
        DefaultInfo(files = depset([ctx.outputs.jar])),
    ]

jar_jar = rule(
    implementation = _jar_jar_impl,
    attrs = {
        "input_jar": attr.label(allow_single_file = True),
        "rules": attr.label(allow_single_file = True),
        "jarjar_runner": attr.label(
            executable = True,
            cfg = "host",
            default = Label("@io_bazel_rules_kotlin//third_party:jarjar_runner"),
        ),
    },
    outputs = {
        "jar": "%{name}.jar",
    },
    provides = [JavaInfo],
)
