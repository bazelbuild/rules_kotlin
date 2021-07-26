def _rules_repository_impl(repository_ctx):
    attrs = repository_ctx.attrs
    repository_ctx.extract(attrs.file.archive)

rules_repository = repository_rule(
    implementation = _rules_repository_impl,
    attrs = {
        "archive": attr.label(
            doc = "label to repository archive",
        ),
        "parent": attr.label(
            doc = "label to parent repository",
        ),
        "repo_mapping": attr.string_dict(
            doc = "dict of development path to qualified release path.",
        ),
    },
)
