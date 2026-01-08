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

load("@com_github_jetbrains_kotlin//:capabilities.bzl", _KOTLIN_OPTS = "KOTLIN_OPTS")
load("@com_github_jetbrains_kotlin//:generated_opts.bzl", "GENERATED_KOPTS")
load("//src/main/starlark/core/options:convert.bzl", "convert")
load("//src/main/starlark/core/options:derive.bzl", "derive")

_ALLOWED_SUPPRESS_LEVELS = [
    "error",
    "warning",
    "disabled",
]

def _map_warning_level(values):
    if not values:
        return None

    args = []
    for k, v in values.items():
        if v not in _ALLOWED_SUPPRESS_LEVELS:
            fail("Error: Suppress key '{}' has an invalid value of '{}'".format(k, v))
        args.append("-Xwarning-level={}:{}".format(k, v))
    return args

def _map_optin_class_to_flag(values):
    return ["-opt-in=%s" % v for v in values]

def _map_backend_threads_to_flag(n):
    if n == 1:
        return None
    return ["-Xbackend-threads=%d" % n]

def _map_jvm_target_to_flag(version):
    if not version:
        return None
    return ["-jvm-target=%s" % version]

def _map_jdk_release_to_flag(version):
    if not version:
        return None
    return ["-Xjdk-release=%s" % version]

# Manual overrides for options that need custom handling.
# These take precedence over GENERATED_KOPTS.
_MANUAL_KOPTS = {
    # Custom handling for stdlib inclusion - not a direct kotlinc flag
    "include_stdlibs": struct(
        args = dict(
            default = "all",
            doc = "Don't automatically include the Kotlin standard libraries into the classpath (stdlib and reflect).",
            values = ["all", "stdlib", "none"],
        ),
        type = attr.string,
        value_to_flag = {
            "all": None,
            "none": ["-no-stdlib"],
            "stdlib": ["-no-reflect"],
        },
    ),
    # Custom JVM target with specific allowed values
    "jvm_target": struct(
        args = dict(
            default = "",
            doc = "The target version of the generated JVM bytecode",
            values = ["1.6", "1.8", "9", "10", "11", "12", "13", "15", "16", "17", "21"],
        ),
        type = attr.string,
        value_to_flag = None,
        map_value_to_flag = _map_jvm_target_to_flag,
    ),
    # Custom warning control - maps to different flags
    "warn": struct(
        args = dict(
            default = "report",
            doc = "Control warning behaviour.",
            values = ["off", "report", "error"],
        ),
        type = attr.string,
        value_to_flag = {
            "error": ["-Werror"],
            "off": ["-nowarn"],
            "report": None,
        },
    ),
    # Custom backend threads with int type
    "x_backend_threads": struct(
        flag = "-Xbackend-threads",
        args = dict(
            default = 1,
            doc = "When using the IR backend, run lowerings by file in N parallel threads. 0 means use a thread per processor core. Default value is 1.",
        ),
        type = attr.int,
        value_to_flag = None,
        map_value_to_flag = _map_backend_threads_to_flag,
    ),
    # Custom JDK release with specific allowed values
    "x_jdk_release": struct(
        args = dict(
            default = "",
            doc = """Compile against the specified JDK API version, similarly to javac's '-release'. This requires JDK 9 or newer.
                     The supported versions depend on the JDK used; for JDK 17+, the supported versions are 1.8 and 9-21.
                     This also sets the value of '-jvm-target' to be equal to the selected JDK version.""",
            values = ["1.6", "1.8", "9", "10", "11", "12", "13", "15", "16", "17", "21"],
        ),
        type = attr.string,
        value_to_flag = None,
        map_value_to_flag = _map_jdk_release_to_flag,
    ),
    # Keep lambdas/sam_conversions with empty string default for backward compat
    "x_lambdas": struct(
        flag = "-Xlambdas",
        args = dict(
            default = "class",
            doc = """Change codegen behavior of lambdas. Defaults to "class" (anonymous inner classes), which differs from Kotlin 2.x/Gradle default of "indy" (invokedynamic). Set to "indy" for Gradle-compatible bytecode.""",
            values = ["class", "indy"],
        ),
        type = attr.string,
        value_to_flag = {
            "": None,
            "class": ["-Xlambdas=class"],
            "indy": ["-Xlambdas=indy"],
        },
    ),
    # Custom opt-in handling
    "x_optin": struct(
        args = dict(
            default = [],
            doc = "Define APIs to opt-in to.",
        ),
        type = attr.string_list,
        value_to_flag = None,
        map_value_to_flag = _map_optin_class_to_flag,
    ),
    "x_sam_conversions": struct(
        flag = "-Xsam-conversions",
        args = dict(
            default = "class",
            doc = """Change codegen behavior of SAM/functional interfaces. Defaults to "class" (anonymous inner classes), which differs from Kotlin 2.x/Gradle default of "indy" (invokedynamic). Set to "indy" for Gradle-compatible bytecode.""",
            values = ["class", "indy"],
        ),
        type = attr.string,
        value_to_flag = {
            "": None,
            "class": ["-Xsam-conversions=class"],
            "indy": ["-Xsam-conversions=indy"],
        },
    ),
    # Custom suppress warning with derive mechanism
    "x_suppress_warning": struct(
        args = dict(
            default = [],
            doc = "Suppress specific warnings globally",
        ),
        type = attr.string_list,
        value_to_flag = {
            derive.info: derive.repeated_values_for("-Xsuppress-warning="),
        },
    ),
    # Custom warning level with dict type
    "x_warning_level": struct(
        args = dict(
            default = {},
            doc = "Suppress specific warnings globally. Ex: 'OPTION': '(error|warning|disabled)'",
        ),
        type = attr.string_dict,
        value_to_flag = None,
        map_value_to_flag = _map_warning_level,
    ),
}

# Merge generated options with manual overrides (manual takes precedence)
_KOPTS_ALL = dict(GENERATED_KOPTS)
_KOPTS_ALL.update(_MANUAL_KOPTS)

def _merge(key, rule_defined):
    """Merges rule option with compiler option."""
    if key not in _KOTLIN_OPTS:
        # No flag associated with option.
        return rule_defined
    generated = _KOTLIN_OPTS[key]
    merged = {k: getattr(k, rule_defined) for k in dir(rule_defined)}
    merged["doc"] = generated.doc
    merged["default"] = generated.default
    return struct(**merged)

def _no_merge(_, definition):
    return definition

_maybe_merge_definition = _merge if hasattr(_KOTLIN_OPTS, "get") else _no_merge

# Filters out options that are not available in current compiler release
_KOPTS = {
    attr: _maybe_merge_definition(attr, defn)
    for (attr, defn) in _KOPTS_ALL.items()
    if not hasattr(defn, "flag") or defn.flag in _KOTLIN_OPTS
}

KotlincOptions = provider(
    fields = {
        name: o.args["doc"]
        for name, o in _KOPTS.items()
    },
)

def _kotlinc_options_impl(ctx):
    return [KotlincOptions(**{n: getattr(ctx.attr, n, None) for n in _KOPTS})]

kt_kotlinc_options = rule(
    implementation = _kotlinc_options_impl,
    doc = "Define kotlin compiler options.",
    provides = [KotlincOptions],
    attrs = {n: o.type(**o.args) for n, o in _KOPTS.items()},
)

def kotlinc_options_to_flags(kotlinc_options):
    """Translate KotlincOptions to worker flags

    Args:
        kotlinc_options maybe containing KotlincOptions
    Returns:
        list of flags to add to the command line.
    """
    return convert.javac_options_to_flags(_KOPTS, kotlinc_options)
