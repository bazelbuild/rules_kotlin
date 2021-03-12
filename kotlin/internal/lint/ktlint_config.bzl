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
    doc = """Used to configure ktlint.

    `ktlint` can be configured to use a `.editorconfig`, as documented at
    https://github.com/pinterest/ktlint/#editorconfig"""
)
