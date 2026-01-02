# Copyright 2024 The Bazel Authors. All rights reserved.
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

"""Symbolic macros for kt_jvm_test and kt_jvm_binary.

These macros implement kt_jvm_test and kt_jvm_binary by composing kt_jvm_library
with native java_test/java_binary rules. This provides better Windows compatibility
by leveraging Bazel's native Java rules.

Usage:
    load("@rules_kotlin//kotlin:jvm_compat.bzl", "kt_jvm_test", "kt_jvm_binary")
"""

load("@rules_java//java:defs.bzl", "java_binary", "java_test")
load("//kotlin/internal/jvm:jvm.bzl", "kt_jvm_library")

# Split strings for inferring test class from source file paths.
# Ported from impl.bzl
_SPLIT_STRINGS = [
    "src/test/java/",
    "src/test/kotlin/",
    "javatests/",
    "kotlin/",
    "java/",
    "test/",
]

def _infer_test_class(name, srcs):
    """Infer test class from source files.

    This is a best-effort attempt to infer the test class from source file paths.
    It looks for a source file matching the target name and attempts to derive
    the fully qualified class name from the path.

    Args:
        name: The target name
        srcs: List of source file labels

    Returns:
        Inferred test class string, or empty string if inference fails
    """
    for src in srcs:
        src_path = str(src)

        # Get filename without extension
        filename = src_path.split("/")[-1].rsplit(".", 1)[0]

        # Only consider sources that match the target name
        if filename == name:
            for splitter in _SPLIT_STRINGS:
                if splitter in src_path:
                    parts = src_path.split(splitter, 1)
                    if len(parts) == 2:
                        # Convert path to class name: "com/example/Test.kt" -> "com.example.Test"
                        return parts[1].rsplit(".", 1)[0].replace("/", ".")
    return ""

def _kt_jvm_test_compat_impl(
        name,
        visibility = None,
        srcs = [],
        deps = [],
        runtime_deps = [],
        data = [],
        resources = [],
        test_class = "",
        main_class = "",
        jvm_flags = [],
        env = {},
        env_inherit = [],
        plugins = [],
        kotlinc_opts = None,
        javac_opts = None,
        associates = [],
        module_name = "",
        resource_jars = [],
        resource_strip_prefix = None,
        tags = [],
        **kwargs):
    """Implementation of kt_jvm_test using kt_jvm_library + java_test.

    This creates two targets:
    - <name>_lib: A kt_jvm_library that compiles the Kotlin sources
    - <name>: A java_test that runs the tests using the compiled library
    """
    lib_name = name + "_lib"

    # Create the kt_jvm_library for compilation
    kt_jvm_library(
        name = lib_name,
        srcs = srcs,
        deps = deps,
        resources = resources,
        plugins = plugins,
        kotlinc_opts = kotlinc_opts,
        javac_opts = javac_opts,
        associates = associates,
        module_name = module_name,
        resource_jars = resource_jars,
        resource_strip_prefix = resource_strip_prefix,
        visibility = ["//visibility:private"],
        tags = tags + ["manual"],  # Don't build standalone
        **kwargs
    )

    # Infer test_class if not provided
    effective_test_class = test_class
    if not effective_test_class:
        effective_test_class = _infer_test_class(name, srcs)

    # Effective main_class defaults to BazelTestRunner
    effective_main_class = main_class if main_class else "com.google.testing.junit.runner.BazelTestRunner"

    # Create the java_test
    java_test(
        name = name,
        visibility = visibility,
        runtime_deps = [":" + lib_name] + (runtime_deps if runtime_deps else []),
        data = data,
        test_class = effective_test_class,
        main_class = effective_main_class,
        jvm_flags = ["-ea"] + (jvm_flags if jvm_flags else []),
        env = env,
        env_inherit = env_inherit,
        tags = tags,
    )

# Define the symbolic macro for kt_jvm_test
kt_jvm_test_compat = macro(
    doc = """Kotlin JVM test implemented as kt_jvm_library + java_test.

This macro creates:
- A kt_jvm_library target (name = "<name>_lib") for Kotlin compilation
- A java_test target (name = "<name>") for test execution

This implementation provides better Windows compatibility by leveraging
Bazel's native java_test rule.
""",
    implementation = _kt_jvm_test_compat_impl,
    attrs = {
        "srcs": attr.label_list(
            doc = "Kotlin and Java source files",
            allow_files = [".kt", ".java", ".srcjar"],
            default = [],
        ),
        "deps": attr.label_list(
            doc = "Compile-time dependencies",
            default = [],
        ),
        "runtime_deps": attr.label_list(
            doc = "Runtime-only dependencies",
            default = [],
        ),
        "data": attr.label_list(
            doc = "Runtime data files",
            default = [],
            allow_files = True,
        ),
        "resources": attr.label_list(
            doc = "Resource files to include in the jar",
            default = [],
            allow_files = True,
        ),
        "test_class": attr.string(
            doc = "The Java class to be loaded by the test runner. Inferred from srcs if not provided.",
            default = "",
        ),
        "main_class": attr.string(
            doc = "Main class for the test runner. Defaults to BazelTestRunner.",
            default = "",
        ),
        "jvm_flags": attr.string_list(
            doc = "JVM flags to pass to the test",
            default = [],
        ),
        "env": attr.string_dict(
            doc = "Environment variables to set when the test is executed",
            default = {},
        ),
        "env_inherit": attr.string_list(
            doc = "Environment variables to inherit from the shell",
            default = [],
        ),
        "plugins": attr.label_list(
            doc = "Kotlin compiler plugins",
            default = [],
        ),
        "kotlinc_opts": attr.label(
            doc = "Kotlinc options target",
            default = None,
        ),
        "javac_opts": attr.label(
            doc = "Javac options target",
            default = None,
        ),
        "associates": attr.label_list(
            doc = "Kotlin deps for internal access within the same module",
            default = [],
        ),
        "module_name": attr.string(
            doc = "Kotlin module name",
            default = "",
        ),
        "resource_jars": attr.label_list(
            doc = "JARs containing resources to merge into the output",
            default = [],
        ),
        "resource_strip_prefix": attr.label(
            doc = "Path prefix to strip from resource paths",
            default = None,
        ),
    },
)

def _kt_jvm_binary_compat_impl(
        name,
        visibility = None,
        srcs = [],
        deps = [],
        runtime_deps = [],
        data = [],
        resources = [],
        main_class = "",
        jvm_flags = [],
        env = {},
        env_inherit = [],
        plugins = [],
        kotlinc_opts = None,
        javac_opts = None,
        associates = [],
        module_name = "",
        resource_jars = [],
        resource_strip_prefix = None,
        tags = [],
        **kwargs):
    """Implementation of kt_jvm_binary using kt_jvm_library + java_binary.

    This creates two targets:
    - <name>_lib: A kt_jvm_library that compiles the Kotlin sources
    - <name>: A java_binary that runs the application using the compiled library
    """
    lib_name = name + "_lib"

    # Create the kt_jvm_library for compilation
    kt_jvm_library(
        name = lib_name,
        srcs = srcs,
        deps = deps,
        resources = resources,
        plugins = plugins,
        kotlinc_opts = kotlinc_opts,
        javac_opts = javac_opts,
        associates = associates,
        module_name = module_name,
        resource_jars = resource_jars,
        resource_strip_prefix = resource_strip_prefix,
        visibility = ["//visibility:private"],
        tags = tags + ["manual"],
        **kwargs
    )

    # Create the java_binary
    # Note: env_inherit is accepted for API compatibility but has no effect on binaries
    # See https://github.com/bazelbuild/rules_kotlin/issues/1432
    java_binary(
        name = name,
        visibility = visibility,
        runtime_deps = [":" + lib_name] + (runtime_deps if runtime_deps else []),
        data = data,
        main_class = main_class,
        jvm_flags = jvm_flags if jvm_flags else [],
        env = env,
        tags = tags,
    )

# Define the symbolic macro for kt_jvm_binary
kt_jvm_binary_compat = macro(
    doc = """Kotlin JVM binary implemented as kt_jvm_library + java_binary.

This macro creates:
- A kt_jvm_library target (name = "<name>_lib") for Kotlin compilation
- A java_binary target (name = "<name>") for binary execution

This implementation provides better Windows compatibility by leveraging
Bazel's native java_binary rule.
""",
    implementation = _kt_jvm_binary_compat_impl,
    attrs = {
        "srcs": attr.label_list(
            doc = "Kotlin and Java source files",
            allow_files = [".kt", ".java", ".srcjar"],
            default = [],
        ),
        "deps": attr.label_list(
            doc = "Compile-time dependencies",
            default = [],
        ),
        "runtime_deps": attr.label_list(
            doc = "Runtime-only dependencies",
            default = [],
        ),
        "data": attr.label_list(
            doc = "Runtime data files",
            default = [],
            allow_files = True,
        ),
        "resources": attr.label_list(
            doc = "Resource files to include in the jar",
            default = [],
            allow_files = True,
        ),
        "main_class": attr.string(
            doc = "Name of class with main() method to use as entry point",
            mandatory = True,
        ),
        "jvm_flags": attr.string_list(
            doc = "JVM flags to pass to the binary",
            default = [],
        ),
        "env": attr.string_dict(
            doc = "Environment variables to set when the binary is executed",
            default = {},
        ),
        "env_inherit": attr.string_list(
            doc = "NOTE: Has no effect on binaries, kept for API compatibility. See https://github.com/bazelbuild/rules_kotlin/issues/1432",
            default = [],
        ),
        "plugins": attr.label_list(
            doc = "Kotlin compiler plugins",
            default = [],
        ),
        "kotlinc_opts": attr.label(
            doc = "Kotlinc options target",
            default = None,
        ),
        "javac_opts": attr.label(
            doc = "Javac options target",
            default = None,
        ),
        "associates": attr.label_list(
            doc = "Kotlin deps for internal access within the same module",
            default = [],
        ),
        "module_name": attr.string(
            doc = "Kotlin module name",
            default = "",
        ),
        "resource_jars": attr.label_list(
            doc = "JARs containing resources to merge into the output",
            default = [],
        ),
        "resource_strip_prefix": attr.label(
            doc = "Path prefix to strip from resource paths",
            default = None,
        ),
    },
)
