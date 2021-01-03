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
load("//kotlin:kotlin.bzl", "kt_jvm_library")
load("//third_party:jarjar.bzl", "jar_jar")
load("//kotlin/internal/utils:packager.bzl", "release_archive")

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

jar_jar(
    name = "skip-code-gen",
    input_jar = "//src/main/kotlin/io/bazel/kotlin/plugin:skip-code-gen_deploy.jar",
    rules = "shade.jarjar",
    visibility = ["//visibility:public"],
)

jar_jar(
    name = "jdeps-gen",
    input_jar = "//src/main/kotlin/io/bazel/kotlin/plugin/jdeps:jdeps-gen_deploy.jar",
    rules = "shade.jarjar",
    visibility = ["//visibility:public"],
)

java_binary(
    name = "builder",
    data = [
        "//src/main/kotlin:skip-code-gen",
        "//src/main/kotlin:jdeps-gen",
        "//src/main/kotlin/io/bazel/kotlin/compiler",
        "@com_github_jetbrains_kotlin//:lib/kotlin-compiler.jar",
    ],
    jvm_flags = [
        "-XX:+IgnoreUnrecognizedVMOptions",
        "--add-opens=java.base/java.nio=ALL-UNNAMED",
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=jdk.jdeps/com.sun.tools.jdeps=ALL-UNNAMED",
    ],
    main_class = "io.bazel.kotlin.builder.KotlinBuilderMain",
    visibility = ["//visibility:public"],
    runtime_deps = [":builder_jar_jar"],
)

java_binary(
    name = "jdeps_merger",
    jvm_flags = [
        "-XX:+IgnoreUnrecognizedVMOptions",
        "--add-opens=java.base/java.nio=ALL-UNNAMED",
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=jdk.jdeps/com.sun.tools.jdeps=ALL-UNNAMED",
    ],
    main_class = "io.bazel.kotlin.builder.JdepsMergerMain",
    visibility = ["//visibility:public"],
    runtime_deps = [":builder_jar_jar"],
)

release_archive(
    name = "pkg",
    srcs = [
        ":skip-code-gen.jar",
        ":jdeps-gen.jar",
    ],
    package_dir = "src/main/kotlin",  # explicitly set the package directory, as there are no parent release_archives.
    src_map = {
        ":builder_deploy.jar": "kotlin_worker.jar",
        ":jdeps_merger_deploy.jar": "jdeps_merger_worker.jar",
        "BUILD.release.bazel": "BUILD.bazel",
    },
    deps = [
        "//src/main/kotlin/io/bazel/kotlin/compiler:pkg",
    ],
)
