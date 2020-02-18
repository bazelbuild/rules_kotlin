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

load("//kotlin:kotlin.bzl", "kt_jvm_library")
load(":defs.bzl", "kt_rules_e2e_test", "kt_rules_test")

kt_jvm_library(
    name = "assertion_test_case",
    testonly = 1,
    srcs = ["KotlinAssertionTestCase.kt"],
    visibility = ["//visibility:public"],
    exports = [
        "@com_github_jetbrains_kotlin//:kotlin-test",
        "@kotlin_rules_maven//:com_google_guava_guava",
        "@kotlin_rules_maven//:com_google_truth_truth",
        "@kotlin_rules_maven//:junit_junit",
    ],
    deps = [
        "@com_github_jetbrains_kotlin//:kotlin-test",
        "@kotlin_rules_maven//:com_google_guava_guava",
    ],
)

kt_rules_e2e_test(
    name = "KotlinJvmBasicAssertionTest",
    srcs = ["KotlinJvmBasicAssertionTest.kt"],
    data = ["//src/test/data/jvm/basic"],
)

kt_rules_e2e_test(
    name = "KotlinNormalizationAssertionTest",
    srcs = ["KotlinNormalizationAssertionTest.kt"],
    data = ["//src/test/data/jvm/basic"],
)

kt_rules_e2e_test(
    name = "KotlinJvmKaptAssertionTest",
    srcs = ["KotlinJvmKaptAssertionTest.kt"],
    data = ["//src/test/data/jvm/kapt"],
)

kt_rules_e2e_test(
    name = "KotlinJvmDaggerExampleTest",
    srcs = ["KotlinJvmDaggerExampleTest.kt"],
    data = ["//examples/dagger:coffee_app"],
)

kt_rules_e2e_test(
    name = "KotlinJvmFriendsVisibilityTest",
    srcs = ["KotlinJvmFriendsVisibilityTest.kt"],
    friends = ["//src/test/data/jvm/basic:test_friends_library"],
)

kt_rules_e2e_test(
    name = "KotlinJvm13Test",
    srcs = ["KotlinJvm13Test.kt"],
)

test_suite(
    name = "assertion_tests",
    tests = [
        "KotlinJvm13Test",
        "KotlinJvmBasicAssertionTest",
        "KotlinJvmDaggerExampleTest",
        "KotlinJvmFriendsVisibilityTest",
        "KotlinJvmKaptAssertionTest",
    ],
)

test_suite(
    name = "local_assertion_tests",
    tests = [
        ":KotlinNormalizationAssertionTest",
        ":assertion_tests",
    ],
)
