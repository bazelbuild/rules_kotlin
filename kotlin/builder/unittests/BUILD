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
java_test(
    name = "unittests",
    size = "small",
    srcs = glob(["**/*.java"]),
    test_class = "io.bazel.kotlin.builder.mode.jvm.utils.JdepsParserTest",
    deps = [
        "//kotlin/builder/src/io/bazel/kotlin/builder",
        "@com_github_jetbrains_kotlin//:kotlin-stdlib",
        "@io_bazel_rules_kotlin//kotlin/builder/proto:deps",
        "@io_bazel_rules_kotlin_com_google_protobuf_protobuf_java//jar",
    ]
)
