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
        "@io_bazel_rules_kotlin_com_google_protobuf_protobuf_java//jar",
        "@io_bazel_rules_kotlin_com_google_protobuf_protobuf_java_util//jar",
        "@io_bazel_rules_kotlin_com_google_inject_guice//jar",
        "@io_bazel_rules_kotlin_aopalliance_aopalliance//jar",
        "@io_bazel_rules_kotlin_javax_inject_javax_inject//jar",
        "@io_bazel_rules_kotlin_com_google_guava_guava//jar",
        "@io_bazel_rules_kotlin_com_google_code_gson_gson//jar",
    ],
    exports = ["//kotlin/builder/proto"],
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

filegroup(
    name = "builder_runfiles",
    srcs = [
        "@com_github_jetbrains_kotlin//:kotlin-annotation-processing",
        "@com_github_jetbrains_kotlin//:kotlin-script-runtime",
        "@com_github_jetbrains_kotlin//:compiler",
    ],
    visibility = ["//visibility:private"]
)

java_test(
    name = "integrationtests",
    test_class = "io.bazel.kotlin.builder.KotlinBuilderTestSuite",
    srcs = glob(["integrationtests/*.java"]),
    deps = [
        "//kotlin/builder:worker_lib",
        "//kotlin/builder/proto:proto",
        "//third_party/jvm/junit",
        "//third_party/jvm/com/google/protobuf:protobuf_java",
        "//third_party/jvm/com/google/guava",
        "//third_party/jvm/com/google/inject:guice",
        "//third_party/jvm/com/google/truth"
    ],
    data = [
        ":builder_runfiles"
    ]
)

java_test(
    name = "unittests",
    test_class = "io.bazel.kotlin.builder.mode.jvm.utils.JdepsParserTest",
    srcs = glob(["unittests/**/*.java"]),
    deps = [
        ":worker_lib",
        "//third_party/jvm/com/google/protobuf:protobuf_java",
    ],
    size = "small"
)