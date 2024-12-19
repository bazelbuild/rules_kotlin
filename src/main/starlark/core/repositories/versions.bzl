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
    SKYLIB_VERSION = "1.4.2",
    SKYLIB_SHA = "66ffd9315665bfaafc96b52278f57c7e2dd09f5ede279ea6d39b2be471e7e3aa",
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
        version = "1.3.1",
        url_templates = [
            "https://github.com/pinterest/ktlint/releases/download/{version}/ktlint",
        ],
        sha256 = "a9f923be58fbd32670a17f0b729b1df804af882fa57402165741cb26e5440ca1",
    ),
    KOTLIN_CURRENT_COMPILER_RELEASE = version(
        version = "2.0.20",
        url_templates = [
            "https://github.com/JetBrains/kotlin/releases/download/v{version}/kotlin-compiler-{version}.zip",
        ],
        sha256 = "5f5d2a8ad6a718a002acd0775b67a9e27035872fdbd4b0791e3cb3ea00095931",
    ),
    KSP_CURRENT_COMPILER_PLUGIN_RELEASE = version(
        version = "2.0.20-1.0.24",
        url_templates = [
            "https://github.com/google/ksp/releases/download/{version}/artifacts.zip",
        ],
        sha256 = "831e83824c3a0bc5786d6aaf462ad29e0fc1a708e4fc314035f8444d850206d2",
    ),
    ANDROID = struct(
        VERSION = "0.1.1",
        SHA = "cd06d15dd8bb59926e4d65f9003bfc20f9da4b2519985c27e190cddc8b7a7806",
        URLS = ["https://github.com/bazelbuild/rules_android/archive/v%s.zip" % "0.1.1"],
    ),
    # To update: https://github.com/bazelbuild/bazel-toolchains#latest-bazel-and-latest-ubuntu-1604-container
    BAZELCI_RULES = struct(
        version = "1.0.0",
        sha256 = "eca21884e6f66a88c358e580fd67a6b148d30ab57b1680f62a96c00f9bc6a07e",
        # This tarball intentionally does not have a SHA256 because the upstream URL can change without notice
        # For more context: https://github.com/bazelbuild/bazel-toolchains/blob/0c1f7c3c5f9e63f1e0ee91738b964937eea2d3e0/WORKSPACE#L28-L32
        URLS = ["https://github.com/bazelbuild/continuous-integration/releases/download/rules-{version}/bazelci_rules-{version}.tar.gz"],
    ),
    PKG = version(
        version = "1.0.1",
        url_templates = [
            "https://github.com/bazelbuild/rules_pkg/releases/download/{version}/rules_pkg-{version}.tar.gz",
        ],
        sha256 = "d20c951960ed77cb7b341c2a59488534e494d5ad1d30c4818c736d57772a9fef",
    ),
    # needed for rules_pkg and java
    RULES_KOTLIN = version(
        version = "1.9.6",
        url_templates = [
            "https://github.com/bazelbuild/rules_kotlin/releases/download/v{version}/rules_kotlin-v{version}.tar.gz",
        ],
        sha256 = "3b772976fec7bdcda1d84b9d39b176589424c047eb2175bed09aac630e50af43",
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
        version = "0.6.0",
        url_templates = [
            "https://github.com/bazelbuild/rules_testing/releases/download/v{version}/rules_testing-v{version}.tar.gz",
        ],
        sha256 = "02c62574631876a4e3b02a1820cb51167bb9cdcdea2381b2fa9d9b8b11c407c4",
    ),
    KOTLINX_SERIALIZATION_CORE_JVM = version(
        version = "1.6.3",
        url_templates = [
            "https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-serialization-core-jvm/{version}/kotlinx-serialization-core-jvm-{version}.jar",
        ],
        sha256 = "29c821a8d4e25cbfe4f2ce96cdd4526f61f8f4e69a135f9612a34a81d93b65f1",
    ),
    KOTLINX_SERIALIZATION_JSON = version(
        version = "1.6.3",
        url_templates = [
            "https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-serialization-json/{version}/kotlinx-serialization-json-{version}.jar",
        ],
        sha256 = "8c0016890a79ab5980dd520a5ab1a6738023c29aa3b6437c482e0e5fdc06dab1",
    ),
    KOTLINX_SERIALIZATION_JSON_JVM = version(
        version = "1.6.3",
        url_templates = [
            "https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-serialization-json-jvm/{version}/kotlinx-serialization-json-jvm-{version}.jar",
        ],
        sha256 = "d3234179bcff1886d53d67c11eca47f7f3cf7b63c349d16965f6db51b7f3dd9a",
    ),
    RULES_BAZEL_INTEGRATION_TEST = version(
        version = "0.26.0",
        url_templates = [
            "https://github.com/bazel-contrib/rules_bazel_integration_test/releases/download/v{version}/rules_bazel_integration_test.v{version}.tar.gz",
        ],
        sha256 = "ab56cdd55a28781287242c7124ce9ff791ae8318ed641057f10edd98c55d7ed5",
    ),
    CGRINDEL_BAZEL_STARLIB = version(
        version = "0.21.0",
        sha256 = "43e375213dabe0c3928e65412ea7ec16850db93285c8c6f8b0eaa41cacd0f882",
        url_templates = [
            "https://github.com/cgrindel/bazel-starlib/releases/download/v{version}/bazel-starlib.v{version}.tar.gz",
        ],
    ),
    RULES_CC = version(
        version = "0.0.9",
        url_templates = ["https://github.com/bazelbuild/rules_cc/releases/download/{version}/rules_cc-{version}.tar.gz"],
        sha256 = "2037875b9a4456dce4a79d112a8ae885bbc4aad968e6587dca6e64f3a0900cdf",
        strip_prefix_template = "rules_cc-{version}",
    ),
    use_repository = _use_repository,
)
