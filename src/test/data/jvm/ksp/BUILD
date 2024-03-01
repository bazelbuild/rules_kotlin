load("//kotlin:core.bzl", "kt_ksp_plugin")
load("//kotlin:jvm.bzl", "kt_jvm_library")

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

kt_ksp_plugin(
    name = "autoservice",
    processor_class = "dev.zacsweers.autoservice.ksp.AutoServiceSymbolProcessor$Provider",
    deps = [
        "@kotlin_rules_maven//:dev_zacsweers_autoservice_auto_service_ksp",
    ],
)

kt_ksp_plugin(
    name = "moshi",
    processor_class = "com.squareup.moshi.kotlin.codegen.ksp.JsonClassSymbolProcessorProvider",
    deps = [
        "@kotlin_rules_maven//:com_squareup_moshi_moshi",
        "@kotlin_rules_maven//:com_squareup_moshi_moshi_kotlin",
        "@kotlin_rules_maven//:com_squareup_moshi_moshi_kotlin_codegen",
    ],
)

kt_jvm_library(
    name = "moshi_lib",
    srcs = ["CoffeeAppModel.kt"],
    plugins = [":moshi"],
    deps = [
        "@kotlin_rules_maven//:com_squareup_moshi_moshi",
        "@kotlin_rules_maven//:com_squareup_moshi_moshi_kotlin",
        "@kotlin_rules_maven//:com_squareup_moshi_moshi_kotlin_codegen",
    ],
)

kt_jvm_library(
    name = "coffee_lib",
    srcs = ["CoffeeAppService.java"],
    plugins = [":autoservice"],
    deps = [
        "@kotlin_rules_maven//:dev_zacsweers_autoservice_auto_service_ksp",
    ],
)

filegroup(
    name = "ksp",
    srcs = [
        ":coffee_lib.jar",
        ":moshi_lib.jar",
    ],
    visibility = ["//visibility:public"],
)
