# Copyright 2020 The Bazel Authors. All rights reserved.
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

load("@bazel_skylib//:workspace.bzl", "bazel_skylib_workspace")
load("@cgrindel_bazel_starlib//:deps.bzl", "bazel_starlib_dependencies")
load("@io_bazel_stardoc//:setup.bzl", "stardoc_repositories")
load("@released_rules_kotlin//src/main/starlark/core/repositories:initialize.bzl", release_kotlin_repositories = "kotlin_repositories")
load("@rules_bazel_integration_test//bazel_integration_test:deps.bzl", "bazel_integration_test_rules_dependencies")
load("@rules_cc//cc:repositories.bzl", "rules_cc_dependencies", "rules_cc_toolchains")
load("@rules_java//java:repositories.bzl", "rules_java_dependencies", "rules_java_toolchains")
load("@rules_jvm_external//:defs.bzl", "maven_install")
load("@rules_pkg//:deps.bzl", "rules_pkg_dependencies")
load("@rules_proto//proto:repositories.bzl", "rules_proto_dependencies", "rules_proto_toolchains")

def kt_configure():
    """Setup dependencies. Must be called AFTER kt_download_local_dev_dependencies() """
    release_kotlin_repositories(
        is_bzlmod = True,
        compiler_repository_name = "released_com_github_jetbrains_kotlin",
        ksp_repository_name = "released_com_github_google_ksp",
    )

    native.register_toolchains("@released_rules_kotlin//kotlin/internal:default_toolchain")

    rules_cc_dependencies()
    rules_cc_toolchains()

    rules_proto_dependencies()
    rules_proto_toolchains()

    rules_java_dependencies()
    rules_java_toolchains()

    maven_install(
        name = "kotlin_rules_maven",
        fetch_sources = True,
        artifacts = [
            "com.google.code.findbugs:jsr305:3.0.2",
            "junit:junit:4.13-beta-3",
            "com.google.protobuf:protobuf-java:3.6.0",
            "com.google.protobuf:protobuf-java-util:3.6.0",
            "com.google.guava:guava:27.1-jre",
            "com.google.truth:truth:0.45",
            "com.google.auto.service:auto-service:1.1.1",
            "com.google.auto.service:auto-service-annotations:1.1.1",
            "com.google.auto.service:auto-service-annotations:jar:1.1.1",
            "com.google.auto.value:auto-value:1.10.1",
            "com.google.auto.value:auto-value-annotations:1.10.1",
            "org.apache.commons:commons-compress:1.26.2",
            "com.google.dagger:dagger:2.51",
            "com.google.dagger:dagger-compiler:2.51",
            "com.google.dagger:dagger-producers:2.51",
            "javax.annotation:javax.annotation-api:1.3.2",
            "javax.inject:javax.inject:1",
            "org.pantsbuild:jarjar:1.7.2",
            "org.jetbrains.kotlinx:atomicfu-js:0.15.2",
            "org.jetbrains.kotlinx:kotlinx-serialization-core:1.0-M1-1.4.0-rc",
            "dev.zacsweers.autoservice:auto-service-ksp:jar:1.1.0",
            "com.squareup.moshi:moshi:1.15.0",
            "com.squareup.moshi:moshi-kotlin:1.15.0",
            "com.squareup.moshi:moshi-kotlin-codegen:1.15.0",
        ],
        repositories = [
            "https://maven-central.storage.googleapis.com/repos/central/data/",
            "https://maven.google.com",
            "https://repo1.maven.org/maven2",
        ],
    )

    rules_cc_dependencies()
    rules_cc_toolchains()

    rules_pkg_dependencies()

    stardoc_repositories()

    bazel_skylib_workspace()

    bazel_integration_test_rules_dependencies()
    bazel_starlib_dependencies()
    bazel_skylib_workspace()
