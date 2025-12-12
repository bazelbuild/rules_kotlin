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
load("@rules_java//java:defs.bzl", "java_library")

java_library(
    name = "builder",
    srcs = glob(["*.java"]),
    visibility = ["//src:__subpackages__"],
    deps = [
        "//kotlin/compiler:annotations",
        "//kotlin/compiler:kotlin-stdlib",
        "//kotlin/compiler:kotlin-stdlib-jdk7",
        "//kotlin/compiler:kotlin-stdlib-jdk8",
        "//src/main/kotlin/io/bazel/kotlin/builder/tasks",
        "//src/main/kotlin/io/bazel/kotlin/builder/toolchain",
        "//src/main/kotlin/io/bazel/kotlin/builder/utils",
        "//src/main/kotlin/io/bazel/worker",
        "//src/main/protobuf:deps_java_proto",
        "//src/main/protobuf:kotlin_model_java_proto",
        "//third_party:dagger",
        "@kotlin_rules_maven//:javax_annotation_javax_annotation_api",
        "@kotlin_rules_maven//:javax_inject_javax_inject",
    ],
)
