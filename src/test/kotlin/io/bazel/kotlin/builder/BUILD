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

load("@rules_java//java:defs.bzl", "java_library")
load("//src/test/kotlin/io/bazel/kotlin:defs.bzl", "kt_rules_test")

_COMMON_DEPS = [
    "//src/main/kotlin/io/bazel/kotlin/builder",
    "//src/main/protobuf:kotlin_model_java_proto",
    "//src/main/protobuf:deps_java_proto",
    "@kotlin_rules_maven//:com_google_truth_truth",
    "@kotlin_rules_maven//:junit_junit",
    "@kotlin_rules_maven//:com_google_guava_guava",
    "@kotlin_rules_maven//:com_google_code_findbugs_jsr305",
]

java_library(
    name = "test_lib",
    testonly = 1,
    srcs = [
        "Deps.java",
        "DirectoryType.java",
        "KotlinAbstractTestBuilder.java",
        "KotlinJsTestBuilder.java",
        "KotlinJvmTestBuilder.java",
    ],
    data = [
        "//src/main/kotlin:compiler_lib",
        "@com_github_jetbrains_kotlin//:home",
    ],
    exports = _COMMON_DEPS + [
        "@kotlin_rules_maven//:com_google_protobuf_protobuf_java",
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
    name = "KotlinJvmTaskExecutorTest",
    srcs = ["tasks/jvm/KotlinJvmTaskExecutorTest.kt"],
    deps = ["@com_github_jetbrains_kotlin//:kotlin-test"],
)

# TODO(bazelbuild/rules_kotlin/issues/275): Remove full jar reference when the kt_rules_test handles jvm_import data better.
_MAVEN_CENTRAL_PREFIX = "@kotlin_rules_maven//:v1/https/maven-central.storage.googleapis.com/repos/central/data"
_AUTO_VALUE_PREFIX = "%s/com/google/auto/value" % _MAVEN_CENTRAL_PREFIX
_AUTO_VALUE_JAR = _AUTO_VALUE_PREFIX+ "/auto-value/1.6.5/auto-value-1.6.5.jar"
_AUTO_VALUE_ANNOTATIONS_JAR =  _AUTO_VALUE_PREFIX+  "/auto-value-annotations/1.6.5/auto-value-annotations-1.6.5.jar"

kt_rules_test(
    name = "KotlinBuilderJvmKaptTest",
    srcs = ["tasks/jvm/KotlinBuilderJvmKaptTest.java"],
    data = [
       _AUTO_VALUE_JAR,
       _AUTO_VALUE_ANNOTATIONS_JAR,
    ],
    jvm_flags = [
        "-Dauto_value=$(location %s)" %  _AUTO_VALUE_JAR,
        "-Dauto_value_annotations=$(location %s)" % _AUTO_VALUE_ANNOTATIONS_JAR,
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
        ":KotlinJvmTaskExecutorTest",
        ":SourceJarCreatorTest",
    ],
    visibility = ["//visibility:public"],
)
