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
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive", "http_jar")
load("@bazel_tools//tools/build_defs/repo:utils.bzl", "maybe")
load("//kotlin/internal/repositories:rules_stardoc.bzl", "rules_stardoc_repository")
load(":versions.bzl", "versions")

def kt_download_local_dev_dependencies():
    """
    versions.Downloads all necessary http_* artifacts for rules_kotlin dev configuration.

    versions.Must be called before setup_dependencies in the versions.WORKSPACE.
    """
    maybe(
        http_archive,
        name = "com_google_protobuf",
        sha256 = versions.PROTOBUF_SHA,
        strip_prefix = "protobuf-%s" % versions.PROTOBUF_VERSION,
        urls = [
            "https://mirror.bazel.build/github.com/protocolbuffers/protobuf/archive/v%s.tar.gz" % versions.PROTOBUF_VERSION,
            "https://github.com/protocolbuffers/protobuf/archive/v%s.tar.gz" % versions.PROTOBUF_VERSION,
        ],
    )

    maybe(
        http_archive,
        name = "bazel_skylib",
        urls = ["https://github.com/bazelbuild/bazel-skylib/releases/download/%s/bazel-skylib-%s.tar.gz" % (versions.SKYLIB_VERSION, versions.SKYLIB_VERSION)],
        sha256 = versions.SKYLIB_SHA,
    )

    maybe(
        http_jar,
        name = "bazel_deps",
        sha256 = versions.BAZEL_DEPS_SHA,
        url = "https://github.com/hsyed/bazel-deps/releases/download/v%s/parseproject_deploy.jar" % versions.BAZEL_DEPS_VERSION,
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

    maybe(
        http_archive,
        name = "build_bazel_rules_nodejs",
        sha256 = versions.RULES_NODEJS_SHA,
        url = "https://github.com/bazelbuild/rules_nodejs/releases/download/{0}/rules_nodejs-{0}.tar.gz".format(versions.RULES_NODEJS_VERSION),
    )

    maybe(
        http_archive,
        name = "rules_jvm_external",
        sha256 = versions.RULES_JVM_EXTERNAL_SHA,
        strip_prefix = "rules_jvm_external-%s" % versions.RULES_JVM_EXTERNAL_TAG,
        url = "https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % versions.RULES_JVM_EXTERNAL_TAG,
    )

    maybe(
        http_archive,
        name = "rules_pkg",
        url = "https://github.com/bazelbuild/rules_pkg/releases/download/0.2.4/rules_pkg-0.2.4.tar.gz",
        sha256 = "4ba8f4ab0ff85f2484287ab06c0d871dcb31cc54d439457d28fd4ae14b18450a",
    )

    maybe(
        http_archive,
        name = "io_bazel_stardoc",
        sha256 = versions.IO_BAZEL_STARDOC_SHA,
        strip_prefix = "stardoc-%s" % versions.IO_BAZEL_STARDOC_VERSION,
        url = "https://github.com/bazelbuild/stardoc/archive/%s.tar.gz" % versions.IO_BAZEL_STARDOC_VERSION,
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

    rules_stardoc_repository(
        name = "rules_proto",
        sha256 = versions.RULES_PROTO_SHA,
        strip_prefix = "rules_proto-%s" % versions.RULES_PROTO_GIT_COMMIT,
        urls = [
            "https://mirror.bazel.build/github.com/bazelbuild/rules_proto/archive/%s.tar.gz" % versions.RULES_PROTO_GIT_COMMIT,
            "https://github.com/bazelbuild/rules_proto/archive/%s.tar.gz" % versions.RULES_PROTO_GIT_COMMIT,
        ],
        starlark_packages = [
            "proto",
            "proto/private",
        ],
    )
