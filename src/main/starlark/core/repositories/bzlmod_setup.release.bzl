load(
    "//src/main/starlark/core/repositories:initialize.bzl",
    _kotlin_repositories = "kotlin_repositories",
    _kotlinc_version = "kotlinc_version",
    _ksp_version = "ksp_version",
)
load(
    ":bzlmod_impl.bzl",
    "configure_modules_and_repositories",
    "tag_classes",
)

def _rules_kotlin_extensions_impl(mctx):
    configure_modules_and_repositories(
        mctx.modules,
        _kotlin_repositories,
        _kotlinc_version,
        _ksp_version,
    )

rules_kotlin_extensions = module_extension(
    implementation = _rules_kotlin_extensions_impl,
    tag_classes = tag_classes,
)
