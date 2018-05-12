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


proto_library(
    name = "kotlin_model_proto",
    srcs = [":kotlin_model.proto"],
    visibility=["//visibility:public"]
)

java_proto_library(
    name = "kotlin_model_java_proto",
    deps = [":kotlin_model_proto"]
)

_PROTO_LIBS=["deps", "worker_protocol"]

[proto_library(
    name="%s_proto" % lib,
    srcs=["%s.proto" % lib]
    ) for lib in _PROTO_LIBS]

[java_proto_library(
    name="%s_java_proto" % lib,
    deps=["%s_proto" % lib],
    ) for lib in _PROTO_LIBS]

java_import(
    name = "proto",
    jars = [
        "jars/libdeps_proto-speed.jar",
        "jars/libworker_protocol_proto-speed.jar",
        "jars/libkotlin_model_proto-speed.jar"
    ],
    visibility = ["//kotlin/builder:__subpackages__"]
)

filegroup(
    name = "files",
    srcs = [
        "jars/libdeps_proto-speed.jar",
        "jars/libworker_protocol_proto-speed.jar"
    ],
    visibility = ["//kotlin/builder:__subpackages__"]
)