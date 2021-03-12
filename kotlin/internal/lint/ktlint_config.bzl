KtlintConfigInfo = provider(
    fields = {
        "editorconfig": "Editor config file to use",
    },
)

def _ktlint_config_impl(ctx):
    return [
        KtlintConfigInfo(
            editorconfig = ctx.file.editorconfig,
        ),
    ]

ktlint_config = rule(
    _ktlint_config_impl,
    attrs = {
        "editorconfig": attr.label(
            doc = "Editor config file to use",
            mandatory = False,
            allow_single_file = True,
        ),
    },
)
