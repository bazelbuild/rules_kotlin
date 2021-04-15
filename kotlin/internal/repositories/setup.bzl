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

load("@rules_proto//proto:repositories.bzl", "rules_proto_dependencies", "rules_proto_toolchains")
load("@rules_jvm_external//:defs.bzl", "maven_install")
load("//kotlin/internal/repositories:http_java_proto_file.bzl", "http_java_proto_file")
load("@io_bazel_stardoc//:setup.bzl", "stardoc_repositories")
load("@bazel_skylib//:workspace.bzl", "bazel_skylib_workspace")

def kt_configure():
    """Setup dependencies. Must be called AFTER kt_download_local_dev_dependencies() """
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
            "com.google.auto.service:auto-service:1.0-rc5",
            "com.google.auto.service:auto-service-annotations:1.0-rc5",
            "com.google.auto.value:auto-value:1.6.5",
            "com.google.auto.value:auto-value-annotations:1.6.5",
            "com.google.dagger:dagger:2.26",
            "com.google.dagger:dagger-compiler:2.26",
            "com.google.dagger:dagger-producers:2.26",
            "javax.annotation:javax.annotation-api:1.3.2",
            "javax.inject:javax.inject:1",
            "org.pantsbuild:jarjar:1.7.2",
            "org.jetbrains.kotlinx:atomicfu-js:0.15.2",
            "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2",
            "org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.4.2",
            "org.jetbrains.kotlinx:kotlinx-coroutines-test:1.4.2",
            "org.jetbrains.kotlinx:kotlinx-coroutines-debug:1.4.2",
            "org.jetbrains.kotlinx:kotlinx-serialization-runtime:1.0-M1-1.4.0-rc",
        ],
        repositories = [
            "https://maven-central.storage.googleapis.com/repos/central/data/",
            "https://repo1.maven.org/maven2",
        ],
    )

    rules_proto_dependencies()
    rules_proto_toolchains()

    http_java_proto_file(
        name = "deps",
        sha256 = "b861dbce04177df9e4b7204876b2f27e18f40eb6d20b3dffefecdd2baf3cfe92",
        urls = ["https://raw.githubusercontent.com/bazelbuild/bazel/3.4.1/src/main/protobuf/deps.proto"],
    )

    http_java_proto_file(
        name = "worker_protocol",
        sha256 = "1157c93666f98cfcfcc9f7b073b8dac5bbd50e18f5ab981e93c71e03ed08f304",
        urls = ["https://raw.githubusercontent.com/bazelbuild/bazel/3.4.1/src/main/protobuf/worker_protocol.proto"],
    )

    stardoc_repositories()

    bazel_skylib_workspace()
