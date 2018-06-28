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
load("//kotlin:kotlin.bzl", "kt_jvm_test", "kt_jvm_library", "kt_jvm_binary")

kt_jvm_binary(
    name="test_module_name_bin",
    srcs=glob(["helloworld/Main.kt"]),
    main_class= "helloworld.Main",
)

kt_jvm_library(
    name = "test_module_name_lib",
    srcs = ["helloworld/Main.kt"]
)

kt_jvm_library(
    name="test_module_name_attr_lib",
    srcs=glob(["helloworld/Main.kt"]),
    module_name = "hello-module"
)

# a test resource library.
java_library(
    name = "resourcejar",
    resources = glob(["resourcejar/**"]),
    resource_strip_prefix = "tests/integrationtests/jvm/basic/resourcejar"
)

kt_jvm_library(
    name = "test_embed_resources",
    srcs = glob(["testresources/src/*.kt"]),
    resources = glob(["testresources/resources/**/*"]),
    visibility = ["//visibility:public"]
)

kt_jvm_library(
    name = "test_merge_resourcesjar",
    srcs = glob(["testresources/src/*.kt"]),
    resource_jars = [":resourcejar"],
    visibility = ["//visibility:public"]
)


kt_jvm_library(
    name = "test_embed_resources_strip_prefix",
    srcs = glob(["testresources/src/*.kt"]),
    resources = glob(["testresources/resources/**/*"]),
    resource_strip_prefix = "tests/integrationtests/jvm/basic/testresources/resources"
)

kt_jvm_library(
    name = "conventional_strip_resources",
    srcs = glob(["testresources/src/*.kt"]),
    resources = glob(["conventional_strip_resources/src/main/resources/**/*", "conventional_strip_resources/src/test/resources/**/*"])
)

kt_jvm_library(
    name = "propagation_test_exporter_lib",
    srcs = ["propagation/Stub.kt"],
    exports = ["//third_party/jvm/junit"]
)

kt_jvm_library(
    name = "propagation_test_runtime_lib",
    srcs = ["propagation/Stub.kt"],
    runtime_deps = ["//third_party/jvm/junit"]
)

java_binary(
    name = "propagation_ct_consumer",
    main_class = "testing.CompileTimeDependent",
    srcs = ["propagation/CompileTimeDependent.java"],
    deps = [":propagation_test_exporter_lib"]
)

java_binary(
    name = "propagation_rt_via_export_consumer",
    main_class = "testing.RuntimeDependent",
    srcs = ["propagation/RuntimeDependent.java"],
    deps = [":propagation_test_exporter_lib"]
)

java_binary(
    name = "propagation_rt_via_runtime_deps_consumer",
    main_class = "testing.RuntimeDependent",
    srcs = ["propagation/RuntimeDependent.java"],
    deps = [":propagation_test_runtime_lib"]
)

kt_jvm_library(
    name = "test_friends_library",
    srcs = ["test_friends/Service.kt"]
)

# This test should be explicetly executed as module name mangling handling could regress otherwise.
kt_jvm_test(
    name = "friends_tests",
    srcs = ["test_friends/TestFriendsTest.kt"],
    test_class = "test.TestFriendsTest",
    deps = [
        "//third_party/jvm/junit:junit"
    ],
    friends = [":test_friends_library"]
)

filegroup(
    name="cases",
    srcs = [
        ":test_module_name_bin",
        ":test_module_name_lib",
        ":test_module_name_attr_lib",
        ":test_embed_resources.jar",
        ":test_merge_resourcesjar.jar",
        ":test_embed_resources_strip_prefix.jar",
        ":test_module_name_bin.jar",
        ":conventional_strip_resources.jar",
        ":propagation_ct_consumer",
        ":propagation_rt_via_export_consumer",
        ":propagation_rt_via_runtime_deps_consumer"
    ],
    visibility=["//tests/integrationtests:__subpackages__"]
)

