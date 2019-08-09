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
load("//src/main/kotlin:bootstrap.bzl", "kt_bootstrap_library")
load("//kotlin:kotlin.bzl", "kt_jvm_library")

# The compiler library, this is co-located in the kotlin compiler classloader.
kt_bootstrap_library(
    name = "compiler_lib",
    srcs = glob(["io/bazel/kotlin/compiler/*.kt"]),
    neverlink_deps = [
        "@com_github_jetbrains_kotlin//:kotlin-compiler",
        "@com_github_jetbrains_kotlin//:kotlin-annotation-processing",
        "@com_github_jetbrains_kotlin//:kotlin-script-runtime",
    ],
    visibility = ["//src/test/kotlin/io/bazel/kotlin/builder:__subpackages__"],
)

# The builder artifact.
java_binary(
    name = "builder",
    data = [
        ":compiler_lib.jar",
        "@com_github_jetbrains_kotlin//:lib/kotlin-compiler.jar",
    ],
    main_class = "io.bazel.kotlin.builder.KotlinBuilderMain",
    visibility = ["//visibility:public"],
    runtime_deps = ["//src/main/kotlin/io/bazel/kotlin/builder"],
)
