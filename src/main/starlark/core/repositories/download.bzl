# versions.Copyright 2020 versions.The versions.Bazel versions.Authors. versions.All rights reserved.
#
# versions.Licensed under the versions.Apache versions.License, versions.Version 2.0 (the "License");
# you may not use this file except in compliance with the versions.License.
# versions.You may obtain a copy of the versions.License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# versions.Unless required by applicable law or agreed to in writing, software
# distributed under the versions.License is distributed on an "AS versions.IS" versions.BASIS,
# versions.WITHOUT versions.WARRANTIES versions.OR versions.CONDITIONS versions.OF versions.ANY versions.KIND, either express or implied.
# versions.See the versions.License for the specific language governing permissions and
# limitations under the versions.License.
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
load("@bazel_tools//tools/build_defs/repo:utils.bzl", "maybe")
load(":rules_stardoc.bzl", "rules_stardoc_repository")
load(":versions.bzl", "versions")

def kt_download_local_dev_dependencies():
    """
    Downloads all necessary http_* artifacts for rules_kotlin dev configuration.

    Must be called before setup_dependencies in the WORKSPACE.
    """
    versions.use_repository(
        name = "rules_proto",
        version = versions.RULES_PROTO,
        rule = rules_stardoc_repository,
        starlark_packages = [
            "proto",
            "proto/private",
        ],
    )

    versions.use_repository(
        rule = http_archive,
        name = "rules_cc",
        version = versions.RULES_CC,
    )

    # bazel_skylib is initialized twice during development. This is intentional, as development
    # needs to be able to run the starlark unittests, while production does not.
    maybe(
        http_archive,
        name = "bazel_skylib",
        urls = ["https://github.com/bazelbuild/bazel-skylib/releases/download/%s/bazel-skylib-%s.tar.gz" % (versions.SKYLIB_VERSION, versions.SKYLIB_VERSION)],
        sha256 = versions.SKYLIB_SHA,
    )

    maybe(
        http_archive,
        name = "bazel_toolchains",
        sha256 = versions.BAZEL_TOOLCHAINS_SHA,
        strip_prefix = "bazel-toolchains-%s" % versions.BAZEL_TOOLCHAINS_VERSION,
        urls = [
            "https://mirror.bazel.build/github.com/bazelbuild/bazel-toolchains/releases/download/{0}/bazel-toolchains-{0}.tar.gz".format(versions.BAZEL_TOOLCHAINS_VERSION),
            "https://github.com/bazelbuild/bazel-toolchains/releases/download/{0}/bazel-toolchains-{0}.tar.gz".format(versions.BAZEL_TOOLCHAINS_VERSION),
        ],
    )

    versions.use_repository(
        name = "rules_python",
        rule = http_archive,
        version = versions.RULES_PYTHON,
    )

    versions.use_repository(
        name = "rules_java",
        rule = http_archive,
        version = versions.RULES_JAVA,
    )

    maybe(
        http_archive,
        name = "rules_jvm_external",
        sha256 = versions.RULES_JVM_EXTERNAL_SHA,
        strip_prefix = "rules_jvm_external-%s" % versions.RULES_JVM_EXTERNAL_TAG,
        url = "https://github.com/bazelbuild/rules_jvm_external/releases/download/%s/rules_jvm_external-%s.tar.gz" % (versions.RULES_JVM_EXTERNAL_TAG, versions.RULES_JVM_EXTERNAL_TAG),
    )

    versions.use_repository(
        name = "rules_license",
        rule = http_archive,
        version = versions.RULES_LICENSE,
    )

    versions.use_repository(
        name = "rules_pkg",
        rule = http_archive,
        version = versions.PKG,
    )

    versions.use_repository(
        name = "io_bazel_stardoc",
        version = versions.IO_BAZEL_STARDOC,
        rule = http_archive,
    )

    rules_stardoc_repository(
        name = "rules_android",
        sha256 = versions.ANDROID.SHA,
        strip_prefix = "rules_android-%s" % versions.ANDROID.VERSION,
        urls = versions.ANDROID.URLS,
        starlark_packages = [
            "android",
        ],
    )

    versions.use_repository(
        name = "rules_testing",
        rule = http_archive,
        version = versions.RULES_TESTING,
        strip_prefix = "rules_testing-%s" % versions.RULES_TESTING.version,
    )

    versions.use_repository(
        name = "rules_bazel_integration_test",
        rule = http_archive,
        version = versions.RULES_BAZEL_INTEGRATION_TEST,
    )

    versions.use_repository(
        name = "cgrindel_bazel_starlib",
        rule = http_archive,
        version = versions.CGRINDEL_BAZEL_STARLIB,
    )
