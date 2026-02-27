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

# load("@bazel_features//:deps.bzl", "bazel_features_deps")
# load("@bazel_lib//lib:repositories.bzl", "bazel_lib_dependencies")
# load("@bazel_skylib//:workspace.bzl", "bazel_skylib_workspace")
# load("@com_google_protobuf//:protobuf_deps.bzl", "protobuf_deps")
load("@rules_jvm_external//:defs.bzl", "maven_install")
load("@rules_jvm_external//:repositories.bzl", "rules_jvm_external_deps")
load("@rules_jvm_external//:setup.bzl", "rules_jvm_external_setup")
# load("@rules_proto//proto:repositories.bzl", "rules_proto_dependencies")
# load("@rules_proto//proto:setup.bzl", "rules_proto_setup")
# load("@rules_proto//proto:toolchains.bzl", "rules_proto_toolchains")

def kt_configure():
    """Setup dependencies."""
    #     rules_proto_dependencies()
    #     rules_proto_toolchains()
    #     rules_proto_setup()
    #
    #     protobuf_deps()
    #
    #     bazel_skylib_workspace()
    #     bazel_features_deps()
    #     bazel_lib_dependencies()

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
    )
