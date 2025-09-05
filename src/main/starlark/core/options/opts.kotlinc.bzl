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
load("//src/main/starlark/core/options:convert.bzl", "convert")
load("//src/main/starlark/core/options:derive.bzl", "derive")

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

_KOPTS_ALL = {
    "warn": struct(
        args = dict(
            default = "report",
            doc = "Control warning behaviour.",
            values = ["off", "report", "error"],
        ),
        type = attr.string,
        value_to_flag = {
            "off": ["-nowarn"],
            "report": None,
            "error": ["-Werror"],
        },
    ),
    "include_stdlibs": struct(
        args = dict(
            default = "all",
            doc = "Don't automatically include the Kotlin standard libraries into the classpath (stdlib and reflect).",
            values = ["all", "stdlib", "none"],
        ),
        type = attr.string,
        value_to_flag = {
            "all": None,
            "stdlib": ["-no-reflect"],
            "none": ["-no-stdlib"],
        },
    ),
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
    "x_suppress_version_warnings": struct(
        flag = "-Xsuppress-version-warnings",
        args = dict(
            default = False,
            doc = "Suppress warnings about outdated, inconsistent, or experimental language or API versions.",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-Xsuppress-version-warnings"],
        },
    ),
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
    "x_allow_result_return_type": struct(
        flag = "-Xallow-result-return-type",
        args = dict(
            default = False,
            doc = "Enable kotlin.Result as a return type",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-Xallow-result-return-type"],
        },
    ),
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
    "x_no_receiver_assertions": struct(
        flag = "-Xno-receiver-assertions",
        args = dict(
            default = False,
            doc = "Don't generate not-null assertion for extension receiver arguments of platform types",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-Xno-receiver-assertions"],
        },
    ),
    "x_no_optimized_callable_references": struct(
        flag = "-Xno-optimized-callable-references",
        args = dict(
            default = False,
            doc = "Do not use optimized callable reference superclasses. Available from 1.4.",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-Xno-optimized-callable-references"],
        },
    ),
    "x_explicit_api_mode": struct(
        flag = "-Xexplicit-api",
        args = dict(
            default = "off",
            doc = "Enable explicit API mode for Kotlin libraries.",
            values = ["off", "warning", "strict"],
        ),
        type = attr.string,
        value_to_flag = {
            "off": None,
            "warning": ["-Xexplicit-api=warning"],
            "strict": ["-Xexplicit-api=strict"],
        },
    ),
    "x_annotation_default_target": struct(
        args = dict(
            default = "off",
            doc = """Change the default annotation targets for constructor properties:
-Xannotation-default-target=first-only: use the first of the following allowed targets: '@param:', '@property:', '@field:';
-Xannotation-default-target=first-only-warn: same as first-only, and raise warnings when both '@param:' and either '@property:' or '@field:' are allowed;
-Xannotation-default-target=param-property: use '@param:' target if applicable, and also use the first of either '@property:' or '@field:';
default: 'first-only-warn' in language version 2.2+, 'first-only' in version 2.1 and before.""",
            values = ["off", "first-only", "first-only-warn", "param-property"],
        ),
        type = attr.string,
        value_to_flag = {
            "off": None,
            "first-only": ["-Xannotation-default-target=first-only"],
            "first-only-warn": ["-Xannotation-default-target=first-only-warn"],
            "param-property": ["-Xannotation-default-target=param-property"],
        },
    ),
    "java_parameters": struct(
        args = dict(
            default = False,
            doc = "Generate metadata for Java 1.8+ reflection on method parameters.",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-java-parameters"],
        },
    ),
    "x_multi_platform": struct(
        flag = "-Xmulti-platform",
        args = dict(
            default = False,
            doc = "Enable experimental language support for multi-platform projects",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-Xmulti-platform"],
        },
    ),
    "x_sam_conversions": struct(
        flag = "-Xsam-conversions",
        args = dict(
            default = "class",
            doc = "Change codegen behavior of SAM/functional interfaces",
            values = ["class", "indy"],
        ),
        type = attr.string,
        value_to_flag = {
            "class": ["-Xsam-conversions=class"],
            "indy": ["-Xsam-conversions=indy"],
        },
    ),
    "x_lambdas": struct(
        flag = "-Xlambdas",
        args = dict(
            default = "class",
            doc = "Change codegen behavior of lambdas",
            values = ["class", "indy"],
        ),
        type = attr.string,
        value_to_flag = {
            "class": ["-Xlambdas=class"],
            "indy": ["-Xlambdas=indy"],
        },
    ),
    "x_emit_jvm_type_annotations": struct(
        flag = "-Xemit-jvm-type-annotations",
        args = dict(
            default = False,
            doc = "Basic support for type annotations in JVM bytecode.",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-Xemit-jvm-type-annotations"],
        },
    ),
    "x_optin": struct(
        args = dict(
            default = [],
            doc = "Define APIs to opt-in to.",
        ),
        type = attr.string_list,
        value_to_flag = None,
        map_value_to_flag = _map_optin_class_to_flag,
    ),
    "x_use_fir": struct(
        # 1.6
        flag = "-Xuse-fir",
        args = dict(
            default = False,
            doc = "Compile using the experimental Kotlin Front-end IR. Available from 1.6.",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-Xuse-fir"],
        },
    ),
    "x_use_k2": struct(
        # 1.7
        flag = "-Xuse-k2",
        args = dict(
            default = False,
            doc = "Compile using experimental K2. K2 is a new compiler pipeline, no compatibility guarantees are yet provided",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-Xuse-k2"],
        },
    ),
    "x_no_optimize": struct(
        flag = "-Xno-optimize",
        args = dict(
            default = False,
            doc = "Disable optimizations",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-Xno-optimize"],
        },
    ),
    "x_backend_threads": struct(
        # 1.6.20, 1.7
        flag = "-Xbackend-threads",
        args = dict(
            default = 1,
            doc = "When using the IR backend, run lowerings by file in N parallel threads. 0 means use a thread per processor core. Default value is 1.",
        ),
        type = attr.int,
        value_to_flag = None,
        map_value_to_flag = _map_backend_threads_to_flag,
    ),
    "x_enable_incremental_compilation": struct(
        args = dict(
            default = False,
            doc = "Enable incremental compilation",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-Xenable-incremental-compilation"],
        },
    ),
    "x_report_perf": struct(
        flag = "-Xreport-perf",
        args = dict(
            default = False,
            doc = "Report detailed performance statistics",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-Xreport-perf"],
        },
    ),
    "x_use_fir_lt": struct(
        args = dict(
            default = False,
            doc = "Compile using LightTree parser with Front-end IR. Warning: this feature is far from being production-ready",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-Xuse-fir-lt"],
        },
    ),
    "x_no_source_debug_extension": struct(
        args = dict(
            default = False,
            doc = "Do not generate @kotlin.jvm.internal.SourceDebugExtension annotation on a class with the copy of SMAP",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-Xno-source-debug-extension"],
        },
    ),
    "x_type_enhancement_improvements_strict_mode": struct(
        args = dict(
            default = False,
            doc = "Enables strict mode for type enhancement improvements, enforcing stricter type checking and enhancements.",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-Xtype-enhancement-improvements-strict-mode"],
        },
    ),
    "x_jsr_305": struct(
        args = dict(
            default = "",
            doc = "Specifies how to handle JSR-305 annotations in Kotlin code. Options are 'default', 'ignore', 'warn', and 'strict'.",
            values = ["default", "ignore", "warn", "strict"],
        ),
        type = attr.string,
        value_to_flag = {
            "default": None,
            "ignore": ["-Xjsr305=ignore"],
            "warn": ["-Xjsr305=warn"],
            "strict": ["-Xjsr305=strict"],
        },
        map_value_to_flag = None,
    ),
    "x_assertions": struct(
        args = dict(
            default = "",
            doc = "Configures how assertions are handled. The 'jvm' option enables assertions in JVM code.",
            values = ["jvm"],
        ),
        type = attr.string,
        value_to_flag = {
            "default": None,
            "jvm": ["-Xassertions=jvm"],
        },
        map_value_to_flag = None,
    ),
    "x_jspecify_annotations": struct(
        args = dict(
            default = "",
            doc = "Controls how JSpecify annotations are treated. Options are 'default', 'ignore', 'warn', and 'strict'.",
            values = ["default", "ignore", "warn", "strict"],
        ),
        type = attr.string,
        value_to_flag = {
            "default": None,
            "ignore": ["-Xjspecify-annotations=ignore"],
            "warn": ["-Xjspecify-annotations=warn"],
            "strict": ["-Xjspecify-annotations=strict"],
        },
        map_value_to_flag = None,
    ),
    "x_consistent_data_class_copy_visibility": struct(
        args = dict(
            default = False,
            doc = "The effect of this compiler flag is the same as applying @ConsistentCopyVisibility annotation to all data classes in the module. See https://youtrack.jetbrains.com/issue/KT-11914",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-Xconsistent-data-class-copy-visibility"],
        },
    ),
    "jvm_target": struct(
        args = dict(
            default = "",
            doc = "The target version of the generated JVM bytecode",
            values = ["1.6", "1.8", "9", "10", "11", "12", "13", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24"],
        ),
        type = attr.string,
        value_to_flag = None,
        map_value_to_flag = _map_jvm_target_to_flag,
    ),
    "x_jdk_release": struct(
        args = dict(
            default = "",
            doc = """Compile against the specified JDK API version, similarly to javac's '-release'. This requires JDK 9 or newer.
                     The supported versions depend on the JDK used; for JDK 17+, the supported versions are 1.8 and 9–21.
                     This also sets the value of '-jvm-target' to be equal to the selected JDK version.""",
            values = ["1.6", "1.8", "9", "10", "11", "12", "13", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24"],
        ),
        type = attr.string,
        value_to_flag = None,
        map_value_to_flag = _map_jdk_release_to_flag,
    ),
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
}

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
