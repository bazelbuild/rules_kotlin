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
load("@com_google_protobuf//:protobuf_deps.bzl", "protobuf_deps")
load("@rules_jvm_external//:defs.bzl", "maven_install")

def kt_configure():
    """Setup dependencies."""
    protobuf_deps()

    bazel_skylib_workspace()

    maven_install(
        name = "kotlin_rules_maven",
        fetch_sources = True,
        artifacts = [
            "com.google.code.findbugs:jsr305:3.0.2",
            "com.google.code.gson:gson:2.10.1",
            "com.google.errorprone:error_prone_annotations:2.23.0",
            "junit:junit:4.13-beta-3",
            "com.google.guava:guava:33.0.0-jre",
            "com.google.truth:truth:0.45",
            "com.google.auto.service:auto-service:1.1.1",
            "com.google.auto.service:auto-service-annotations:1.1.1",
            "com.google.auto.value:auto-value:1.10.1",
            "com.google.auto.value:auto-value-annotations:1.10.1",
            "com.google.dagger:dagger:2.53.1",
            "com.google.dagger:dagger-compiler:2.53.1",
            "com.google.dagger:dagger-producers:2.53.1",
            "javax.annotation:javax.annotation-api:1.3.2",
            "javax.inject:javax.inject:1",
            "org.apache.commons:commons-compress:1.26.2",
            "org.pantsbuild:jarjar:1.7.2",
            "dev.zacsweers.autoservice:auto-service-ksp:jar:1.2.0",
            "com.squareup.moshi:moshi:1.15.1",
            "com.squareup.moshi:moshi-kotlin:1.15.1",
            "com.squareup.moshi:moshi-kotlin-codegen:1.15.1",
        ],
        repositories = [
            "https://maven-central.storage.googleapis.com/repos/central/data/",
            "https://maven.google.com",
            "https://repo1.maven.org/maven2",
        ],
    )
