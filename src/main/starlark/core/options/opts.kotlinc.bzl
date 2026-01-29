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

def _map_api_version_to_flag(version):
    if not version:
        return None
    return ["-api-version=%s" % version]

def _map_language_version_to_flag(version):
    if not version:
        return None
    return ["-language-version=%s" % version]

def _map_plugins_optins_to_flag(values):
    result = []
    for v in values:
        result += ["-P", v]
    return result

def _map_x_x_language_to_flag(values):
    return ["-XXLanguage:%s" % v for v in values]

def _map_backend_threads_to_flag(n):
    if n == 1:
        return None
    return ["-Xbackend-threads=%d" % n]

def _map_jvm_target_to_flag(version):
    if not version:
        return None
    # Map short versions to full versions
    if version == "7":
        version = "1.7"
    elif version == "8":
        version = "1.8"
    return ["-jvm-target=%s" % version]

def _map_jdk_release_to_flag(version):
    if not version:
        return None
    # Map short versions to full versions
    if version == "7":
        version = "1.7"
    elif version == "8":
        version = "1.8"
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
            values = ["1.6", "1.7", "1.8", "7", "8", "9", "10", "11", "12", "13", "15", "16", "17", "21"],
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
            values = ["1.6", "1.7", "1.8", "7", "8", "9", "10", "11", "12", "13", "15", "16", "17", "21"],
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
    # API version for Kotlin bundled libraries
    "api_version": struct(
        args = dict(
            default = "",
            doc = "Allow using declarations only from the specified version of Kotlin bundled libraries",
        ),
        type = attr.string,
        value_to_flag = None,
        map_value_to_flag = _map_api_version_to_flag,
    ),
    # Language version for source compatibility
    "language_version": struct(
        args = dict(
            default = "",
            doc = "Provide source compatibility with the specified version of Kotlin",
        ),
        type = attr.string,
        value_to_flag = None,
        map_value_to_flag = _map_language_version_to_flag,
    ),
    # Opt-in annotations (alias for x_optin for compatibility)
    "opt_in": struct(
        args = dict(
            default = [],
            doc = "Define APIs to opt-in to.",
        ),
        type = attr.string_list,
        value_to_flag = None,
        map_value_to_flag = _map_optin_class_to_flag,
    ),
    # Plugin options for compiler plugins
    "plugin_options": struct(
        args = dict(
            default = [],
            doc = "Define compiler plugin options.",
        ),
        type = attr.string_list,
        value_to_flag = None,
        map_value_to_flag = _map_plugins_optins_to_flag,
    ),
    # Progressive mode
    "progressive": struct(
        flag = "-progressive",
        args = dict(
            default = False,
            doc = "Enable the progressive mode for the compiler",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-progressive"],
        },
    ),
    # Allow kotlin package
    "x_allow_kotlin_package": struct(
        args = dict(
            default = False,
            doc = "Allow compiling code in the 'kotlin' package, and allow not requiring 'kotlin.stdlib' in 'module-info'.",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-Xallow-kotlin-package"],
        },
    ),
    # Allow result return type
    "x_allow_result_return_type": struct(
        args = dict(
            default = False,
            doc = "Enable kotlin.Result as a return type",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-Xallow-result-return-type"],
        },
    ),
    # Allow unstable dependencies
    "x_allow_unstable_dependencies": struct(
        flag = "-Xallow-unstable-dependencies",
        args = dict(
            default = False,
            doc = "Do not report errors on classes in dependencies that were compiled by an unstable version of the Kotlin compiler.",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-Xallow-unstable-dependencies"],
        },
    ),
    # Consistent data class copy visibility
    "x_consistent_data_class_copy_visibility": struct(
        args = dict(
            default = False,
            doc = "The effect of this compiler flag is the same as applying @ConsistentCopyVisibility annotation to all data classes in the module.",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-Xconsistent-data-class-copy-visibility"],
        },
    ),
    # Context parameters
    "x_context_parameters": struct(
        flag = "-Xcontext-parameters",
        args = dict(
            default = False,
            doc = "Enable experimental context parameters.",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-Xcontext-parameters"],
        },
    ),
    # Context receivers
    "x_context_receivers": struct(
        flag = "-Xcontext-receivers",
        args = dict(
            default = False,
            doc = "Enable experimental context receivers.",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-Xcontext-receivers"],
        },
    ),
    # Explicit API mode with more values
    "x_explicit_api_mode": struct(
        flag = "-Xexplicit-api",
        args = dict(
            default = "disable",
            doc = "Enable explicit API mode for Kotlin libraries.",
            values = ["disable", "off", "warning", "strict"],
        ),
        type = attr.string,
        value_to_flag = {
            "disable": None,
            "off": None,
            "warning": ["-Xexplicit-api=warning"],
            "strict": ["-Xexplicit-api=strict"],
        },
    ),
    # Inline classes
    "x_inline_classes": struct(
        flag = "-Xinline-classes",
        args = dict(
            default = False,
            doc = "Enable experimental inline classes",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-Xinline-classes"],
        },
    ),
    # JVM default methods
    "x_jvm_default": struct(
        flag = "-Xjvm-default",
        args = dict(
            default = "off",
            doc = "Specifies that a JVM default method should be generated for non-abstract Kotlin interface member.",
            values = ["off", "enable", "disable", "compatibility", "all-compatibility", "all"],
        ),
        type = attr.string,
        value_to_flag = {
            "off": None,
            "enable": ["-Xjvm-default=enable"],
            "disable": ["-Xjvm-default=disable"],
            "compatibility": ["-Xjvm-default=compatibility"],
            "all-compatibility": ["-Xjvm-default=all-compatibility"],
            "all": ["-Xjvm-default=all"],
        },
    ),
    # No call assertions
    "x_no_call_assertions": struct(
        flag = "-Xno-call-assertions",
        args = dict(
            default = False,
            doc = "Don't generate not-null assertions for arguments of platform types",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-Xno-call-assertions"],
        },
    ),
    # No param assertions
    "x_no_param_assertions": struct(
        flag = "-Xno-param-assertions",
        args = dict(
            default = False,
            doc = "Don't generate not-null assertions on parameters of methods accessible from Java",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-Xno-param-assertions"],
        },
    ),
    # Skip prerelease check
    "x_skip_prerelease_check": struct(
        flag = "-Xskip-prerelease-check",
        args = dict(
            default = False,
            doc = "Suppress errors thrown when using pre-release classes.",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-Xskip-prerelease-check"],
        },
    ),
    # Strict Java nullability assertions
    "x_strict_java_nullability_assertions": struct(
        args = dict(
            default = False,
            doc = "Enable strict Java nullability assertions.",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-Xstrict-java-nullability-assertions"],
        },
    ),
    # WASM attach JS exception
    "x_wasm_attach_js_exception": struct(
        args = dict(
            default = False,
            doc = "Enable experimental support for attaching JavaScript exceptions to Kotlin exceptions.",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-Xwasm-attach-js-exception"],
        },
    ),
    # WASM KClass FQN
    "x_wasm_kclass_fqn": struct(
        args = dict(
            default = False,
            doc = "Enable experimental support for KClass::qualifiedName usage.",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-Xwasm-kclass-fqn"],
        },
    ),
    # When guards
    "x_when_guards": struct(
        flag = "-Xwhen-guards",
        args = dict(
            default = False,
            doc = "Enable experimental language support for when guards.",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-Xwhen-guards"],
        },
    ),
    # XXLanguage flags
    "x_x_language": struct(
        args = dict(
            default = [],
            doc = "Language compatibility flags.",
        ),
        type = attr.string_list,
        value_to_flag = None,
        map_value_to_flag = _map_x_x_language_to_flag,
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
