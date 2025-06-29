_URL_TEMPLATE = "https://github.com/JetBrains/kotlin/releases/download/v{version}/kotlin-native-prebuilt-{platform}-{version}.{extension}"
KOTLIN_NATIVE_PLATFORMS = ["linux-x86_64", "macos-x86_64", "macos-aarch64", "windows-x86_64"]

def _kotlin_native_compiler_repository(repo_ctx):
    archive_extension = "tar.gz"
    if repo_ctx.attr.platform == "windows-x86_64":
        # windows releases only have zip files
        archive_extension = "zip"

    url = _URL_TEMPLATE.format(platform = repo_ctx.attr.platform, version = repo_ctx.attr.version, extension = archive_extension)
    repo_ctx.download_and_extract(
        url = url,
        sha256 = repo_ctx.attr.sha256,
        output = ".",
        stripPrefix = "kotlin-native-prebuilt-{platform}-{version}".format(platform = repo_ctx.attr.platform, version = repo_ctx.attr.version),
    )

    repo_ctx.file("BUILD.bazel", """
exports_files(["bin/klib", "bin/kotlinc-native"])
                  """)

kotlin_native_compiler_repository = repository_rule(
    implementation = _kotlin_native_compiler_repository,
    attrs = {
        "version": attr.string(
            mandatory = True,
            doc = "The kotlin-native version to be downloaded",
        ),
        "platform": attr.string(
            mandatory = True,
            doc = "The platform for which to download kotlin-native for",
            values = KOTLIN_NATIVE_PLATFORMS,
        ),
        "sha256": attr.string(
            mandatory = True,
            doc = "the sha256 of the kotlin-native tar/zip for this platform/version.",
        ),
    },
)
