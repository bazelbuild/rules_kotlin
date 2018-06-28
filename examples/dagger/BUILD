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
package(default_visibility = ["//visibility:private"])

load("//kotlin:kotlin.bzl", "kt_jvm_library")

java_plugin(
    name = "dagger_plugin",
    deps = [
        "//third_party/jvm/com/google/dagger:dagger_compiler",
        "@//third_party/jvm/com/google/guava",
        "//third_party/jvm/com/google/dagger:dagger_producers",
        "//third_party/jvm/com/google/dagger",
        "//third_party/jvm/javax/inject:javax_inject"
    ],
    processor_class = "dagger.internal.codegen.ComponentProcessor"
)

java_library(
    name = "dagger_lib",
    exports = [
        "//third_party/jvm/javax/inject:javax_inject",
        "//third_party/jvm/com/google/dagger"
    ],
    exported_plugins = ["dagger_plugin"]
)

# Generate a srcjar to validate intellij plugin correctly attaches it.
genrule(
    name = "tea_lib_src",
    tools = ["@local_jdk//:jdk"],
    cmd = """
cat << EOF > TeaPot.kt
package tea

object TeaPot {
    fun isEmpty() = true
}
EOF
external/local_jdk/bin/jar -cf $@ TeaPot.kt
""",
    outs = ["donkey.srcjar"]
)

kt_jvm_library(
    name = "tea_lib",
    srcs = [":tea_lib_src"]
)

kt_jvm_library(
    name = "coffee_lib",
    srcs = glob(["src/**"]),
    deps = [
        ":tea_lib",
        ":dagger_lib",
        "//third_party:kotlinx_coroutines"
    ],
)

java_binary(
    name = "coffee_app",
    main_class = "coffee.CoffeeApp",
    runtime_deps = [":coffee_lib"],
    visibility = ["//visibility:public"]
)
