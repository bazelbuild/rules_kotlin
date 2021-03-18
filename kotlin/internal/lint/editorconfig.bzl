load(":ktlint_config.bzl", "KtlintConfigInfo")

def get_editorconfig(config):
    return config[KtlintConfigInfo].editorconfig if config else None
