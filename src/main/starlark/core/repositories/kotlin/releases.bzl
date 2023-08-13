"""Kotlinc releases indexed by major versions."""

load("//src/main/starlark/core/repositories:versions.bzl", "versions")

# Index of major kotlinc revision to calculated repository name and release.
KOTLINC_INDEX = {
    major: struct(
        # defining the expected repository name to reduce toil when updating.
        repository_name = "com_github_jetbrains_kotlin_%s" % major.replace(".", "_"),
        release = release,
    )
    for (major, release) in [
        (versions.get_major(compiler_release.version), compiler_release)
        for compiler_release in versions.KOTLIN_COMPILER_RELEASES
    ]
}
