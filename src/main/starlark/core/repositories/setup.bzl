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

load("@rules_jvm_external//:defs.bzl", "maven_install")
load("@rules_jvm_external//:repositories.bzl", "rules_jvm_external_deps")
load("@rules_jvm_external//:setup.bzl", "rules_jvm_external_setup")

def kt_configure():
    """Setup dependencies."""
    rules_jvm_external_deps()
    rules_jvm_external_setup()

    # Keep in sync with MODULE.bazel.
    kotlin_version = "2.3.20-RC"
    ksp_version = "2.3.6"

    maven_install(
        name = "rules_kotlin_maven",
        fetch_sources = True,
        artifacts = [
            "org.jetbrains.kotlin:kotlin-build-tools-impl:{}".format(kotlin_version),
            "org.jetbrains.kotlin:kotlin-annotation-processing-embeddable:{}".format(kotlin_version),
            "com.google.devtools.ksp:symbol-processing-aa:{}".format(ksp_version),
        ],
        repositories = [
            "https://maven-central.storage.googleapis.com/repos/central/data/",
            "https://maven.google.com",
            "https://repo1.maven.org/maven2",
        ],
        maven_install_json = "@rules_kotlin//:rules_kotlin_maven_install.json",
        resolver = "maven",
    )
