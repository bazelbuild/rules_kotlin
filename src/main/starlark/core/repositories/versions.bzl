# All versions for development and release
load("@bazel_tools//tools/build_defs/repo:utils.bzl", "maybe")

# Centralized Kotlin version constant
_KOTLIN_VERSION = "2.3.0"

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
    KOTLIN_BUILD_TOOLS_IMPL = version(
        version = "2.3.20-Beta1",
        url_templates = [
            "https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-build-tools-impl/{version}/kotlin-build-tools-impl-{version}.jar",
        ],
        sha256 = "7026a0ef96e4e9e8355c7af3e2a9ef32b93b0e143cd6b4a01fa9ab9b6135c4d3",
    ),
    KOTLIN_BUILD_TOOLS_API = version(
        version = "2.3.20-Beta1",
        url_templates = [
            "https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-build-tools-api/{version}/kotlin-build-tools-api-{version}.jar",
        ],
        sha256 = "e38d7bb8b9afe9c27f018e97dec60328ff3ec20b62b5e961f1c865b7628dfb4a",
    ),
    KOTLIN_COMPILER_EMBEDDABLE = version(
        version = "2.3.20-Beta1",
        url_templates = [
            "https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-compiler-embeddable/{version}/kotlin-compiler-embeddable-{version}.jar",
        ],
        sha256 = "dee0d35840adc614a08289105361c1562caa2c0f5fc29ca1f9bb928b1b2f7d31",
    ),
    KOTLIN_ANNOTATION_PROCESSING_EMBEDDABLE = version(
        version = "2.3.20-Beta1",
        url_templates = [
            "https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-annotation-processing-embeddable/{version}/kotlin-annotation-processing-embeddable-{version}.jar",
        ],
        sha256 = "709775dcbcd5a27379aba6a08e66d1b85ebb82decbea82f6fd277b2d4e28fcd1",
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
    KOTLINX_COROUTINES_CORE_JVM = version(
        version = "1.10.2",
        url_templates = [
            "https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core-jvm/{version}/kotlinx-coroutines-core-jvm-{version}.jar",
        ],
        sha256 = "5ca175b38df331fd64155b35cd8cae1251fa9ee369709b36d42e0a288ccce3fd",
    ),
    KOTLIN_STDLIB = version(
        version = _KOTLIN_VERSION,
        url_templates = [
            "https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-stdlib/{version}/kotlin-stdlib-{version}.jar",
        ],
        sha256 = "887587c91713250ad52fe14ad9166d042c33835049890e9437f355ffc5a195b1",
    ),
    KOTLIN_REFLECT = version(
        version = _KOTLIN_VERSION,
        url_templates = [
            "https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-reflect/{version}/kotlin-reflect-{version}.jar",
        ],
        sha256 = "714df4be819545ff4de1f36aa2183e0dea94b4c8cdf7ca29e9c89919baf36362",
    ),
    KOTLIN_TEST = version(
        version = _KOTLIN_VERSION,
        url_templates = [
            "https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-test/{version}/kotlin-test-{version}.jar",
        ],
        sha256 = "ae298a046eb9db6facb448685cb29f6ca77ab52afff1169b3fc52519de74da28",
    ),
    KOTLIN_COMPILER = version(
        version = _KOTLIN_VERSION,
        url_templates = [
            "https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-compiler/{version}/kotlin-compiler-{version}.jar",
        ],
        sha256 = "03e377b3beffa83e26674d0663f746bfb969b197fd8aed9432cfd8abd60db0c5",
    ),
    KOTLIN_ANNOTATION_PROCESSING = version(
        version = _KOTLIN_VERSION,
        url_templates = [
            "https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-annotation-processing/{version}/kotlin-annotation-processing-{version}.jar",
        ],
        sha256 = "729f94db8f128281a17c9a59e8dae082232966caf7d48aa9ee2e95eb96dc556c",
    ),
    KOTLIN_JVM_ABI_GEN = version(
        version = _KOTLIN_VERSION,
        url_templates = [
            "https://repo1.maven.org/maven2/org/jetbrains/kotlin/jvm-abi-gen/{version}/jvm-abi-gen-{version}.jar",
        ],
        sha256 = "f9cbf473e00d9c17e8c18a6d54c17bc82ab2fe5f331a82118f0371a677b4f5ea",
    ),
    JETBRAINS_ANNOTATIONS = version(
        version = "13.0",
        url_templates = [
            "https://repo1.maven.org/maven2/org/jetbrains/annotations/{version}/annotations-{version}.jar",
        ],
        sha256 = "ace2a10dc8e2d5fd34925ecac03e4988b2c0f851650c94b8cef49ba1bd111478",
    ),
    KOTLIN_ALLOPEN_COMPILER_PLUGIN = version(
        version = _KOTLIN_VERSION,
        url_templates = [
            "https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-allopen-compiler-plugin/{version}/kotlin-allopen-compiler-plugin-{version}.jar",
        ],
        sha256 = "2615d7dba30c8cedeead122bb00722c92abae0a7708b66b09c537fa252ef0abc",
    ),
    KOTLIN_NOARG_COMPILER_PLUGIN = version(
        version = _KOTLIN_VERSION,
        url_templates = [
            "https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-noarg-compiler-plugin/{version}/kotlin-noarg-compiler-plugin-{version}.jar",
        ],
        sha256 = "41ec4525d6c604de22b40fb817c6437a931d2041aaee8140f4b60e961194b19c",
    ),
    KOTLIN_SAM_WITH_RECEIVER_COMPILER_PLUGIN = version(
        version = _KOTLIN_VERSION,
        url_templates = [
            "https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-sam-with-receiver-compiler-plugin/{version}/kotlin-sam-with-receiver-compiler-plugin-{version}.jar",
        ],
        sha256 = "0bcd9f009a5be75eb020ebe54e9b5314cc5d2e84779de699ffbbcb3f1f921caf",
    ),
    KOTLIN_SERIALIZATION_COMPILER_PLUGIN = version(
        version = _KOTLIN_VERSION,
        url_templates = [
            "https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-serialization-compiler-plugin/{version}/kotlin-serialization-compiler-plugin-{version}.jar",
        ],
        sha256 = "511c9fc07a7eb97ea8f7d66715cbfc573e7e7ef4b6688bc06d9c7d209d8d476a",
    ),
    KOTLIN_SCRIPT_RUNTIME = version(
        version = _KOTLIN_VERSION,
        url_templates = [
            "https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-script-runtime/{version}/kotlin-script-runtime-{version}.jar",
        ],
        sha256 = "db826961371d6605318d9c4e4bfcdbfac94c3a0892cdcabd55224870fead57f1",
    ),
    KOTLIN_PARCELIZE_COMPILER = version(
        version = _KOTLIN_VERSION,
        url_templates = [
            "https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-parcelize-compiler/{version}/kotlin-parcelize-compiler-{version}.jar",
        ],
        sha256 = "4b77694811be14b1fdef7012dc19a73cf5b049d1db449b05b61dafa68b3e8045",
    ),
    KOTLIN_PARCELIZE_RUNTIME = version(
        version = _KOTLIN_VERSION,
        url_templates = [
            "https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-parcelize-runtime/{version}/kotlin-parcelize-runtime-{version}.jar",
        ],
        sha256 = "aec9fabdfea276f359c2ca62ec487cabc7826316dffb5accc965372eef778246",
    ),
    use_repository = _use_repository,
)
