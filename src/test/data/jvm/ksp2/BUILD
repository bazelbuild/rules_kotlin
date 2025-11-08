load("//kotlin:core.bzl", "kt_ksp_plugin")
load("//kotlin:jvm.bzl", "kt_jvm_library")

# Copyright 2025 The Bazel Authors. All rights reserved.
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

# KSP2 Standalone Mode Tests
#
# These tests verify that KSP2 standalone mode works correctly.
# The key test is circular dependencies (Dagger scenario).

package(default_visibility = ["//visibility:private"])

# KSP Plugin Definitions (same as regular KSP tests)

kt_ksp_plugin(
    name = "dagger",
    generates_java = True,
    processor_class = "dagger.internal.codegen.KspComponentProcessor",
    target_embedded_compiler = True,
    deps = ["@kotlin_rules_maven//:com_google_dagger_dagger_compiler"],
)

kt_ksp_plugin(
    name = "autoservice",
    processor_class = "dev.zacsweers.autoservice.ksp.AutoServiceSymbolProcessor$Provider",
    deps = ["@kotlin_rules_maven//:dev_zacsweers_autoservice_auto_service_ksp"],
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

# Test Case 1: Basic Code Generation (AutoService)
#
# Verifies that KSP2 standalone can generate simple code.
# AutoService generates META-INF/services files.

kt_jvm_library(
    name = "ksp2_basic",
    srcs = ["BasicService.kt"],
    plugins = [":autoservice"],
    deps = ["@kotlin_rules_maven//:dev_zacsweers_autoservice_auto_service_ksp"],
    # ENABLE KSP2 STANDALONE MODE
    use_ksp2_standalone = True,
)

# Test Case 2: Circular Dependencies (CRITICAL!)
#
# This is the most important test. It verifies the Dagger scenario where:
# - Kotlin code (CoffeeApp.kt) references generated Java (DaggerCoffeeApp_CoffeeShop)
# - Generated Java references Kotlin types (CoffeeMaker)
#
# With standalone KSP2, this works because all sources compile together in one kotlinc invocation.

kt_jvm_library(
    name = "ksp2_circular_deps",
    srcs = [
        "CoffeeApp.kt",
        "CoffeeMaker.kt",
        "DripCoffeeModule.kt",
    ],
    plugins = [":dagger"],
    deps = [
        "@kotlin_rules_maven//:com_google_dagger_dagger",
        "@kotlin_rules_maven//:javax_inject_javax_inject",
    ],
    # ENABLE KSP2 STANDALONE MODE
    use_ksp2_standalone = True,
)

# Test Case 3: Mixed Java/Kotlin Sources
#
# Verifies that KSP2 standalone works with both Java and Kotlin sources.
# This is important because the circular dependency might involve Java sources too.

kt_jvm_library(
    name = "ksp2_mixed_sources",
    srcs = [
        "CoffeeApp.kt",
        "CoffeeBean.java",
        "CoffeeMaker.kt",
        "DripCoffeeModule.kt",
    ],
    plugins = [":dagger"],
    deps = ["@kotlin_rules_maven//:com_google_dagger_dagger"],
    # ENABLE KSP2 STANDALONE MODE
    use_ksp2_standalone = True,
)

# Test Case 4: Multiple Processors
#
# Verifies that multiple KSP processors can run in standalone mode.

kt_jvm_library(
    name = "ksp2_multiple_processors",
    srcs = [
        "CoffeeAppModel.kt",
        "MultiService.kt",
    ],
    plugins = [
        ":autoservice",
        ":moshi",
    ],
    deps = [
        "@kotlin_rules_maven//:com_squareup_moshi_moshi",
        "@kotlin_rules_maven//:com_squareup_moshi_moshi_kotlin",
        "@kotlin_rules_maven//:dev_zacsweers_autoservice_auto_service_ksp",
    ],
    # ENABLE KSP2 STANDALONE MODE
    use_ksp2_standalone = True,
)
