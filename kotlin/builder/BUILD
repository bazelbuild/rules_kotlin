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
load("//kotlin/builder:bootstrap.bzl", "kotlin_worker_lib")

kotlin_worker_lib(
    srcs = glob(["src/**/*.kt"]),
    args = ["-jvm-target", "1.8"],
    deps = [
        "//kotlin/builder/proto",
        "@com_github_jetbrains_kotlin//:preloader",
        "@com_google_protobuf//:protobuf_java",
        "@com_google_protobuf//:protobuf_java_util",
        "@com_google_code_gson_gson//jar",
        "@com_google_guava_guava//jar",
    ],
    exports = [
        "//kotlin/builder/proto",
    ],
    runtime_deps = [
        "@com_github_jetbrains_kotlin//:kotlin-stdlib",
        "@com_github_jetbrains_kotlin//:kotlin-stdlib-jdk7",
        "@com_github_jetbrains_kotlin//:kotlin-stdlib-jdk8",
        "@com_github_jetbrains_kotlin//:kotlin-reflect"
    ]
)

java_binary(
    name = "builder",
    main_class = "io.bazel.kotlin.builder.KotlinBuilder",
    visibility = ["//visibility:public"],
    runtime_deps = [":worker_lib"]
)

java_test(
    name = "unittests",
    test_class = "io.bazel.kotlin.builder.mode.jvm.utils.JdepsParserTest",
    srcs = glob(["unittests/**/*.java"]),
    deps = [
        ":worker_lib",
        "@com_google_protobuf//:protobuf_java"
    ],
    size = "small"
)