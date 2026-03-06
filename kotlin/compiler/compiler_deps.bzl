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

# List of Kotlin standard library targets for runtime dependencies.
# Note: kotlin-stdlib-jdk7 and kotlin-stdlib-jdk8 are not needed as of Kotlin 1.8+,
# since JDK 8 extensions are included in the main stdlib.
KOTLIN_STDLIBS = [
    "//kotlin/compiler:annotations",
    "//kotlin/compiler:kotlin-reflect",
    "//kotlin/compiler:kotlin-stdlib",
    "//kotlin/compiler:kotlin-compiler",
    "//kotlin/compiler:kotlin-build-tools-impl",
    "//kotlin/compiler:kotlin-annotation-processing",
    "//kotlin/compiler:jvm-abi-gen",
    "//kotlin/compiler:kotlinx-coroutines-core-jvm",
    "//kotlin/compiler:kotlinx-serialization-core-jvm",
    "//kotlin/compiler:kotlinx-serialization-json-jvm",
]

# Build worker should not pull kotlin-compiler from final runtime deps to avoid classpath leakage.
KOTLIN_BUILD_RUNTIME_STDLIBS = [
    dep
    for dep in KOTLIN_STDLIBS
    if dep != "//kotlin/compiler:kotlin-compiler"
]

# KSP2 needs IntelliJ coroutines variant rather than the default kotlinx artifact.
KSP2_RUNTIME_STDLIBS = [
    dep
    for dep in KOTLIN_STDLIBS
    if dep != "//kotlin/compiler:kotlinx-coroutines-core-jvm"
] + [
    "//kotlin/compiler:ksp-intellij-kotlinx-coroutines-core-jvm",
]
