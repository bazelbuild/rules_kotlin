# RELEASE-CONTENT-START
load(
    "//src/main/starlark/core/repositories:initialize.bzl",
    _kotlin_repositories = "kotlin_repositories",
    _kotlinc_version = "kotlinc_version",
    _ksp_version = "ksp_version",
    _versions = "versions",
)

kotlin_repositories = _kotlin_repositories
versions = _versions
kotlinc_version = _kotlinc_version
ksp_version = _ksp_version
# RELEASE-CONTENT-END

# Dev-only: load dev setup functions
load("//src/main/starlark/core/repositories:setup.bzl", "kt_configure")
load("//src/main/starlark/core/repositories:workspace_compat.bzl", "workspace_compat")

# Dev-only: wrapper that adds dev-specific setup
def kotlin_repositories(
        is_bzlmod = False,
        compiler_release = _versions.KOTLIN_CURRENT_COMPILER_RELEASE,
        ksp_compiler_release = _versions.KSP_CURRENT_COMPILER_PLUGIN_RELEASE):
    """Call this in the WORKSPACE file to setup the Kotlin rules.

    Args:
        compiler_release: (internal) version provider from versions.bzl.
        ksp_compiler_release: (internal) version provider from versions.bzl.
    """
    _kotlin_repositories(is_bzlmod = is_bzlmod, compiler_release = compiler_release, ksp_compiler_release = ksp_compiler_release)
    workspace_compat()
    kt_configure()
