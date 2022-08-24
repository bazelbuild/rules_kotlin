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
load("@io_bazel_rules_kotlin//kotlin:core.bzl", "define_kt_toolchain", "kt_compiler_plugin")
load("@bazel_skylib//rules:build_test.bzl", "build_test")
load("@rules_java//java:defs.bzl", "java_binary", "java_library", "java_plugin")
load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

package(default_visibility = ["//visibility:public"])

# Kotlin Toolchain

define_kt_toolchain(
    name = "kotlin_toolchain",
    api_version = "1.6",
    experimental_use_abi_jars = True,
    jvm_target = "1.8",
    language_version = "1.6",
)

kt_compiler_plugin(
    name = "ksp-symbol-processing",
    id = "com.google.devtools.ksp.symbol-processing",
    options = {
        "apclasspath": "{apclasspath}",
        "projectBaseDir": "{incrementalData}",
        "incremental": "{temp}",
        "classOutputDir": "{generatedClasses}",
        "javaOutputDir": "{generatedSources}",
        "kotlinOutputDir": "{generatedSources}",
        "resourceOutputDir": "{generatedSources}",
        "kspOutputDir": "{incrementalData}",
        "cachesDir": "{incrementalData}",
        "withCompilation": "true",
        "allWarningsAsErrors": "false",
    },
    target_embedded_compiler = True,
    visibility = ["//visibility:public"],
    deps = [
        "@maven//:com_google_devtools_ksp_symbol_processing",
        "@maven//:com_google_devtools_ksp_symbol_processing_api",
    ],
)

java_plugin(
    name = "moshi-kotlin-codegen",
    generates_api = 1,
    processor_class = "com.squareup.moshi.kotlin.codegen.ksp.JsonClassSymbolProcessorProvider",
    deps = [
        "@maven//:com_squareup_moshi_moshi",
        "@maven//:com_squareup_moshi_moshi_kotlin",
        "@maven//:com_squareup_moshi_moshi_kotlin_codegen",
    ],
)

java_library(
    name = "moshi-java-plugin",
    exported_plugins = [":moshi-kotlin-codegen"],
    exports = [
        "@maven//:com_squareup_moshi_moshi",
        "@maven//:com_squareup_moshi_moshi_kotlin",
    ],
)

kt_jvm_library(
    name = "coffee_lib",
    srcs = [
        "CoffeeApp.kt",
        "CoffeeAppModel.kt",
    ],
    plugins = ["//:ksp-symbol-processing"],
    deps = ["//:moshi-java-plugin"],
)

java_binary(
    name = "coffee_app",
    main_class = "coffee.CoffeeApp",
    visibility = ["//visibility:public"],
    runtime_deps = [":coffee_lib"],
)

build_test(
    name = "force_build_app_test",
    targets = [
        "//:coffee_app",
    ],
)
