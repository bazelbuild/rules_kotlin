load("@bazel_skylib//rules:build_test.bzl", "build_test")
load("@rules_java//java:defs.bzl", "java_binary", "java_plugin")

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
load("@rules_kotlin//kotlin:core.bzl", "define_kt_toolchain", "kt_compiler_plugin", "kt_ksp_plugin", "kt_plugin_cfg")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

package(default_visibility = ["//visibility:public"])

define_kt_toolchain(name = "kotlin_toolchain")

java_plugin(
    name = "autovalue",
    generates_api = True,
    processor_class = "com.google.auto.value.processor.AutoValueProcessor",
    deps = ["@maven//:com_google_auto_value_auto_value"],
)

kt_ksp_plugin(
    name = "moshi-kotlin-codegen",
    processor_class = "com.squareup.moshi.kotlin.codegen.ksp.JsonClassSymbolProcessorProvider",
    deps = [
        "@maven//:com_squareup_moshi_moshi",
        "@maven//:com_squareup_moshi_moshi_kotlin",
        "@maven//:com_squareup_moshi_moshi_kotlin_codegen",
    ],
)

kt_ksp_plugin(
    name = "autoservice",
    processor_class = "dev.zacsweers.autoservice.ksp.AutoServiceSymbolProcessor$Provider",
    deps = [
        "@maven//:com_google_auto_service_auto_service_annotations",
        "@maven//:dev_zacsweers_autoservice_auto_service_ksp",
    ],
)

kt_jvm_library(
    name = "coffee_lib",
    srcs = glob([
        "*.kt",
        "*.java",
    ]),
    plugins = [
        "//:moshi-kotlin-codegen",
        "//:autovalue",
        "//:autoservice",
    ],
    deps = [
        "@maven//:com_google_auto_service_auto_service_annotations",
        "@maven//:com_google_auto_value_auto_value_annotations",
        "@maven//:com_squareup_moshi_moshi",
        "@maven//:com_squareup_moshi_moshi_kotlin",
    ],
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
        # build_test doesn't actually fail unless you force the deploy jar to be built
        "//:coffee_app_deploy.jar",
    ],
)

kt_compiler_plugin(
    name = "ksp",
    compile_phase = True,
    id = "com.google.devtools.ksp.symbol-processing",
    options = {
        "apclasspath": "{classpath}",
        # projectBaseDir shouldn't matter because incremental is disabled
        "projectBaseDir": "{temp}",
        # Disable incremental mode
        "incremental": "false",
        # Directory where class files are written to. Files written to this directory are class
        # files being written directly from the annotation processor, not Kotlinc
        "classOutputDir": "{generatedClasses}",
        # Directory where generated Java sources files are written to
        "javaOutputDir": "{generatedSources}",
        # Directory where generated Kotlin sources files are written to
        "kotlinOutputDir": "{generatedSources}",
        # Directory where META-INF data is written to. This might not be the most ideal place to
        # write this. Maybe just directly to the classes directory?
        "resourceOutputDir": "{generatedSources}",
        # TODO(bencodes) Not sure what this directory is yet.
        "kspOutputDir": "{temp}",
        # Directory to write KSP caches. Shouldn't matter because incremental is disabled
        "cachesDir": "{temp}",
        # Include in compilation as an example. This should be processed in the stubs phase.
        "withCompilation": "true",
        # Set returnOkOnError to false because we want to fail the build if there are any errors
        "returnOkOnError": "false",
        "allWarningsAsErrors": "false",
    },
    deps = [
        "@rules_kotlin//kotlin/compiler:symbol-processing-api",
        "@rules_kotlin//kotlin/compiler:symbol-processing-cmdline",
    ],
)

kt_plugin_cfg(
    name = "ksp_moshi",
    options = {
    },
    plugin = ":ksp",
    deps = [
        "@maven//:com_squareup_moshi_moshi",
        "@maven//:com_squareup_moshi_moshi_kotlin",
        "@maven//:com_squareup_moshi_moshi_kotlin_codegen",
    ],
)

kt_jvm_library(
    name = "raw_ksp_coffee_app_lib",
    srcs = ["CoffeeAppModel.kt"],
    plugins = [
        "//:ksp",
        "//:ksp_moshi",
    ],
    deps = [
        "@maven//:com_google_auto_service_auto_service_annotations",
        "@maven//:com_google_auto_value_auto_value_annotations",
        "@maven//:com_squareup_moshi_moshi",
        "@maven//:com_squareup_moshi_moshi_kotlin",
    ],
)

build_test(
    name = "raw_ksp_lib_test",
    targets = [
        "//:raw_ksp_coffee_app_lib",
    ],
)
