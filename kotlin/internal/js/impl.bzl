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
    _KtJsInfo = "KtJsInfo",
    _TOOLCHAIN_TYPE = "TOOLCHAIN_TYPE",
)
load(
    "//kotlin/internal:opts.bzl",
    "KotlincOptions",
    "kotlinc_options_to_flags",
)
load(
    "//kotlin/internal/utils:utils.bzl",
    _utils = "utils",
)

# The following kt-js flags are currently not used.
# -Xfriend-modules=<path>
#  -output-postfix <path>     Path to file which will be added to the end of output file
#  -output-prefix <path>      Path to file which will be added to the beginning of output file
#  -source-map-base-dirs <path> Base directories which are used to calculate relative paths to source files in source map
#  -source-map-embed-sources { always, never, inlining }
#                             Embed source files into source map
#  -source-map-prefix         Prefix for paths in a source map
#  -Xtyped-arrays

def kt_js_library_impl(ctx):
    toolchain = ctx.toolchains[_TOOLCHAIN_TYPE]

    libraries = depset([d[_KtJsInfo].jar for d in ctx.attr.deps])

    args = _utils.init_args(
        ctx,
        "kt_js_library",
        _utils.derive_module_name(ctx),
    )

    kotlinc_options = ctx.attr.kotlinc_opts[KotlincOptions] if ctx.attr.kotlinc_opts else toolchain.kotlinc_options

    args.add_all(
        "--kotlin_js_passthrough_flags",
        kotlinc_options_to_flags(kotlinc_options) + [
            "-source-map",
            "-Xir-produce-klib-dir",
            "-no-stdlib",
            "-module-kind",
            ctx.attr.module_kind,
            "-target",
            ctx.attr.js_target,
        ],
    )

    args.add("--output", ctx.outputs.js)
    args.add("--kotlin_output_js_jar", ctx.outputs.jar)
    args.add("--kotlin_output_srcjar", ctx.outputs.srcjar)
    args.add("--strict_kotlin_deps", "off")
    args.add("--reduced_classpath_mode", "NONE")

    args.add_all("--kotlin_js_libraries", libraries, omit_if_empty = False)
    args.add_all("--sources", ctx.files.srcs)

    inputs, _, input_manifests = ctx.resolve_command(tools = [toolchain.kotlinbuilder, toolchain.kotlin_home])

    ctx.actions.run(
        mnemonic = "KotlinCompile",
        inputs = depset(inputs + ctx.files.srcs, transitive = [libraries]),
        outputs = [
            ctx.outputs.js,
            ctx.outputs.js_map,
            ctx.outputs.jar,
            ctx.outputs.srcjar,
        ],
        executable = toolchain.kotlinbuilder.files_to_run.executable,
        execution_requirements = {"supports-workers": "1"},
        arguments = [args],
        progress_message = "Compiling Kotlin to JS %%{label} { kt: %d }" % len(ctx.files.srcs),
        input_manifests = input_manifests,
        env = {
            "REPOSITORY_NAME": _utils.builder_workspace_name(ctx),
        },
    )

    return [
        DefaultInfo(
            files = depset([ctx.outputs.js, ctx.outputs.js_map]),
        ),
        _KtJsInfo(
            js = ctx.outputs.js,
            js_map = ctx.outputs.js_map,
            jar = ctx.outputs.jar,
            srcjar = ctx.outputs.srcjar,
        ),
    ]

# buildifier: disable=unused-variable
def _strip_version(jarfile):
    """strip version suffix if present
       e.g. kotlinx-html-js-0.6.12.jar ->  kotlinx-html-js.jar
    """
    if not jarfile.endswith(".jar"):
        fail("_strip_version expects paths ending with .jar")
    segments = jarfile[:-len(".jar")].split("-")

    # Remove the last segment if all digits separated by dot
    parts = segments[-1].split(".")
    if len([p for p in parts if not p.isdigit()]) == 0:
        segments = segments[0:-1]
    return "%s.jar" % "-".join(segments)

def kt_js_import_impl(ctx):
    if len(ctx.files.jars) != 1:
        fail("a single jar should be supplied, multiple jars not supported")
    jar_file = ctx.files.jars[0]

    srcjar = ctx.files.srcjar[0] if len(ctx.files.srcjar) == 1 else None

    args = ctx.actions.args()
    args.add("--jar", jar_file)
    args.add("--import_pattern", "^[^/.]+\\.js$")
    args.add("--import_out", ctx.outputs.js)
    args.add("--aux_pattern", "^[^/]+\\.js\\.map$")
    args.add("--aux_out", ctx.outputs.js_map)

    tools, _, input_manifest = ctx.resolve_command(tools = [ctx.attr._importer])
    ctx.actions.run(
        inputs = [jar_file],
        tools = tools,
        executable = ctx.executable._importer,
        outputs = [
            ctx.outputs.js,
            ctx.outputs.js_map,
        ],
        arguments = [args],
        input_manifests = input_manifest,
    )

    return [
        DefaultInfo(
            files = depset([ctx.outputs.js, ctx.outputs.js_map]),
        ),
        _KtJsInfo(
            js = ctx.outputs.js,
            js_map = ctx.outputs.js_map,
            jar = jar_file,
            srcjar = srcjar,
        ),
    ]
