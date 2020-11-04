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
load("@rules_java//java:defs.bzl", "java_binary", "java_library", "java_plugin")

package(default_visibility = ["//visibility:private"])

load("//kotlin:kotlin.bzl", "kt_jvm_library")

# Generate a srcjar to validate intellij plugin correctly attaches it.
genrule(
    name = "tea_lib_src",
    outs = ["tea_lib_src.srcjar"],
    cmd = """
cat << EOF > TeaPot.kt
package tea
object TeaPot {
    fun isEmpty() = true
}
EOF
$(JAVABASE)/bin/jar -cf $@ TeaPot.kt
rm TeaPot.kt
""",
    toolchains = ["@bazel_tools//tools/jdk:current_host_java_runtime"],
)

genrule(
    name = "chai_lib_src",
    outs = ["chai_lib_src.srcjar"],
    cmd = """
cat << EOF > ChaiCup.kt
package chai
object ChaiCup {
    fun isEmpty() = true
}
EOF
$(JAVABASE)/bin/jar -cf $@ ChaiCup.kt
rm ChaiCup.kt
""",
    toolchains = ["@bazel_tools//tools/jdk:current_host_java_runtime"],
)


kt_jvm_library(
    name = "coffee_lib",
    srcs = glob(["src/**"]) + [
        # Adding a file ending with .srcjar is how code generation patterns are implemented.
        ":tea_lib_src",
        ":chai_lib_src",
    ],
    deps = [
        "//third_party:dagger",
        "@kotlin_rules_maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
    ],
)

java_binary(
    name = "coffee_app",
    main_class = "coffee.CoffeeApp",
    visibility = ["//visibility:public"],
    runtime_deps = [":coffee_lib"],
)
