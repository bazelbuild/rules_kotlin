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
load("//kotlin:kotlin.bzl", "kt_jvm_library")

# Should not be linked against to allow the compiler workspace to be switched out when toolchains support this.
_COMPILER_DEPS = [
    "@com_github_jetbrains_kotlin//:compiler",
    "@com_github_jetbrains_kotlin//:kotlin-annotation-processing",
    "@com_github_jetbrains_kotlin//:kotlin-script-runtime",
]

# Common depset for the builder.
_COMMON_DEPS = [
    "//kotlin/builder/proto:deps",
    "//kotlin/builder/proto:kotlin_model",
    "//kotlin/builder/proto:worker",
    "@com_github_jetbrains_kotlin//:preloader",
    "@io_bazel_rules_kotlin_com_google_protobuf_protobuf_java//jar",
    "@io_bazel_rules_kotlin_com_google_protobuf_protobuf_java_util//jar",
    "@io_bazel_rules_kotlin_com_google_inject_guice//jar",
    "@io_bazel_rules_kotlin_aopalliance_aopalliance//jar",
    "@io_bazel_rules_kotlin_javax_inject_javax_inject//jar",
    "@io_bazel_rules_kotlin_com_google_guava_guava//jar",
    "@io_bazel_rules_kotlin_com_google_code_gson_gson//jar",
    "@com_github_jetbrains_kotlin//:kotlin-reflect",
    "@com_github_jetbrains_kotlin//:kotlin-stdlib",
    "@com_github_jetbrains_kotlin//:kotlin-stdlib-jdk7",
    "@com_github_jetbrains_kotlin//:kotlin-stdlib-jdk8",
]

# The compiler library, this is co-located in the kotlin compiler classloader.
kotlin_worker_lib(
    name = "compiler_lib",
    srcs = glob(["src/io/bazel/kotlin/compiler/**/*.kt"]),
    args = [
        "-jvm-target","1.8",
    ],
    neverlink_deps = _COMPILER_DEPS,
)

# The builder library.
kotlin_worker_lib(
    name = "builder_lib",
    srcs = glob(["src/io/bazel/kotlin/builder/**/*.kt"]),
    args = [
        "-jvm-target","1.8",
    ],
    deps = _COMMON_DEPS
)

# This is a hack so that the builder and compiler sources can be picked up by intellij.
kt_jvm_library(
    name = "for_ide",
    srcs = glob(["src/**/*.kt"]),
    visibility = ["//visibility:private"],
    deps = _COMMON_DEPS + _COMPILER_DEPS,
)

# The builder artifact.
java_binary(
    name = "builder",
    main_class = "io.bazel.kotlin.builder.KotlinBuilder",
    visibility = ["//visibility:public"],
    runtime_deps = [
        ":builder_lib",
    ],
    data = [
        ":compiler_lib.jar"
    ]
)

# This is defined so that tests can wire up a builder.
filegroup(
    name = "builder_runfiles_for_tests",
    srcs = [
        ":compiler_lib.jar",
        "@com_github_jetbrains_kotlin//:compiler",
        "@com_github_jetbrains_kotlin//:kotlin-annotation-processing",
        "@com_github_jetbrains_kotlin//:kotlin-script-runtime",
    ],
    testonly = 1,
    visibility = ["//visibility:private"],
)

java_library(
    name = "builder_for_tests",
    testonly = 1,
    exports = _COMMON_DEPS + [
        ":builder_lib",
        ":compiler_lib",
    ],
    data = [
        "builder_runfiles_for_tests"
    ]
)

java_test(
    name = "integrationtests",
    srcs = glob(["integrationtests/*.java"]),
    test_class = "io.bazel.kotlin.builder.KotlinBuilderTestSuite",
    deps = [
        "//kotlin/builder:builder_for_tests",
        "//third_party/jvm/com/google/truth",
        "//third_party/jvm/junit",
    ],
)

java_test(
    name = "unittests",
    size = "small",
    srcs = glob(["unittests/**/*.java"]),
    test_class = "io.bazel.kotlin.builder.mode.jvm.utils.JdepsParserTest",
    deps = [":builder_for_tests"],
)
