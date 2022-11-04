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
load(
    "//kotlin/internal:defs.bzl",
    _JAVA_RUNTIME_TOOLCHAIN_TYPE = "JAVA_RUNTIME_TOOLCHAIN_TYPE",
    _JAVA_TOOLCHAIN_TYPE = "JAVA_TOOLCHAIN_TYPE",
    _KtJvmInfo = "KtJvmInfo",
    _TOOLCHAIN_TYPE = "TOOLCHAIN_TYPE",
)
load(
    "//kotlin/internal/jvm:jvm.bzl",
    _kt_jvm_library = "kt_jvm_library",
    _kt_lib_common_attr = "lib_common_attr_exposed",
    _kt_lib_common_outputs_exposed = "common_outputs_exposed",
    _kt_android_local_test_runnable_common_attr_exposed = "runnable_common_attr_exposed"
)
load(
    "//kotlin/internal/jvm:android_impl.bzl",
    _kt_android_library_impl = "kt_android_library_impl",
    _kt_android_local_test_impl = "kt_android_local_test_impl",
)
load("//kotlin/internal/utils:utils.bzl", "utils")

_common_toolchains = [
    "@io_bazel_rules_android//toolchains/android:toolchain_type",
    _TOOLCHAIN_TYPE,
    _JAVA_TOOLCHAIN_TYPE,
    _JAVA_RUNTIME_TOOLCHAIN_TYPE,
]


kt_android_library = rule(
    doc = """This rule compiles and links Kotlin and Java sources into a .jar file.""",
    attrs = dict(_kt_lib_common_attr.items() + {
        "manifest": attr.label(
            doc = """The name of the Android manifest file, normally `AndroidManifest.xml`.
                Must be defined if resource_files or assets are defined.""",
            default = None,
            allow_single_file = True,
        ),
        "exports_manifest": attr.int(
            doc = """Whether to export manifest entries to `android_binary` targets
                that depend on this target. `uses-permissions` attributes are never exported.""",
            default = 1,
        ),
        "custom_package": attr.string(
            doc = """Java package for which java sources will be generated. By default the package
             is inferred from the directory where the BUILD file containing the rule is. You can
             specify a different package but this is highly discouraged since it can introduce
             classpath conflicts with other libraries that will only be detected at runtime.""",
        ),
        "resource_files": attr.label_list(
            doc = """The list of resources to be packaged. This is typically a glob of all files
                under the res directory. Generated files (from genrules) can be referenced by Label
                here as well. The only restriction is that the generated outputs must be under the
                same "res" directory as any other resource files that are included.""",
            default = [],
            allow_files = True,
        ),
        "assets": attr.label_list(
            doc = """The list of assets to be packaged. This is typically a `glob` of all files
                under the `assets` directory. You can also reference other rules (any rule that
                produces files) or exported files in the other packages, as long as all those files
                are under the `assets_dir` directory in the corresponding package.""",
            default = [],
            allow_files = True,
        ),
        "assets_dir": attr.string(
            doc = """The string giving the path to the files in assets. The pair assets and
                `assets_dir` describe packaged assets and either both attributes should be provided
                or none of them.""",
        ),
        "friends": attr.label_list(
                    doc = """A single Kotlin dep which allows the test code access to internal members. Currently uses the output
                    jar of the module -- i.e., exported deps won't be included.""",
                    default = [],
                    providers = [JavaInfo],
        ),
        "_android_resources_busybox": attr.label(
            executable = True,
            cfg = "host",
            default = Label("@bazel_tools//tools/android:busybox"),
            allow_files = True,
        ),
        "android_sdk": attr.label(
            default = Label("@bazel_tools//tools/android:sdk"),
            allow_files = True,
        ),
        "proguard_specs": attr.label_list(
            doc = """Files to be used as Proguard specification.

        These will describe the set of specifications to be used by Proguard. If specified,
        they will be added to any android_binary target depending on this library.

        The files included here must only have idempotent rules, namely -dontnote, -dontwarn,
        assumenosideeffects, and rules that start with -keep. Other options can only appear in
        android_binary's proguard_specs, to ensure non-tautological merges.""",
            default = [],
            allow_files = True,
        ),
    }.items()),
    outputs = _kt_lib_common_outputs_exposed,
    toolchains = _common_toolchains,
    fragments = ["android", "java"], # Required fragments of the target configuration
    host_fragments = ["java"], # Required fragments of the host configuration
    implementation = _kt_android_library_impl,
    provides = [JavaInfo, _KtJvmInfo],
)

kt_android_local_test = rule(
    doc = """Setup a simple kotlin_test.

    **Notes:**
    * The kotlin test library is not added implicitly, it is available with the label
    `@com_github_jetbrains_kotlin//:kotlin-test`.
    """,
    attrs = utils.add_dicts(_kt_android_local_test_runnable_common_attr_exposed, {
        "_bazel_test_runner": attr.label(
            default = Label("@bazel_tools//tools/jdk:TestRunner_deploy.jar"),
            allow_files = True,
        ),
        "friends": attr.label_list(
            doc = """A single Kotlin dep which allows the test code access to internal members. Currently uses the output
            jar of the module -- i.e., exported deps won't be included.""",
            default = [],
            providers = [JavaInfo, _KtJvmInfo],
        ),
        "test_class": attr.string(
            doc = "The Java class to be loaded by the test runner.",
            default = "",
        ),
        "main_class": attr.string(default = "com.google.testing.junit.runner.BazelTestRunner"),
        "manifest": attr.label(
            doc = """The name of the Android manifest file, normally `AndroidManifest.xml`.
                Must be defined if resource_files or assets are defined.""",
            default = None,
            allow_single_file = True,
        ),
        "manifest_values": attr.string_dict(
            doc = """A dictionary of values to be overridden in the manifest.""",
            default = {}),
        "custom_package": attr.string(
            doc = """Java package for which java sources will be generated. By default the package
             is inferred from the directory where the BUILD file containing the rule is. You can
             specify a different package but this is highly discouraged since it can introduce
             classpath conflicts with other libraries that will only be detected at runtime.""",
        ),
        "_android_resources_busybox": attr.label(
            executable = True,
            cfg = "host",
            default = Label("@bazel_tools//tools/android:busybox"),
            allow_files = True,
        ),
        "android_sdk": attr.label(
            default = Label("@bazel_tools//tools/android:sdk"),
            allow_files = True,
        ),
        # TODO(mgalindo): unify with android_sdk (BUILD-3813)
        # rules_android uses ctx._android_sdk rules_kotlin uses ctx.android_sdk
        # rules_android should just use ctx.android_sdk and then this should
        # be deleted.
        "_android_sdk": attr.label(
            default = Label("@bazel_tools//tools/android:sdk"),
            allow_files = True,
        ),
        "_enable_jdeps": attr.label(default = ":kotlin_deps"),
        "_lcov_merger": attr.label(
            default = Label("@bazel_tools//tools/test/CoverageOutputGenerator/java/com/google/devtools/coverageoutputgenerator:Main"),
        ),
    }),
    outputs = _kt_lib_common_outputs_exposed,
    executable = True,
    test = True,
    toolchains = _common_toolchains,
    fragments = ["android", "java"], # Required fragments of the target configuration
    host_fragments = ["java"], # Required fragments of the host configuration
    implementation = _kt_android_local_test_impl,
)
