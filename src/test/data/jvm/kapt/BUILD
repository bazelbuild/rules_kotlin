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

java_plugin(
    name = "autovalue",
    generates_api = 1,
    processor_class = "com.google.auto.value.processor.AutoValueProcessor",
    deps = ["//third_party/jvm/com/google/auto/value:auto_value"],
)

java_plugin(
    name = "autoservice",
    generates_api = 0,
    processor_class = "com.google.auto.service.processor.AutoServiceProcessor",
    deps = ["//third_party/jvm/com/google/auto/service:auto_service"],
)

java_plugin(
    name = "autovalue_no_processor_class",
    generates_api = 1,
    deps = ["//third_party/jvm/com/google/auto/value:auto_value_annotations"],
)

kt_jvm_library(
    name = "ap_kotlin",
    srcs = ["kotlin/TestKtValue.kt"],
    plugins = [":autovalue"],
    deps = ["//third_party/jvm/com/google/auto/value:auto_value_annotations"],
)

kt_jvm_library(
    name = "ap_kotlin_mixed_no_plugin",
    srcs = [
        "java/TestAPNoGenReferences.java",
        "kotlin/TestKtAPNoGenReference.kt",
    ],
    deps = ["//third_party/jvm/com/google/auto/value:auto_value_annotations"],
)

kt_jvm_library(
    name = "ap_kotlin_mixed",
    srcs = [
        "java/TestAutoValue.java",
        "kotlin/TestKtValue.kt",
    ],
    plugins = [":autovalue"],
    deps = ["//third_party/jvm/com/google/auto/value:auto_value_annotations"],
)

kt_jvm_library(
    name = "ap_kotlin_resources",
    srcs = ["kotlin/TestKtService.kt"],
    plugins = [":autoservice"],
    deps = ["//third_party/jvm/com/google/auto/service:auto_service_annotations"],
)

kt_jvm_library(
    name = "ap_kotlin_resources_mixed",
    srcs = [
        "java/TestJavaService.java",
        "kotlin/TestKtService.kt",
    ],
    plugins = [":autoservice"],
    deps = ["//third_party/jvm/com/google/auto/service:auto_service_annotations"],
)

kt_jvm_library(
    name = "ap_kotlin_mixed_multiple_plugins",
    srcs = [
        "java/TestAutoValue.java",
        "java/TestJavaService.java",
        "kotlin/TestKtService.kt",
        "kotlin/TestKtValue.kt",
    ],
    plugins = [
        ":autoservice",
        ":autovalue",
    ],
    deps = [
        "//third_party/jvm/com/google/auto/service:auto_service_annotations",
        "//third_party/jvm/com/google/auto/value:auto_value_annotations",
    ],
)

kt_jvm_library(
    name = "ap_kotlin_mixed_multiple_plugins_one_without_processor_class",
    srcs = [
        "java/TestAPNoGenReferences.java",
        "java/TestJavaService.java",
        "kotlin/TestKtAPNoGenReference.kt",
        "kotlin/TestKtService.kt",
    ],
    plugins = [
        ":autoservice",
        ":autovalue_no_processor_class",
    ],
    deps = [
        "//third_party/jvm/com/google/auto/service:auto_service_annotations",
        "//third_party/jvm/com/google/auto/value:auto_value_annotations",
    ],
)

java_library(
    name = "library_exporting_autovalue_and_junit",
    exported_plugins = [
        ":autovalue",
        ":autoservice",
    ],
)

kt_jvm_library(
    name = "ap_kotlin_mixed_inherit_plugin_via_exported_deps",
    srcs = [
        "java/TestAutoValue.java",
        "java/TestJavaService.java",
        "kotlin/TestKtService.kt",
        "kotlin/TestKtValue.kt",
    ],
    plugins = [":autovalue"],
    deps = [
        "library_exporting_autovalue_and_junit",
        "//third_party/jvm/com/google/auto/service:auto_service_annotations",
        "//third_party/jvm/com/google/auto/value:auto_value_annotations",
    ],
)

filegroup(
    name = "kapt",
    srcs = [
        ":ap_kotlin.jar",
        ":ap_kotlin_mixed",
        ":ap_kotlin_mixed_inherit_plugin_via_exported_deps",
        ":ap_kotlin_mixed_multiple_plugins",
        ":ap_kotlin_mixed_multiple_plugins_one_without_processor_class",
        ":ap_kotlin_mixed_no_plugin",
        ":ap_kotlin_resources",
        ":ap_kotlin_resources_mixed",
    ],
    visibility = ["//visibility:public"],
)
