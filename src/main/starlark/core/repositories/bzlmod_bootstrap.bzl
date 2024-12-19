load("@released_rules_kotlin//src/main/starlark/core/repositories:initialize.bzl", "kotlin_repositories")
load("//src/main/starlark/core/repositories:versions.bzl", "versions")

def _rules_kotlin_bootstrap_extensions_impl(_):
    kotlin_repositories(
        is_bzlmod = True,
        compiler_repository_name = "released_com_github_jetbrains_kotlin",
        ksp_repository_name = "released_com_github_google_ksp",
        compiler_release = versions.KOTLIN_CURRENT_COMPILER_RELEASE,
        ksp_compiler_release = versions.KSP_CURRENT_COMPILER_PLUGIN_RELEASE,
    )

rules_kotlin_bootstrap_extensions = module_extension(
    implementation = _rules_kotlin_bootstrap_extensions_impl,
    tag_classes = {},
)
