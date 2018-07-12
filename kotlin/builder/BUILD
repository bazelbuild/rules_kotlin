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

# The compiler library, this is co-located in the kotlin compiler classloader.
kt_bootstrap_library(
    name = "compiler_lib",
    srcs = glob(["src/io/bazel/kotlin/compiler/**/*.kt"]),
    neverlink_deps = [
        "@com_github_jetbrains_kotlin//:kotlin-compiler",
        "@com_github_jetbrains_kotlin//:kotlin-annotation-processing",
        "@com_github_jetbrains_kotlin//:kotlin-script-runtime",
    ],
)

# The builder artifact.
java_binary(
    name = "builder",
    data = [":compiler_lib.jar"],
    main_class = "io.bazel.kotlin.builder.KotlinBuilder",
    visibility = ["//visibility:public"],
    runtime_deps = ["//kotlin/builder/src/io/bazel/kotlin/builder"],
)

filegroup(
    name = "data_for_tests",
    srcs = [
        ":compiler_lib",
        "@com_github_jetbrains_kotlin//:kotlin-annotation-processing",
        "@com_github_jetbrains_kotlin//:kotlin-compiler",
        "@com_github_jetbrains_kotlin//:kotlin-script-runtime",
    ],
    testonly = 1,
    visibility = ["//visibility:private"],
)

java_library(
    name = "builder_lib_for_tests",
    runtime_deps = [
        "@com_github_jetbrains_kotlin//:kotlin-reflect",
        "@io_bazel_rules_kotlin_aopalliance_aopalliance//jar",
    ],
    testonly = 1,
    data = [":data_for_tests"],
    exports = [
        "//kotlin/builder/src/io/bazel/kotlin/builder",
        "//third_party/jvm/com/google/truth",
        "//third_party/jvm/junit",
        "@io_bazel_rules_kotlin//kotlin/builder/proto:deps",
        "@io_bazel_rules_kotlin//kotlin/builder/proto:kotlin_model",
        "@io_bazel_rules_kotlin_com_google_guava_guava//jar",
        "@io_bazel_rules_kotlin_com_google_inject_guice//jar",
        "@io_bazel_rules_kotlin_com_google_protobuf_protobuf_java//jar",
    ],
    visibility = [
        "//kotlin/builder/integrationtests:__subpackages__",
        "//kotlin/builder/unittests:__subpackages__"
    ]
)
