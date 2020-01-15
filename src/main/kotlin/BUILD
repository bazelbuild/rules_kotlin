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
load("@rules_java//java:defs.bzl", "java_binary")
load("//src/main/kotlin:bootstrap.bzl", "kt_bootstrap_library")
load("//kotlin:kotlin.bzl", "kt_jvm_library")
load("//third_party:jarjar.bzl", "jar_jar")

# The compiler library, this is co-located in the kotlin compiler classloader.
kt_bootstrap_library(
    name = "compiler_lib",
    srcs = glob(["io/bazel/kotlin/compiler/*.kt"]),
    neverlink_deps = [
        "@com_github_jetbrains_kotlin//:kotlin-compiler",
        "@com_github_jetbrains_kotlin//:kotlin-annotation-processing",
        "@com_github_jetbrains_kotlin//:kotlin-script-runtime",
    ],
    visibility = ["//src/test/kotlin/io/bazel/kotlin/builder:__subpackages__"],
)

java_binary(
    name = "builder_raw",
    create_executable = False,
    runtime_deps = ["//src/main/kotlin/io/bazel/kotlin/builder"],
)

# The builder artifact. Shaded to ensure that libraries it uses are not leaked to
# the code it's running against (e.g. dagger)
jar_jar(
    name = "builder_jar_jar",
    input_jar = ":builder_raw_deploy.jar",
    rules = "shade.jarjar",
)

java_binary(
    name = "builder",
    data = [
        ":compiler_lib.jar",
        "@com_github_jetbrains_kotlin//:lib/kotlin-compiler.jar",
    ],
    main_class = "io.bazel.kotlin.builder.KotlinBuilderMain",
    runtime_deps = [":builder_jar_jar"],
    jvm_flags = [
        "-XX:+IgnoreUnrecognizedVMOptions",
        "--add-opens=java.base/java.nio=ALL-UNNAMED",
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=jdk.jdeps/com.sun.tools.jdeps=ALL-UNNAMED",
    ],
    visibility = ["//visibility:public"],
)