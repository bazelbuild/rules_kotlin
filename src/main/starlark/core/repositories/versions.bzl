# All versions for development and release
load("@bazel_tools//tools/build_defs/repo:utils.bzl", "maybe")

version = provider(
    fields = {
        "sha256": "sha256 checksum for the version being downloaded.",
        "strip_prefix_template": "string template with the placeholder {version}.",
        "url_templates": "list of string templates with the placeholder {version}",
        "version": "the version in the form \\D+.\\D+.\\D+(.*)",
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
    # IMPORTANT! rules_kotlin does not use the bazel_skylib unittest in production
    # This means the bazel_skylib_workspace call is skipped, as it only registers the unittest
    # toolchains. However, if a new workspace dependency is introduced, this precondition will fail.
    # Why skip it? Because it would introduce a 3rd function call to rules kotlin setup:
    # 1. Download archive
    # 2. Download dependencies and Configure rules
    # --> 3. Configure dependencies <--
    BAZEL_SKYLIB = version(
        version = "1.7.1",
        sha256 = "bc283cdfcd526a52c3201279cda4bc298652efa898b10b4db0837dc51652756f",
        url_templates = [
            "https://github.com/bazelbuild/bazel-skylib/releases/download/{version}/bazel-skylib-{version}.tar.gz",
        ],
    ),
    BAZEL_FEATURES = version(
        version = "1.25.0",
        sha256 = "4fd9922d464686820ffd8fcefa28ccffa147f7cdc6b6ac0d8b07fde565c65d66",
        strip_prefix_template = "bazel_features-{version}",
        url_templates = [
            "https://github.com/bazel-contrib/bazel_features/releases/download/v{version}/bazel_features-v{version}.tar.gz",
        ],
    ),
    RULES_JVM_EXTERNAL = version(
        version = "6.6",
        sha256 = "3afe5195069bd379373528899c03a3072f568d33bd96fe037bd43b1f590535e7",
        strip_prefix_template = "rules_jvm_external-{version}",
        url_templates = [
            "https://github.com/bazelbuild/rules_jvm_external/releases/download/{version}/rules_jvm_external-{version}.tar.gz",
        ],
    ),
    COM_GOOGLE_PROTOBUF = version(
        version = "29.0",
        sha256 = "10a0d58f39a1a909e95e00e8ba0b5b1dc64d02997f741151953a2b3659f6e78c",
        strip_prefix_template = "protobuf-{version}",
        url_templates = [
            "https://github.com/protocolbuffers/protobuf/releases/download/v{version}/protobuf-{version}.tar.gz",
        ],
    ),
    RULES_PROTO = version(
        version = "7.0.2",
        sha256 = "0e5c64a2599a6e26c6a03d6162242d231ecc0de219534c38cb4402171def21e8",
        strip_prefix_template = "rules_proto-{version}",
        url_templates = [
            "https://github.com/bazelbuild/rules_proto/releases/download/{version}/rules_proto-{version}.tar.gz",
        ],
    ),
    PINTEREST_KTLINT = version(
        version = "1.6.0",
        url_templates = [
            "https://github.com/pinterest/ktlint/releases/download/{version}/ktlint",
        ],
        sha256 = "5ba1ac917a06b0f02daaa60d10abbedd2220d60216af670c67a45b91c74cf8bb",
    ),
    KOTLIN_CURRENT_COMPILER_RELEASE = version(
        version = "2.2.21",
        url_templates = [
            "https://github.com/JetBrains/kotlin/releases/download/v{version}/kotlin-compiler-{version}.zip",
        ],
        sha256 = "a623871f1cd9c938946948b70ef9170879f0758043885bbd30c32f024e511714",
    ),
    KSP_CURRENT_COMPILER_PLUGIN_RELEASE = version(
        version = "2.2.21-2.0.4",
        url_templates = [
            "https://github.com/google/ksp/releases/download/{version}/artifacts.zip",
        ],
        sha256 = "6550f1117d7c9590cc9a5075b92682a218c8e1df4093d7e683d73cc481733dd1",
    ),
    KOTLIN_BUILD_TOOLS_IMPL = version(
        version = "2.2.21",
        url_templates = [
            "https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-build-tools-impl/{version}/kotlin-build-tools-impl-{version}.jar",
        ],
        sha256 = "458b63ef8fc50a0a3180fe688ec1b995745992a05c495dac61e58d1088927a80",
    ),
    RULES_ANDROID = version(
        version = "0.6.4",
        url_templates = [
            "https://github.com/bazelbuild/rules_android/releases/download/v{version}/rules_android-v{version}.tar.gz",
        ],
        strip_prefix_template = "rules_android-{version}",
        sha256 = "4135e2fa37a94bb36c7801e33faef2934c9fe4f9a84d0035eacc4154c2c30e44",
    ),
    RULES_JAVA = version(
        version = "8.9.0",
        url_templates = [
            "https://github.com/bazelbuild/rules_java/releases/download/{version}/rules_java-{version}.tar.gz",
        ],
        sha256 = "8daa0e4f800979c74387e4cd93f97e576ec6d52beab8ac94710d2931c57f8d8b",
    ),
    RULES_LICENSE = version(
        version = "1.0.0",
        url_templates = [
            "https://mirror.bazel.build/github.com/bazelbuild/rules_license/releases/download/{version}/rules_license-{version}.tar.gz",
            "https://github.com/bazelbuild/rules_license/releases/download/{version}/rules_license-{version}.tar.gz",
        ],
        sha256 = "26d4021f6898e23b82ef953078389dd49ac2b5618ac564ade4ef87cced147b38",
    ),
    BAZEL_WORKER_API = version(
        version = "0.0.6",
        sha256 = "5aac6ae6a23015cc7984492a114dc539effc244ec5ac7f8f6b1539c15fb376eb",
        strip_prefix_template = "bazel-worker-api-{version}",
        url_templates = [
            "https://github.com/bazelbuild/bazel-worker-api/releases/download/v{version}/bazel-worker-api-v{version}.tar.gz",
        ],
    ),
    KOTLINX_SERIALIZATION_CORE_JVM = version(
        version = "1.8.1",
        url_templates = [
            "https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-serialization-core-jvm/{version}/kotlinx-serialization-core-jvm-{version}.jar",
        ],
        sha256 = "3565b6d4d789bf70683c45566944287fc1d8dc75c23d98bd87d01059cc76f2b3",
    ),
    KOTLINX_SERIALIZATION_JSON = version(
        version = "1.8.1",
        url_templates = [
            "https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-serialization-json/{version}/kotlinx-serialization-json-{version}.jar",
        ],
        sha256 = "58adf3358a0f99dd8d66a550fbe19064d395e0d5f7f1e46515cd3470a56fbbb0",
    ),
    KOTLINX_SERIALIZATION_JSON_JVM = version(
        version = "1.8.1",
        url_templates = [
            "https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-serialization-json-jvm/{version}/kotlinx-serialization-json-jvm-{version}.jar",
        ],
        sha256 = "8769e5647557e3700919c32d508f5c5dad53c5d8234cd10846354fbcff14aa24",
    ),
    use_repository = _use_repository,
)
