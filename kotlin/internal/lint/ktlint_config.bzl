KtlintConfigInfo = provider(
    fields = {
        "android_rules_enabled": "Turn on Android Kotlin Style Guide compatibility",
        "editorconfig": "Editor config file to use",
        "experimental_rules_enabled": "Turn on experimental rules (ktlint-ruleset-experimental)",
    },
)

def _ktlint_config_impl(ctx):
    return [
        KtlintConfigInfo(
            editorconfig = ctx.file.editorconfig,
            android_rules_enabled = ctx.attr.android_rules_enabled,
            experimental_rules_enabled = ctx.attr.experimental_rules_enabled,
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
        "android_rules_enabled": attr.bool(
            doc = "Turn on Android Kotlin Style Guide compatibility",
            default = False,
        ),
        "experimental_rules_enabled": attr.bool(
            doc = "Turn on experimental rules (ktlint-ruleset-experimental)",
            default = False,
        ),
    },
    doc = """Used to configure ktlint.

    `ktlint` can be configured to use a `.editorconfig`, as documented at
    https://github.com/pinterest/ktlint/#editorconfig""",
)
