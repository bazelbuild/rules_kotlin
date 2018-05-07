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
    name = "dagger_plugin",
    deps = [
        "@dagger_compiler//jar",
        "@com_google_guava_guava//jar",
        "@dagger_producers//jar",
        "@dagger//jar",
        "@javax_inject//jar"
    ],
    processor_class = "dagger.internal.codegen.ComponentProcessor"
)

java_library(
    name = "dagger_lib",
    exports = [
        "@javax_inject//jar",
        "@dagger//jar",
    ],
    exported_plugins = ["dagger_plugin"]
)

kt_jvm_library(
    name = "coffee_lib",
    srcs = glob(["src/**"]),
    deps = [":dagger_lib"],
)

java_binary(
    name = "coffee_app",
    main_class = "coffee.CoffeeApp",
    runtime_deps = [":coffee_lib"],
    visibility = ["//visibility:public"]
)
