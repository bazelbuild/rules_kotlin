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
package(default_visibility = ["//visibility:private"])

load("//src/test/kotlin/io/bazel/kotlin:defs.bzl", "kt_rules_test")

_COMMON_DEPS = [
    "//src/main/kotlin/io/bazel/kotlin/builder",
    "//src/main/protobuf:kotlin_model",
    "//third_party/jvm/com/google/truth",
    "//third_party/jvm/junit",
    "@io_bazel_rules_kotlin_com_google_guava_guava//jar",
]

java_library(
    name = "test_lib",
    testonly = 1,
    srcs = [
        "Deps.java",
        "KotlinBuilderJsTestTask.java",
        "KotlinBuilderJvmTestTask.java",
        "KotlinBuilderResource.java",
    ],
    data = [
        "//src/main/kotlin:compiler_lib",
        "@com_github_jetbrains_kotlin//:home",
    ],
    exports = _COMMON_DEPS + [
        "@io_bazel_rules_kotlin_com_google_protobuf_protobuf_java//jar",
        "//src/main/protobuf:deps",
    ],
    runtime_deps = [
        "@com_github_jetbrains_kotlin//:kotlin-reflect",
        "@com_github_jetbrains_kotlin//:kotlin-stdlib",
    ],
    deps = _COMMON_DEPS + [
        "//third_party:autovalue",
    ],
)

kt_rules_test(
    name = "SourceJarCreatorTest",
    srcs = ["utils/jars/SourceJarCreatorTest.java"],
)

kt_rules_test(
    name = "JdepsParserTest",
    srcs = ["tasks/jvm/JdepsParserTest.java"],
)

kt_rules_test(
    name = "KotlinBuilderJvmBasicTest",
    srcs = ["tasks/jvm/KotlinBuilderJvmBasicTest.java"],
)

kt_rules_test(
    name = "KotlinBuilderJvmKaptTest",
    srcs = ["tasks/jvm/KotlinBuilderJvmKaptTest.java"],
    data = [
        "//third_party/jvm/com/google/auto/value:auto_value",
    ],
)

kt_rules_test(
    name = "KotlinBuilderJsTest",
    srcs = ["tasks/js/KotlinBuilderJsTest.java"],
)

test_suite(
    name = "builder_tests",
    tests = [
        ":JdepsParserTest",
        ":KotlinBuilderJsTest",
        ":KotlinBuilderJvmBasicTest",
        ":KotlinBuilderJvmKaptTest",
        ":SourceJarCreatorTest",
    ],
    visibility = ["//visibility:public"],
)
