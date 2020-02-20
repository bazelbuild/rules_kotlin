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
load("@rules_java//java:defs.bzl", "java_import")

package(default_visibility = ["//visibility:public"])

load("@rules_proto//proto:defs.bzl", "proto_library")
load("@rules_java//java:defs.bzl", "java_proto_library")

alias(
    name = "deps_java_proto",
    actual = "@deps//:java_proto",
)

alias(
    name = "worker_protocol_java_proto",
    actual = "@worker_protocol//:java_proto",
)

proto_library(
    name = "kotlin_model_proto",
    srcs = [":kotlin_model.proto"],
    visibility = ["//visibility:public"],
)

java_proto_library(
    name = "kotlin_model_java_proto",
    deps = [":kotlin_model_proto"],
)

java_library(
    name = "protobuf",
    exports = [
        ":deps_java_proto",
        ":kotlin_model_java_proto",
        ":worker_protocol_java_proto",
    ],
)
