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

BAZEL_TOOLCHAINS_VERSION = "be10bee3010494721f08a0fccd7f57411a1e773e"
BAZEL_TOOLCHAINS_SHA = "5962fe677a43226c409316fcb321d668fc4b7fa97cb1f9ef45e7dc2676097b26"

SKYLIB_VERSION = "0.8.0"
SKYLIB_SHA = "2ea8a5ed2b448baf4a6855d3ce049c4c452a6470b1efd1504fdb7c1c134d220a"

PROTOBUF_GIT_COMMIT = "09745575a923640154bcf307fba8aedff47f240a"  # v3.8.0, as of 2019-05-28
PROTOBUF_SHA = "76ee4ba47dec6146872b6cd051ae5bd12897ef0b1523d5aeb56d81a5a4ca885a"

BAZEL_DEPS_VERSION = "0.1.0"
BAZEL_DEPS_SHA = "05498224710808be9687f5b9a906d11dd29ad592020246d4cd1a26eeaed0735e"

RULES_JVM_EXTERNAL_TAG = "2.7"
RULES_JVM_EXTERNAL_SHA = "f04b1466a00a2845106801e0c5cec96841f49ea4e7d1df88dc8e4bf31523df74"

def kt_download_local_dev_dependencies():
    """
    Downloads all necessary http_* artifacts for rules_kotlin dev configuration.

    Must be called before setup_dependencies in the WORKSPACE.
    """
    maybe(
           http_archive,
        name = "com_google_protobuf",
        sha256 = "b404fe166de66e9a5e6dab43dc637070f950cdba2a8a4c9ed9add354ed4f6525",
        strip_prefix = "protobuf-b4f193788c9f0f05d7e0879ea96cd738630e5d51",
        url = "https://github.com/protocolbuffers/protobuf/archive/b4f193788c9f0f05d7e0879ea96cd738630e5d51.zip",
    )

    maybe(
        http_archive,
        name = "rules_proto",
        sha256 = "602e7161d9195e50246177e7c55b2f39950a9cf7366f74ed5f22fd45750cd208",
        strip_prefix = "rules_proto-97d8af4dc474595af3900dd85cb3a29ad28cc313",
        urls = [
            "https://mirror.bazel.build/github.com/bazelbuild/rules_proto/archive/97d8af4dc474595af3900dd85cb3a29ad28cc313.tar.gz",
            "https://github.com/bazelbuild/rules_proto/archive/97d8af4dc474595af3900dd85cb3a29ad28cc313.tar.gz",
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
            "https://mirror.bazel.build/github.com/bazelbuild/bazel-toolchains/archive/%s.tar.gz" % BAZEL_TOOLCHAINS_VERSION,
            "https://github.com/bazelbuild/bazel-toolchains/archive/%s.tar.gz" % BAZEL_TOOLCHAINS_VERSION,
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
