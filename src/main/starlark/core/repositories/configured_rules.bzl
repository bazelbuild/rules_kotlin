def _rules_repository_impl(repository_ctx):
    attr = repository_ctx.attr
    repository_ctx.extract(attr.archive)

rules_repository = repository_rule(
    implementation = _rules_repository_impl,
    attrs = {
        "archive": attr.label(
            doc = "label to repository archive",
            allow_single_file = True,
        ),
        "parent": attr.label(
            doc = "label to parent repository",
        ),
    },
)
