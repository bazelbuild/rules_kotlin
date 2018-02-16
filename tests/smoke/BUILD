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
load("//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary", "kt_jvm_test")

# a test resource library.
java_library(
    name = "resourcejar",
    resources = glob(["resourcejar/**"]),
    resource_strip_prefix = "tests/smoke/resourcejar"
)

kt_jvm_test(
    name = "junittest",
    srcs = glob(["junittest/JunitTest.kt"]),
    test_class="tests.smoke.junittest.JunitTest",
    size="small",
    data=glob(["data/*"]),
    deps = ["@junit_junit//jar"]
)

kt_jvm_library(
    name = "test_merge_resourcesjar",
    srcs = glob(["testresources/src/*.kt"]),
    resource_jars = [":resourcejar"],
)

kt_jvm_library(
    name = "test_embed_resources",
    srcs = glob(["testresources/src/*.kt"]),
    resources = glob(["testresources/resources/**/*"]),
    visibility = ["//visibility:public"]
)

kt_jvm_library(
    name = "test_embed_resources_strip_prefix",
    srcs = glob(["testresources/src/*.kt"]),
    resources = glob(["testresources/resources/**/*"]),
    resource_strip_prefix = "tests/smoke/testresources/resources"
)

kt_jvm_library(
    name = "conventional_strip_resources",
    srcs = glob(["testresources/src/*.kt"]),
    resources = glob(["conventional_strip_resources/src/main/resources/**/*", "conventional_strip_resources/src/test/resources/**/*"])
)

kt_jvm_library(
    name = "propagation_test_exporter_lib",
    srcs = ["propagation/Stub.kt"],
    exports = ["@junit_junit//jar"]
)

kt_jvm_library(
    name = "propagation_test_runtime_lib",
    srcs = ["propagation/Stub.kt"],
    runtime_deps = ["@junit_junit//jar"]
)

java_binary(
    name = "propagation_ct_consumer",
    main_class = "testing.CompileTimeDependent",
    srcs = ["propagation/CompileTimeDependent.java"],
    deps = [":propagation_test_exporter_lib"]
)

java_binary(
    name = "propagation_ct_consumer_fail_on_runtime",
    main_class = "testing.CompileTimeDependent",
    srcs = ["propagation/CompileTimeDependent.java"],
    deps = [":propagation_test_runtime_lib"],
    tags = ["manual"]
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

kt_jvm_binary(
    name="helloworld",
    srcs=glob(["helloworld/Main.kt"]),
    main_class= "helloworld.Main",
    data=glob(["data/*"]),
)

kt_jvm_library(
    name="modulename",
    srcs=glob(["helloworld/Main.kt"]),
    module_name = "hello-module"
)

genrule(
    name="helloworld_host_config_test",
    tools=["//tests/smoke:helloworld"],
    outs=["helloworld_output.txt"],
    cmd = """./$(location //tests/smoke:helloworld) > "$@" """
)

kt_jvm_library(
    name = "hellojava",
    srcs = glob(["hellojava/*.kt", "hellojava/*.java"]),
)

kt_jvm_library(
    name = "hellojava_withmerge",
    resources = glob(["resourcejar/**"]),
    srcs = glob(["hellojava/*.kt", "hellojava/*.java"]),
)

java_plugin(
    name = "autovalue",
    deps = ["@autovalue//jar"],
    processor_class = "com.google.auto.value.processor.AutoValueProcessor",
    generates_api = 1,
)

java_plugin(
    name = "autoservice",
    deps = [
        "@autoservice//jar",
        "@guava//jar",
        "@auto_common//jar"
    ],
    processor_class = "com.google.auto.service.processor.AutoServiceProcessor",
    generates_api = 0
)

java_plugin(
    name = "autovalue_no_processor_class",
    deps = ["@autovalue//jar"],
    generates_api = 1,
)

kt_jvm_library(
    name = "ap_kotlin",
    srcs = ["kapt/kotlin/TestKtValue.kt"],
    deps = ["@autovalue//jar"],
    plugins = [":autovalue"]
)

kt_jvm_library(
    name = "ap_kotlin_mixed_no_plugin",
    srcs = ["kapt/kotlin/TestKtAPNoGenReference.kt", "kapt/java/TestAPNoGenReferences.java"],
    deps = ["@autovalue//jar"]
)

kt_jvm_library(
    name = "ap_kotlin_mixed",
    srcs = ["kapt/kotlin/TestKtValue.kt", "kapt/java/TestAutoValue.java"],
    deps = ["@autovalue//jar"],
    plugins = [":autovalue"]
)

kt_jvm_library(
    name = "ap_kotlin_resources",
    srcs = ["kapt/kotlin/TestKtService.kt"],
    deps = ["@autoservice//jar"],
    plugins = [":autoservice"]
)

kt_jvm_library(
    name = "ap_kotlin_resources_mixed",
    srcs = ["kapt/kotlin/TestKtService.kt", "kapt/java/TestJavaService.java"],
    deps = ["@autoservice//jar"],
    plugins = [":autoservice"]
)

kt_jvm_library(
    name = "ap_kotlin_mixed_multiple_plugins",
    srcs = ["kapt/kotlin/TestKtService.kt", "kapt/java/TestJavaService.java", "kapt/java/TestAutoValue.java", "kapt/kotlin/TestKtValue.kt"],
    deps = ["@autoservice//jar", "@autovalue//jar"],
    plugins = [":autoservice", ":autovalue"]
)

kt_jvm_library(
    name = "ap_kotlin_mixed_multiple_plugins_one_without_processor_class",
    srcs = ["kapt/kotlin/TestKtService.kt", "kapt/java/TestJavaService.java", "kapt/java/TestAPNoGenReferences.java", "kapt/kotlin/TestKtAPNoGenReference.kt"],
    deps = ["@autoservice//jar", "@autovalue//jar"],
    plugins = [":autoservice", ":autovalue_no_processor_class"]
)

java_library(
    name = "library_exporting_autovalue_and_junit",
    exported_plugins = [":autovalue", ":autoservice"],
)

kt_jvm_library(
    name = "ap_kotlin_mixed_inherit_plugin_via_exported_deps",
    srcs = ["kapt/kotlin/TestKtService.kt", "kapt/java/TestJavaService.java", "kapt/java/TestAutoValue.java", "kapt/kotlin/TestKtValue.kt"],
    deps = ["@autoservice//jar", "@autovalue//jar", "library_exporting_autovalue_and_junit"],
    plugins = [":autovalue"]
)


