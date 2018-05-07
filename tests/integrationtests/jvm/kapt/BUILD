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
package(default_visibility=["//visibility:private"])
load("//kotlin:kotlin.bzl", "kt_jvm_library")

java_plugin(
    name = "autovalue",
    deps = ["@autovalue//jar"],
    processor_class = "com.google.auto.value.processor.AutoValueProcessor",
    generates_api = 1,
)

java_plugin(
    name = "autoservice",
    deps = [
        "@autoservice//jar",
        "@com_google_guava_guava//jar",
        "@auto_common//jar"
    ],
    processor_class = "com.google.auto.service.processor.AutoServiceProcessor",
    generates_api = 0
)

java_plugin(
    name = "autovalue_no_processor_class",
    deps = ["@autovalue//jar"],
    generates_api = 1,
)

kt_jvm_library(
    name = "ap_kotlin",
    srcs = ["kotlin/TestKtValue.kt"],
    deps = ["@autovalue//jar"],
    plugins = [":autovalue"]
)

kt_jvm_library(
    name = "ap_kotlin_mixed_no_plugin",
    srcs = ["kotlin/TestKtAPNoGenReference.kt", "java/TestAPNoGenReferences.java"],
    deps = ["@autovalue//jar"]
)

kt_jvm_library(
    name = "ap_kotlin_mixed",
    srcs = ["kotlin/TestKtValue.kt", "java/TestAutoValue.java"],
    deps = ["@autovalue//jar"],
    plugins = [":autovalue"]
)

kt_jvm_library(
    name = "ap_kotlin_resources",
    srcs = ["kotlin/TestKtService.kt"],
    deps = ["@autoservice//jar"],
    plugins = [":autoservice"]
)

kt_jvm_library(
    name = "ap_kotlin_resources_mixed",
    srcs = ["kotlin/TestKtService.kt", "java/TestJavaService.java"],
    deps = ["@autoservice//jar"],
    plugins = [":autoservice"]
)

kt_jvm_library(
    name = "ap_kotlin_mixed_multiple_plugins",
    srcs = ["kotlin/TestKtService.kt", "java/TestJavaService.java", "java/TestAutoValue.java", "kotlin/TestKtValue.kt"],
    deps = ["@autoservice//jar", "@autovalue//jar"],
    plugins = [":autoservice", ":autovalue"]
)

kt_jvm_library(
    name = "ap_kotlin_mixed_multiple_plugins_one_without_processor_class",
    srcs = ["kotlin/TestKtService.kt", "java/TestJavaService.java", "java/TestAPNoGenReferences.java", "kotlin/TestKtAPNoGenReference.kt"],
    deps = ["@autoservice//jar", "@autovalue//jar"],
    plugins = [":autoservice", ":autovalue_no_processor_class"]
)

java_library(
    name = "library_exporting_autovalue_and_junit",
    exported_plugins = [":autovalue", ":autoservice"],
)

kt_jvm_library(
    name = "ap_kotlin_mixed_inherit_plugin_via_exported_deps",
    srcs = ["kotlin/TestKtService.kt", "java/TestJavaService.java", "java/TestAutoValue.java", "kotlin/TestKtValue.kt"],
    deps = ["@autoservice//jar", "@autovalue//jar", "library_exporting_autovalue_and_junit"],
    plugins = [":autovalue"]
)

filegroup(
    name = "cases",
    srcs = [
        ":ap_kotlin.jar",
        ":ap_kotlin_mixed_no_plugin",
        ":ap_kotlin_mixed",
        ":ap_kotlin_resources",
        ":ap_kotlin_resources_mixed",
        ":ap_kotlin_mixed_multiple_plugins",
        ":ap_kotlin_mixed_multiple_plugins_one_without_processor_class",
        ":ap_kotlin_mixed_inherit_plugin_via_exported_deps",
    ],
    visibility=["//tests/integrationtests:__subpackages__"]
)