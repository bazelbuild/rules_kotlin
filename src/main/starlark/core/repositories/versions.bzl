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

def _use_repository(rule, name, version, **kwargs):
    rule_arguments = dict(kwargs)
    rule_arguments["sha256"] = version.sha256
    rule_arguments["urls"] = [u.format(version = version.version) for u in version.url_templates]
    if (hasattr(version, "strip_prefix_template")):
        rule_arguments["strip_prefix"] = version.strip_prefix_template.format(version = version.version)

    maybe(rule, name = name, **rule_arguments)

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
        version = "1.39.0",
        sha256 = "5ab1a90d09fd74555e0df22809ad589627ddff263cff82535815aa80ca3e3562",
        strip_prefix_template = "bazel_features-{version}",
        url_templates = [
            "https://github.com/bazel-contrib/bazel_features/releases/download/v{version}/bazel_features-v{version}.tar.gz",
        ],
    ),
    BAZEL_LIB = version(
        version = "3.1.0",
        sha256 = "fd0fe4df9b6b7837d5fd765c04ffcea462530a08b3d98627fb6be62a693f4e12",
        strip_prefix_template = "bazel-lib-{version}",
        url_templates = [
            "https://github.com/bazel-contrib/bazel-lib/releases/download/v{version}/bazel-lib-v{version}.tar.gz",
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
    # Used for kotlin capabilities repository (compiler version detection)
    KOTLIN_CURRENT_COMPILER_RELEASE = version(
        version = "2.3.20-Beta1",
        url_templates = [
            "https://github.com/JetBrains/kotlin/releases/download/v{version}/kotlin-compiler-{version}.zip",
        ],
        sha256 = "44dd417f868bd462ea3827931b5294d05f17d4708fd646de25ccebfa0c62f239",
    ),
    KSP_CURRENT_COMPILER_PLUGIN_RELEASE = version(
        version = "2.3.3",
        url_templates = [
            "https://github.com/google/ksp/releases/download/{version}/artifacts.zip",
        ],
        sha256 = "24cb0d869ab2ae9fcf630a747b6b7e662e4be26e8b83b9272f6f3c24813e0c5a",
    ),
    RULES_ANDROID = version(
        version = "0.7.0",
        url_templates = [
            "https://github.com/bazelbuild/rules_android/releases/download/v{version}/rules_android-v{version}.tar.gz",
        ],
        strip_prefix_template = "rules_android-{version}",
        sha256 = "ef1a446260b7f620e2aae11d4c96389369eb865ade01fcdd389a8196168b8d9b",
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
        version = "0.0.8",
        sha256 = "a58c8c1e53aec7d66498168b0525bfd87add4d3cfd18e3ed03d5bed929dd68fc",
        strip_prefix_template = "bazel-worker-api-{version}",
        url_templates = [
            "https://github.com/bazelbuild/bazel-worker-api/releases/download/v{version}/bazel-worker-api-v{version}.tar.gz",
        ],
    ),
    PY_ABSL = version(
        version = "2.1.0",
        sha256 = "8a3d0830e4eb4f66c4fa907c06edf6ce1c719ced811a12e26d9d3162f8471758",
        url_templates = [
            "https://github.com/abseil/abseil-py/archive/refs/tags/v{version}.tar.gz",
        ],
        strip_prefix_template = "abseil-py-{version}",
    ),
    RULES_CC = version(
        version = "0.0.16",
        url_templates = ["https://github.com/bazelbuild/rules_cc/releases/download/{version}/rules_cc-{version}.tar.gz"],
        sha256 = "bbf1ae2f83305b7053b11e4467d317a7ba3517a12cef608543c1b1c5bf48a4df",
        strip_prefix_template = "rules_cc-{version}",
    ),
    use_repository = _use_repository,
)
