load(
    "//kotlin/internal/repositories:release_repositories.bzl",
    _kotlin_repositories = "kotlin_repositories",
)
load(
    "//kotlin/internal/repositories:versions.bzl",
    _versions = "versions",
)

kotlin_repositories = _kotlin_repositories
versions = _versions
