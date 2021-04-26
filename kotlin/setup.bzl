load("@apple_rules_lint//lint:setup.bzl", "ruleset_lint_setup")

def kotlin_setup():
    # TODO: Remove this check when we upgrade to the next apple_rules_lint
    if not native.existing_rule("apple_linters"):
        ruleset_lint_setup()