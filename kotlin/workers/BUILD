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
java_library(
    name = "lib",
    srcs = glob(["src/**/*.java"]),
    deps = [
        "//kotlin/workers/proto",
        "@com_github_jetbrains_kotlin//:preloader"
    ],
    exports = ["//kotlin/workers/proto"],
    visibility = ["//visibility:private"]
)

java_binary(
    name = "compiler_jvm",
    main_class = "io.bazel.ruleskotlin.workers.compilers.jvm.KotlinJvmBuilder",
    visibility = ["//visibility:public"],
    runtime_deps = ["lib"]
)

java_test(
    name = "unittests",
    test_class = "io.bazel.ruleskotlin.workers.compilers.jvm.utils.JdepsParserTest",
    srcs = glob(["unittests/**/*.java"]),
    deps = ["lib"],
    size = "small"
)