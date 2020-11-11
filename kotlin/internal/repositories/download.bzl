# Copyright 2020 The Bazel Authors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive", "http_file", "http_jar")
load("@bazel_tools//tools/build_defs/repo:utils.bzl", "maybe")

RULES_NODEJS_VERSION = "0.36.1"
RULES_NODEJS_SHA = "3356c6b767403392bab018ce91625f6d15ff8f11c6d772dc84bc9cada01c669a"

BAZEL_TOOLCHAINS_VERSION = "3.1.0"
BAZEL_TOOLCHAINS_SHA = "726b5423e1c7a3866a3a6d68e7123b4a955e9fcbe912a51e0f737e6dab1d0af2"

SKYLIB_VERSION = "0.8.0"
SKYLIB_SHA = "2ea8a5ed2b448baf4a6855d3ce049c4c452a6470b1efd1504fdb7c1c134d220a"

PROTOBUF_VERSION = "3.11.3"
PROTOBUF_SHA = "cf754718b0aa945b00550ed7962ddc167167bd922b842199eeb6505e6f344852"

BAZEL_DEPS_VERSION = "0.1.0"
BAZEL_DEPS_SHA = "05498224710808be9687f5b9a906d11dd29ad592020246d4cd1a26eeaed0735e"

RULES_JVM_EXTERNAL_TAG = "2.7"
RULES_JVM_EXTERNAL_SHA = "f04b1466a00a2845106801e0c5cec96841f49ea4e7d1df88dc8e4bf31523df74"

RULES_PROTO_GIT_COMMIT = "f6b8d89b90a7956f6782a4a3609b2f0eee3ce965"
RULES_PROTO_SHA = "4d421d51f9ecfe9bf96ab23b55c6f2b809cbaf0eea24952683e397decfbd0dd0"

IO_BAZEL_STARDOC_VERSION = "0.4.0"
IO_BAZEL_STARDOC_SHA = "6d07d18c15abb0f6d393adbd6075cd661a2219faab56a9517741f0fc755f6f3c"

def kt_download_local_dev_dependencies():
    """
    Downloads all necessary http_* artifacts for rules_kotlin dev configuration.

    Must be called before setup_dependencies in the WORKSPACE.
    """
    maybe(
        http_archive,
        name = "com_google_protobuf",
        sha256 = PROTOBUF_SHA,
        strip_prefix = "protobuf-%s" % PROTOBUF_VERSION,
        urls = [
            "https://mirror.bazel.build/github.com/protocolbuffers/protobuf/archive/v%s.tar.gz" % PROTOBUF_VERSION,
            "https://github.com/protocolbuffers/protobuf/archive/v%s.tar.gz" % PROTOBUF_VERSION,
        ],
    )

    maybe(
        http_archive,
        name = "rules_proto",
        sha256 = RULES_PROTO_SHA,
        strip_prefix = "rules_proto-%s" % RULES_PROTO_GIT_COMMIT,
        urls = [
            "https://mirror.bazel.build/github.com/bazelbuild/rules_proto/archive/%s.tar.gz" % RULES_PROTO_GIT_COMMIT,
            "https://github.com/bazelbuild/rules_proto/archive/%s.tar.gz" % RULES_PROTO_GIT_COMMIT,
        ],
    )

    maybe(
        http_archive,
        name = "bazel_skylib",
        urls = ["https://github.com/bazelbuild/bazel-skylib/archive/%s.tar.gz" % SKYLIB_VERSION],
        strip_prefix = "bazel-skylib-%s" % SKYLIB_VERSION,
        sha256 = SKYLIB_SHA,
    )

    maybe(
        http_jar,
        name = "bazel_deps",
        sha256 = BAZEL_DEPS_SHA,
        url = "https://github.com/hsyed/bazel-deps/releases/download/v%s/parseproject_deploy.jar" % BAZEL_DEPS_VERSION,
    )

    maybe(
        http_archive,
        name = "bazel_toolchains",
        sha256 = BAZEL_TOOLCHAINS_SHA,
        strip_prefix = "bazel-toolchains-%s" % BAZEL_TOOLCHAINS_VERSION,
        urls = [
            "https://mirror.bazel.build/github.com/bazelbuild/bazel-toolchains/releases/download/{0}/bazel-toolchains-{0}.tar.gz".format(BAZEL_TOOLCHAINS_VERSION),
            "https://github.com/bazelbuild/bazel-toolchains/releases/download/{0}/bazel-toolchains-{0}.tar.gz".format(BAZEL_TOOLCHAINS_VERSION),
        ],
    )

    maybe(
        http_archive,
        name = "build_bazel_rules_nodejs",
        sha256 = RULES_NODEJS_SHA,
        url = "https://github.com/bazelbuild/rules_nodejs/releases/download/{0}/rules_nodejs-{0}.tar.gz".format(RULES_NODEJS_VERSION),
    )

    maybe(
        http_archive,
        name = "rules_jvm_external",
        sha256 = RULES_JVM_EXTERNAL_SHA,
        strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
        url = "https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % RULES_JVM_EXTERNAL_TAG,
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
        sha256 = IO_BAZEL_STARDOC_SHA,
        strip_prefix = "stardoc-%s" % IO_BAZEL_STARDOC_VERSION,
        url = "https://github.com/bazelbuild/stardoc/archive/%s.tar.gz" % IO_BAZEL_STARDOC_VERSION,
    )
