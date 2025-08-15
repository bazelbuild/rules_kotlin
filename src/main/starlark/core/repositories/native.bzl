load(":capabilities.bzl", "kotlin_capabilities_repository")

_CAPABILITIES_BUILD_TEMPLATE = Label("BUILD.kotlin-native_capabilities.bazel")

def _kotlin_native_compiler_repository_impl(repository_ctx):
    attr = repository_ctx.attr
    repository_ctx.download_and_extract(
        attr.urls,
        sha256 = attr.sha256,
        stripPrefix = attr.strip_prefix,
    )
    repository_ctx.template(
        "BUILD.bazel",
        attr._template,
        executable = False,
    )

kotlin_native_compiler_repository_rule = repository_rule(
    implementation = _kotlin_native_compiler_repository_impl,
    attrs = {
        "urls": attr.string_list(
            mandatory = True,
            doc = "A list of urls for the kotlin-native compiler",
        ),
        "sha256": attr.string(
            mandatory = True,
            doc = "the sha256 of the kotlin-native tar/zip for this platform/version.",
        ),
        "_template": attr.label(
            doc = "The build file template for the kotlin-native repository",
            default = ":BUILD.com_github_jetbrains_kotlin_native.bazel",
        ),
        "strip_prefix": attr.string(
            mandatory = True,
            doc = "The prefix to be stripped from the extracted kotlin-native compiler archive, and is platform specific",
        ),
    },
)

def kotlin_native_compiler_repository(name, compiler_version, **kwargs):
    git_repo = name + "_git"
    kotlin_native_compiler_repository_rule(
        name = git_repo,
        **kwargs
    )

    kotlin_capabilities_repository(
        name = name,
        git_repository_name = git_repo,
        compiler_version = compiler_version,
        template = _CAPABILITIES_BUILD_TEMPLATE,
    )
