# Copyright 2022 The Bazel Authors. All rights reserved.
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

def kt_configure_ksp():
    """
    Defines aliases for KSP artifacts from Maven.

    Must be called in kotlin/compiler/BUILD.bazel
    """
    if native.package_name() != "kotlin/compiler":
        fail("kt_configure_ksp must be called in kotlin/compiler not %s" % native.package_name())

    # KSP artifacts are now fetched via rules_jvm_external in MODULE.bazel
    # symbol-processing-aa-embeddable is the shaded version for use with kotlin-compiler-embeddable
    native.alias(
        name = "symbol-processing-aa",
        actual = "@kotlin_rules_maven//:com_google_devtools_ksp_symbol_processing_aa_embeddable",
        visibility = ["//visibility:public"],
    )

    native.alias(
        name = "symbol-processing-common-deps",
        actual = "@kotlin_rules_maven//:com_google_devtools_ksp_symbol_processing_common_deps",
        visibility = ["//visibility:public"],
    )

    native.alias(
        name = "symbol-processing-api",
        actual = "@kotlin_rules_maven//:com_google_devtools_ksp_symbol_processing_api",
        visibility = ["//visibility:public"],
    )
