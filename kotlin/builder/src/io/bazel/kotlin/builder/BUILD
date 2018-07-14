# Copyright 2018 The Bazel Authors. All rights reserved.
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
load("//kotlin/builder:bootstrap.bzl", "kt_bootstrap_library")

kt_bootstrap_library(
    name = "builder_kt",
    srcs = glob([
        "toolchain/*.kt",
        "tasks/**/*.kt",
        "utils/**/*.kt",
    ]),
    deps = [
        "@com_github_jetbrains_kotlin//:kotlin-preloader",
        "@io_bazel_rules_kotlin//kotlin/builder/proto:deps",
        "@io_bazel_rules_kotlin//kotlin/builder/proto:kotlin_model",
        "@io_bazel_rules_kotlin//kotlin/builder/proto:worker",
        "@io_bazel_rules_kotlin_com_google_protobuf_protobuf_java//jar",
        "@io_bazel_rules_kotlin_com_google_protobuf_protobuf_java_util//jar",
        "@io_bazel_rules_kotlin_javax_inject_javax_inject//jar",
    ],
)

java_library(
    name = "builder",
    srcs = glob(["*.java"]),
    visibility = ["//kotlin/builder:__subpackages__"],
    exports = [":builder_kt"],
    deps = [
        ":builder_kt",
        "//kotlin/builder/proto:kotlin_model",
        "//third_party:dagger",
        "@com_github_jetbrains_kotlin//:annotations",
    ],
    runtime_deps = [
        "@com_github_jetbrains_kotlin//:kotlin-stdlib",
        "@com_github_jetbrains_kotlin//:kotlin-stdlib-jdk7",
        "@com_github_jetbrains_kotlin//:kotlin-stdlib-jdk8",
        "@io_bazel_rules_kotlin_com_google_code_gson_gson//jar",
    ]
)
