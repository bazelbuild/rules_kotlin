# Copyright 2020 The Bazel Authors. All rights reserved.
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

_KOPTS = {
    "warn": struct(
        args = dict(
            default = "report",
            doc = "Control warning behaviour.",
            values = ["off", "report", "error"],
        ),
        type = attr.string,
    ),
    "include_stdlibs": struct(
        args = dict(
            default = "all",
            doc = "Don't automatically include the Kotlin standard libraries into the classpath (stdlib and reflect).",
            values = ["all", "stdlib", "none"],
        ),
        type = attr.string,
    ),
    "x_use_experimental": struct(
        args = dict(
            default = True,
            doc = "Allow the experimental language features.",
        ),
        type = attr.bool,
    ),
    "x_use_ir": struct(
        args = dict(
            default = False,
            doc = "Enable or disable the experimental IR backend.",
        ),
        type = attr.bool,
    ),
    "x_allow_jvm_ir_dependencies": struct(
        args = dict(
            default = False,
            doc = "Suppress errors thrown when using dependencies not compiled by the IR backend.",
        ),
        type = attr.bool,
    ),
    "x_skip_prerelease_check": struct(
        args = dict(
            default = False,
            doc = "Suppress errors thrown when using pre-release classes.",
        ),
        type = attr.bool,
    ),
     "x_inline_classes": struct(
        args = dict(
            default = False,
            doc = "Enable experimental inline classes",
        ),
        type = attr.bool,
    ),
    "x_allow_result_return_type": struct(
        args = dict(
            default = False,
            doc = "Enable kotlin.Result as a return type",
        ),
        type = attr.bool,
    ),
    "x_jvm_default": struct(
        args = dict(
            default = "off",
            doc = "Specifies that a JVM default method should be generated for non-abstract Kotlin interface member.",
            values = ["off", "enable", "compatibility"],
        ),
        type = attr.string,
    ),
    "x_no_optimized_callable_references": struct(
        args = dict(
            default = False,
            doc = "Do not use optimized callable reference superclasses. Available from 1.4.",
        ),
        type = attr.bool,
    ),
}

KotlincOptions = provider(
    fields = {
        name: o.args["doc"]
        for name, o in _KOPTS.items()
    },
)

def _kotlinc_options_impl(ctx):
    return struct(
        providers = [
            KotlincOptions(**{n: getattr(ctx.attr, n, None) for n in _KOPTS}),
        ],
    )

kt_kotlinc_options = rule(
    implementation = _kotlinc_options_impl,
    doc = "Define kotlin compiler options.",
    provides = [KotlincOptions],
    attrs = {
        n: o.type(**o.args)
        for n, o in _KOPTS.items()
    },
)

_JOPTS = {
    "warn": struct(
        args = dict(
            default = "report",
            doc = "Control warning behaviour.",
            values = ["off", "report", "error"],
        ),
        type = attr.string,
    ),
    "x_ep_disable_all_checks": struct(
        args = dict(
            default = False,
            doc = "See javac -XepDisableAllChecks documentation",
        ),
        type = attr.bool,
    ),
    "x_lint": struct(
        args = dict(
            default = [],
            doc = "See javac -Xlint: documentation",
        ),
        type = attr.string_list,
    ),
    "xd_suppress_notes": struct(
        args = dict(
            default = False,
            doc = "See javac -XDsuppressNotes documentation",
        ),
        type = attr.bool,
    ),
}

JavacOptions = provider(
    fields = {
        name: o.args["doc"]
        for name, o in _JOPTS.items()
    },
)

def _javac_options_impl(ctx):
    return struct(
        providers = [
            JavacOptions(**{n: getattr(ctx.attr, n, None) for n in _JOPTS}),
        ],
    )

kt_javac_options = rule(
    implementation = _javac_options_impl,
    doc = "Define java compiler options for kt_jvm_* rules with java sources.",
    provides = [JavacOptions],
    attrs = {n: o.type(**o.args) for n, o in _JOPTS.items()},
)
