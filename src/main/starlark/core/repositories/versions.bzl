# All versions for development and release
load("@bazel_tools//tools/build_defs/repo:utils.bzl", "maybe")

version = provider(
    fields = {
        "url_templates": "list of string templates with the placeholder {version}",
        "version": "the version in the form \\D+.\\D+.\\D+(.*)",
        "sha256": "sha256 checksum for the version being downloaded.",
        "strip_prefix_template": "string template with the placeholder {version}.",
    },
)

def _use_repository(name, version, rule, **kwargs):
    http_archive_arguments = dict(kwargs)
    http_archive_arguments["sha256"] = version.sha256
    http_archive_arguments["urls"] = [u.format(version = version.version) for u in version.url_templates]
    if (hasattr(version, "strip_prefix_template")):
        http_archive_arguments["strip_prefix"] = version.strip_prefix_template.format(version = version.version)

    maybe(rule, name = name, **http_archive_arguments)

versions = struct(
    BAZEL_TOOLCHAINS_VERSION = "4.1.0",
    BAZEL_TOOLCHAINS_SHA = "179ec02f809e86abf56356d8898c8bd74069f1bd7c56044050c2cd3d79d0e024",
    # IMPORTANT! rules_kotlin does not use the bazel_skylib unittest in production
    # This means the bazel_skylib_workspace call is skipped, as it only registers the unittest
    # toolchains. However, if a new workspace dependency is introduced, this precondition will fail.
    # Why skip it? Because it would introduce a 3rd function call to rules kotlin setup:
    # 1. Download archive
    # 2. Download dependencies and Configure rules
    # --> 3. Configure dependencies <--
    SKYLIB_VERSION = "1.7.1",
    SKYLIB_SHA = "bc283cdfcd526a52c3201279cda4bc298652efa898b10b4db0837dc51652756f",
    PROTOBUF_VERSION = "3.11.3",
    PROTOBUF_SHA = "cf754718b0aa945b00550ed7962ddc167167bd922b842199eeb6505e6f344852",
    RULES_JVM_EXTERNAL_TAG = "5.3",
    RULES_JVM_EXTERNAL_SHA = "d31e369b854322ca5098ea12c69d7175ded971435e55c18dd9dd5f29cc5249ac",
    RULES_PROTO = version(
        version = "5.3.0-21.7",
        sha256 = "dc3fb206a2cb3441b485eb1e423165b231235a1ea9b031b4433cf7bc1fa460dd",
        strip_prefix_template = "rules_proto-{version}",
        url_templates = [
            "https://github.com/bazelbuild/rules_proto/archive/refs/tags/{version}.tar.gz",
        ],
    ),
    IO_BAZEL_STARDOC = version(
        version = "0.5.6",
        sha256 = "dfbc364aaec143df5e6c52faf1f1166775a5b4408243f445f44b661cfdc3134f",
        url_templates = [
            "https://mirror.bazel.build/github.com/bazelbuild/stardoc/releases/download/{version}/stardoc-{version}.tar.gz",
            "https://github.com/bazelbuild/stardoc/releases/download/{version}/stardoc-{version}.tar.gz",
        ],
    ),
    PINTEREST_KTLINT = version(
        version = "1.3.0",
        url_templates = [
            "https://github.com/pinterest/ktlint/releases/download/{version}/ktlint",
        ],
        sha256 = "01b2e0ef893383a50dbeb13970fe7fa3be36ca3e83259e01649945b09d736985",
    ),
    KOTLIN_CURRENT_COMPILER_RELEASE = version(
        version = "1.9.23",
        url_templates = [
            "https://github.com/JetBrains/kotlin/releases/download/v{version}/kotlin-compiler-{version}.zip",
        ],
        sha256 = "93137d3aab9afa9b27cb06a824c2324195c6b6f6179d8a8653f440f5bd58be88",
    ),
    KSP_CURRENT_COMPILER_PLUGIN_RELEASE = version(
        version = "1.9.23-1.0.20",
        url_templates = [
            "https://github.com/google/ksp/releases/download/{version}/artifacts.zip",
        ],
        sha256 = "ee0618755913ef7fd6511288a232e8fad24838b9af6ea73972a76e81053c8c2d",
    ),
    ANDROID = struct(
        VERSION = "0.1.1",
        SHA = "cd06d15dd8bb59926e4d65f9003bfc20f9da4b2519985c27e190cddc8b7a7806",
        URLS = ["https://github.com/bazelbuild/rules_android/archive/v%s.zip" % "0.1.1"],
    ),
    # To update: https://github.com/bazelbuild/bazel-toolchains#latest-bazel-and-latest-ubuntu-1604-container
    RBE = struct(
        # This tarball intentionally does not have a SHA256 because the upstream URL can change without notice
        # For more context: https://github.com/bazelbuild/bazel-toolchains/blob/0c1f7c3c5f9e63f1e0ee91738b964937eea2d3e0/WORKSPACE#L28-L32
        URLS = ["https://storage.googleapis.com/rbe-toolchain/bazel-configs/rbe-ubuntu1604/latest/rbe_default.tar"],
    ),
    PKG = version(
        version = "0.7.0",
        url_templates = [
            "https://github.com/bazelbuild/rules_pkg/releases/download/{version}/rules_pkg-{version}.tar.gz",
        ],
        sha256 = "8a298e832762eda1830597d64fe7db58178aa84cd5926d76d5b744d6558941c2",
    ),
    # needed for rules_pkg and java
    RULES_KOTLIN = version(
        version = "1.9.0",
        url_templates = [
            "https://github.com/bazelbuild/rules_kotlin/releases/download/v{version}/rules_kotlin-v{version}.tar.gz",
        ],
        sha256 = "5766f1e599acf551aa56f49dab9ab9108269b03c557496c54acaf41f98e2b8d6",
    ),
    # needed for rules_pkg and java
    RULES_PYTHON = version(
        version = "0.23.1",
        strip_prefix_template = "rules_python-{version}",
        url_templates = [
            "https://github.com/bazelbuild/rules_python/archive/refs/tags/{version}.tar.gz",
        ],
        sha256 = "84aec9e21cc56fbc7f1335035a71c850d1b9b5cc6ff497306f84cced9a769841",
    ),
    # needed for rules_pkg and java
    RULES_JAVA = version(
        version = "7.2.0",
        url_templates = [
            "https://github.com/bazelbuild/rules_java/releases/download/{version}/rules_java-{version}.tar.gz",
        ],
        sha256 = "eb7db63ed826567b2ceb1ec53d6b729e01636f72c9f5dfb6d2dfe55ad69d1d2a",
    ),
    RULES_LICENSE = version(
        version = "0.0.3",
        url_templates = [
            "https://mirror.bazel.build/github.com/bazelbuild/rules_license/releases/download/{version}/rules_license-{version}.tar.gz",
            "https://github.com/bazelbuild/rules_license/releases/download/{version}/rules_license-{version}.tar.gz",
        ],
        sha256 = None,
    ),
    RULES_TESTING = version(
        version = "0.5.0",
        url_templates = [
            "https://github.com/bazelbuild/rules_testing/releases/download/v{version}/rules_testing-v{version}.tar.gz",
        ],
        sha256 = "b84ed8546f1969d700ead4546de9f7637e0f058d835e47e865dcbb13c4210aed",
    ),
    use_repository = _use_repository,
)
