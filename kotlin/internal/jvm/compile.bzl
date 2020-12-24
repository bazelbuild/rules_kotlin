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
    _KtCompilerPluginInfo = "KtCompilerPluginInfo",
    _KtJvmInfo = "KtJvmInfo",
    _TOOLCHAIN_TYPE = "TOOLCHAIN_TYPE",
)
load(
    "//kotlin/internal/jvm:plugins.bzl",
    _merge_plugin_infos = "merge_plugin_infos",
    _plugin_mappers = "mappers",
)
load(
    "//kotlin/internal:opts.bzl",
    _KotlincOptions = "KotlincOptions",
    _JavacOptions = "JavacOptions",
)
load(
    "//kotlin/internal:compiler_plugins.bzl",
    _plugins_to_classpaths = "plugins_to_classpaths",
    _plugins_to_options = "plugins_to_options",
)
load(
    "//kotlin/internal/utils:utils.bzl",
    _utils = "utils",
)
load(
    "@bazel_tools//tools/jdk:toolchain_utils.bzl",
    "find_java_runtime_toolchain",
    "find_java_toolchain",
)

# UTILITY ##############################################################################################################
def _java_info(target):
    return target[JavaInfo] if JavaInfo in target else None

def _partitioned_srcs(srcs):
    """Creates a struct of srcs sorted by extension. Fails if there are no sources."""
    kt_srcs = []
    java_srcs = []
    src_jars = []

    for f in srcs:
        if f.path.endswith(".kt"):
            kt_srcs.append(f)
        elif f.path.endswith(".java"):
            java_srcs.append(f)
        elif f.path.endswith(".srcjar"):
            src_jars.append(f)

    return struct(
        kt = kt_srcs,
        java = java_srcs,
        all_srcs = kt_srcs + java_srcs,
        src_jars = src_jars,
    )

def _compiler_toolchains(ctx):
    """Creates a struct of the relevant compilation toolchains"""
    return struct(
        kt = ctx.toolchains[_TOOLCHAIN_TYPE],
        java = find_java_toolchain(ctx, ctx.attr._java_toolchain),
        java_runtime = find_java_runtime_toolchain(ctx, ctx.attr._host_javabase),
    )

def _compiler_friends(ctx, friends):
    """Creates a struct of friends meta data"""

    # TODO extract and move this into common. Need to make it generic first.
    if len(friends) == 0:
        return struct(
            targets = [],
            module_name = _utils.derive_module_name(ctx),
            paths = [],
        )
    elif len(friends) == 1:
        if friends[0][_KtJvmInfo] == None:
            fail("only kotlin dependencies can be friends")
        elif ctx.attr.module_name:
            fail("if friends has been set then module_name cannot be provided")
        else:
            return struct(
                targets = friends,
                paths = _java_info(friends[0]).compile_jars,
                module_name = friends[0][_KtJvmInfo].module_name,
            )
    else:
        fail("only one friend is possible")

def _jvm_deps(toolchains, friend, deps, runtime_deps = []):
    """Encapsulates jvm dependency metadata."""
    dep_infos = [_java_info(d) for d in friend.targets + deps] + [toolchains.kt.jvm_stdlibs]
    return struct(
        deps = dep_infos,
        compile_jars = depset(
            transitive = [
                d.compile_jars
                for d in dep_infos
            ] + [
                d.transitive_compile_time_jars
                for d in dep_infos
            ],
        ),
        runtime_deps = [_java_info(d) for d in runtime_deps],
    )

def _java_infos_to_compile_jars(java_infos):
    return depset(transitive = [j.compile_jars for j in java_infos])

def _exported_plugins(deps):
    """Encapsulates compiler dependency metadata."""
    plugins = []
    for dep in deps:
        if _KtJvmInfo in dep and dep[_KtJvmInfo] != None:
            plugins.extend(dep[_KtJvmInfo].exported_compiler_plugins.to_list())
    return plugins

def _collect_plugins_for_export(local, exports):
    """Collects into a depset. """
    return depset(
        local,
        transitive = [
            e[_KtJvmInfo].exported_compiler_plugins
            for e in exports
            if _KtJvmInfo in e and e[_KtJvmInfo]
        ],
    )

_CONVENTIONAL_RESOURCE_PATHS = [
    "src/main/resources",
    "src/test/resources",
    "kotlin",
]

def _adjust_resources_path_by_strip_prefix(path, resource_strip_prefix):
    if not path.startswith(resource_strip_prefix):
        fail("Resource file %s is not under the specified prefix to strip" % path)

    clean_path = path[len(resource_strip_prefix):]
    return resource_strip_prefix, clean_path

def _adjust_resources_path_by_default_prefixes(path):
    for cp in _CONVENTIONAL_RESOURCE_PATHS:
        dir_1, dir_2, rel_path = path.partition(cp)
        if rel_path:
            return dir_1 + dir_2, rel_path
    return "", path

def _adjust_resources_path(path, resource_strip_prefix):
    if resource_strip_prefix:
        return _adjust_resources_path_by_strip_prefix(path, resource_strip_prefix)
    else:
        return _adjust_resources_path_by_default_prefixes(path)

def _merge_kt_jvm_info(module_name, providers):
    language_versions = {p.language_version: True for p in providers if p.language_version}
    if len(language_versions) != 1:
        fail("Conflicting kt language versions: %s" % language_versions)
    return _KtJvmInfo(
        language_versions.keys()[0],
        modules_jar = [p.module_jars for p in providers],
        exported_compiler_plugins = depset(transitive = [
            p.exported_compiler_plugins
            for p in providers
        ]),
    )

def _kotlinc_options_provider_to_flags(opts, language_version):
    if not opts:
        return ""
    
    # Validate the compiler opts before they are mapped over to flags
    _validate_kotlinc_options(opts, language_version)

    flags = []
    if opts.warn == "off":
        flags.append("-nowarn")
    elif opts.warn == "error":
        flags.append("-Werror")
    if opts.x_use_experimental:
        flags.append("-Xuse-experimental=kotlin.Experimental")
    if opts.x_use_ir:
        flags.append("-Xuse-ir")
    if opts.x_allow_jvm_ir_dependencies:
        flags.append("-Xallow-jvm-ir-dependencies")
    if opts.x_skip_prerelease_check:
        flags.append("-Xskip-prerelease-check")
    if opts.x_inline_classes:
        flags.append("-Xinline-classes")
    if opts.x_allow_result_return_type:
        flags.append("-Xallow-result-return-type")
    if opts.include_stdlibs == "stdlib":
        flags.append("-no-reflect")
    elif opts.include_stdlibs == "none":
        flags.append("-no-stdlib")
    if opts.x_jvm_default == "enable":
        flags.append("-Xjvm-default=enable")
    elif opts.x_jvm_default == "compatibility":
        flags.append("-Xjvm-default=compatibility")
    if opts.x_no_optimized_callable_references:
        flags.append("-Xno-optimized-callable-references")
    return flags

def _validate_kotlinc_options(opts, language_version):
    if opts.x_allow_jvm_ir_dependencies and language_version < "1.4":
        fail("The x_allow_jvm_ir_dependencies flag is only avaliable in Kotlin version 1.4 or greater")
    pass

def _javac_options_provider_to_flags(opts):
    if not opts:
        return ""

    flags = []
    if opts.warn == "off":
        flags.append("-nowarn")
    elif opts.warn == "error":
        flags.append("-Werror")
    if opts.x_ep_disable_all_checks:
        flags.append("-XepDisableAllChecks")
    if opts.x_lint:
        flags.extend(["-Xlint:%s" % check for check in opts.x_lint])
    if opts.xd_suppress_notes:
        flags.append("-XDsuppressNotes")
    return flags

def _format_compile_plugin_options(options):
    """Format options into id:value for cmd line."""
    return ["%s:%s" % (o.id, o.value) for o in options]

# INTERNAL ACTIONS #####################################################################################################
def _fold_jars_action(ctx, rule_kind, toolchains, output_jar, input_jars, action_type = ""):
    """Set up an action to Fold the input jars into a normalized output jar."""
    args = ctx.actions.args()
    args.add_all([
        "--normalize",
        "--compression",
    ])
    args.add_all([
        "--deploy_manifest_lines",
        "Target-Label: %s" % str(ctx.label),
        "Injecting-Rule-Kind: %s" % rule_kind,
    ])
    args.add("--output", output_jar)
    args.add_all(input_jars, before_each = "--sources")
    ctx.actions.run(
        mnemonic = "KotlinFoldJars" + action_type,
        inputs = input_jars,
        outputs = [output_jar],
        executable = toolchains.java.single_jar,
        arguments = [args],
        progress_message = "Merging Kotlin output jar %s%s from %d inputs" % (
            ctx.label,
            "" if not action_type else " (%s)" % action_type,
            len(input_jars),
        ),
    )

def _resourcejar_args_action(ctx):
    res_cmd = []
    for f in ctx.files.resources:
        c_dir, res_path = _adjust_resources_path(f.short_path, ctx.attr.resource_strip_prefix)
        target_path = res_path
        if target_path[0] == "/":
            target_path = target_path[1:]
        line = "{target_path}={c_dir}{res_path}\n".format(
            res_path = res_path,
            target_path = target_path,
            c_dir = c_dir,
        )
        res_cmd.extend([line])
    zipper_args_file = ctx.actions.declare_file("%s_resources_zipper_args" % ctx.label.name)
    ctx.actions.write(zipper_args_file, "".join(res_cmd))
    return zipper_args_file

def _build_resourcejar_action(ctx):
    """sets up an action to build a resource jar for the target being compiled.
    Returns:
        The file resource jar file.
    """
    resources_jar_output = ctx.actions.declare_file(ctx.label.name + "-resources.jar")
    zipper_args = _resourcejar_args_action(ctx)
    ctx.actions.run_shell(
        mnemonic = "KotlinZipResourceJar",
        inputs = ctx.files.resources + [zipper_args],
        tools = [ctx.executable._zipper],
        outputs = [resources_jar_output],
        command = "{zipper} c {resources_jar_output} @{path}".format(
            path = zipper_args.path,
            resources_jar_output = resources_jar_output.path,
            zipper = ctx.executable._zipper.path,
        ),
        progress_message = "Creating intermediate resource jar %s" % ctx.label,
    )
    return resources_jar_output

def _run_merge_jdeps_action(ctx, rule_kind, toolchains, jdeps, outputs):
    """Creates a Jdeps merger action invocation."""
    args = ctx.actions.args()
    args.set_param_file_format("multiline")
    args.use_param_file("--flagfile=%s", use_always = True)

    args.add("--target_label", ctx.label)

    for f, path in outputs.items():
        args.add("--" + f, path)

    args.add_all("--inputs", jdeps, omit_if_empty = True)

    mnemonic = "JdepsMerge"
    progress_message = "%s %s { jdeps: %d }" % (
        mnemonic,
        ctx.label,
        len(jdeps),
    )

    tools, input_manifests = ctx.resolve_tools(
        tools = [
            toolchains.kt.jdeps_merger,
        ],
    )

    ctx.actions.run(
        mnemonic = mnemonic,
        inputs = depset(jdeps),
        tools = tools,
        input_manifests = input_manifests,
        outputs = [f for f in outputs.values()],
        executable = toolchains.kt.jdeps_merger.files_to_run.executable,
        execution_requirements = {
            "supports-workers": "1",
        },
        arguments = [args],
        progress_message = progress_message,
    )

def _run_kt_builder_action(
        ctx,
        rule_kind,
        toolchains,
        srcs,
        generated_src_jars,
        friend,
        compile_deps,
        annotation_processors,
        transitive_runtime_jars,
        plugins,
        outputs,
        build_java = True,
        build_kotlin = True,
        mnemonic = "KotlinCompile"):
    """Creates a KotlinBuilder action invocation."""
    args = _utils.init_args(ctx, rule_kind, friend.module_name)

    for f, path in outputs.items():
        args.add("--" + f, path)

    # Unwrap kotlinc_options/javac_options options or default to the ones being provided by the toolchain
    kotlinc_options = ctx.attr.kotlinc_opts[_KotlincOptions] if ctx.attr.kotlinc_opts else toolchains.kt.kotlinc_options
    javac_options = ctx.attr.javac_opts[_JavacOptions] if ctx.attr.javac_opts else toolchains.kt.javac_options
    args.add_all("--kotlin_passthrough_flags", _kotlinc_options_provider_to_flags(kotlinc_options, toolchains.kt.language_version))
    args.add_all("--javacopts", _javac_options_provider_to_flags(javac_options))
    
    # TODO: Implement Strict Kotlin deps: (https://github.com/bazelbuild/rules_kotlin/issues/419)
    # This flag is currently unused by the builder but required for the unused_deps tool
    args.add_all("--direct_dependencies", _java_infos_to_compile_jars(compile_deps.deps))
    args.add_all("--classpath", compile_deps.compile_jars)
    args.add_all("--sources", srcs.all_srcs, omit_if_empty = True)
    args.add_all("--source_jars", srcs.src_jars + generated_src_jars, omit_if_empty = True)

    args.add_joined("--kotlin_friend_paths", friend.paths, join_with = "\n")

    # Collect and prepare plugin descriptor for the worker.
    args.add_all(
        "--processors",
        annotation_processors,
        map_each = _plugin_mappers.kt_plugin_to_processor,
        omit_if_empty = True,
    )
    args.add_all(
        "--processorpath",
        annotation_processors,
        map_each = _plugin_mappers.kt_plugin_to_processorpath,
        omit_if_empty = True,
    )

    compiler_plugins = [
        p[_KtCompilerPluginInfo]
        for p in plugins
        if _KtCompilerPluginInfo in p and p[_KtCompilerPluginInfo]
    ]

    stubs_compiler_plugins = [
        kcp
        for kcp in compiler_plugins
        if kcp.stubs
    ]

    compiler_compiler_plugins = [
        ccp
        for ccp in compiler_plugins
        if ccp.compile
    ]

    if compiler_plugins and not (stubs_compiler_plugins or compiler_compiler_plugins):
        fail("plugins but no phase plugins: %s" % compiler_plugins)

    args.add_all(
        "--stubs_plugin",
        [j for p in stubs_compiler_plugins for j in p.plugin_jars],
        omit_if_empty = True,
    )

    args.add_all(
        "--stubs_plugin_classpath",
        depset(transitive = [p.classpath for p in stubs_compiler_plugins]),
        omit_if_empty = True,
    )

    args.add_all(
        "--stubs_plugin_options",
        [p.options for p in stubs_compiler_plugins],
        map_each = _format_compile_plugin_options,
        omit_if_empty = True,
    )

    args.add_all(
        "--compiler_plugin",
        [j for p in compiler_compiler_plugins for j in p.plugin_jars],
        omit_if_empty = True,
    )

    args.add_all(
        "--compiler_plugin_classpath",
        depset(transitive = [p.classpath for p in compiler_compiler_plugins]),
        omit_if_empty = True,
    )

    args.add_all(
        "--compiler_plugin_options",
        [p.options for p in compiler_compiler_plugins],
        map_each = _format_compile_plugin_options,
        omit_if_empty = True,
    )

    args.add("--build_java", build_java)
    args.add("--build_kotlin", build_kotlin)

    progress_message = "%s %s { kt: %d, java: %d, srcjars: %d } for %s" % (
        mnemonic,
        ctx.label,
        len(srcs.kt),
        len(srcs.java),
        len(srcs.src_jars),
        ctx.var.get("TARGET_CPU", "UNKNOWN CPU"),
    )

    tools, input_manifests = ctx.resolve_tools(
        tools = [
            toolchains.kt.kotlinbuilder,
            toolchains.kt.kotlin_home,
        ],
    )

    ctx.actions.run(
        mnemonic = mnemonic,
        inputs = depset(
            srcs.all_srcs + srcs.src_jars + generated_src_jars,
            transitive = [compile_deps.compile_jars, transitive_runtime_jars] + [p.classpath for p in compiler_plugins],
        ),
        tools = tools,
        input_manifests = input_manifests,
        outputs = [f for f in outputs.values()],
        executable = toolchains.kt.kotlinbuilder.files_to_run.executable,
        execution_requirements = {"supports-workers": "1"},
        arguments = [args],
        progress_message = progress_message,
        env = {
            "LC_CTYPE": "en_US.UTF-8",  # For Java source files
        },
    )

# MAIN ACTIONS #########################################################################################################

def kt_jvm_produce_jar_actions(ctx, rule_kind):
    """This macro sets up a compile action for a Kotlin jar.

    Args:
        ctx: Invoking rule ctx, used for attr, actions, and label.
        rule_kind: The rule kind --e.g., `kt_jvm_library`.
    Returns:
        A struct containing the providers JavaInfo (`java`) and `kt` (KtJvmInfo). This struct is not intended to be
        used as a legacy provider -- rather the caller should transform the result.
    """
    toolchains = _compiler_toolchains(ctx)
    srcs = _partitioned_srcs(ctx.files.srcs)
    friend = _compiler_friends(ctx, friends = getattr(ctx.attr, "friends", []))
    compile_deps = _jvm_deps(
        toolchains,
        friend,
        deps = ctx.attr.deps,
        runtime_deps = ctx.attr.runtime_deps,
    )
    annotation_processors = _plugin_mappers.targets_to_annotation_processors(ctx.attr.plugins + ctx.attr.deps)
    transitive_runtime_jars = _plugin_mappers.targets_to_transitive_runtime_jars(ctx.attr.plugins + ctx.attr.deps)
    plugins = ctx.attr.plugins + _exported_plugins(deps = ctx.attr.deps)

    generated_src_jars = []
    if toolchains.kt.experimental_use_abi_jars:
        compile_jar = ctx.actions.declare_file(ctx.label.name + ".abi.jar")
        output_jars = _run_kt_java_builder_actions(
            ctx = ctx,
            rule_kind = rule_kind,
            toolchains = toolchains,
            srcs = srcs,
            generated_src_jars = [],
            friend = friend,
            compile_deps = compile_deps,
            annotation_processors = annotation_processors,
            transitive_runtime_jars = transitive_runtime_jars,
            plugins = plugins,
            compile_jar = compile_jar
        )

    else:
        kt_java_output_jar = ctx.actions.declare_file(ctx.label.name + "-kt-java.jar")
        _run_kt_builder_action(
            ctx = ctx,
            rule_kind = rule_kind,
            toolchains = toolchains,
            srcs = srcs,
            generated_src_jars = [],
            friend = friend,
            compile_deps = compile_deps,
            annotation_processors = annotation_processors,
            transitive_runtime_jars = transitive_runtime_jars,
            plugins = plugins,
            outputs = {
                "output": kt_java_output_jar,
                "kotlin_output_jdeps": ctx.outputs.jdeps,
            },
        )
        compile_jar = kt_java_output_jar
        output_jars = [kt_java_output_jar]

    # If this rule has any resources declared setup a zipper action to turn them into a jar.
    if len(ctx.files.resources) > 0:
        output_jars.append(_build_resourcejar_action(ctx))
    output_jars.extend(ctx.files.resource_jars)

    # Merge outputs into final runtime jar.
    output_jar = ctx.actions.declare_file(ctx.label.name + ".jar")
    _fold_jars_action(
        ctx,
        rule_kind = rule_kind,
        toolchains = toolchains,
        output_jar = output_jar,
        action_type = "Runtime",
        input_jars = output_jars,
    )

    source_jar = java_common.pack_sources(
        ctx.actions,
        output_jar = output_jar,
        output_source_jar = ctx.outputs.srcjar,
        sources = ctx.files.srcs,
        source_jars = srcs.src_jars + generated_src_jars,
        java_toolchain = toolchains.java,
        host_javabase = toolchains.java_runtime)

    return struct(
        java = JavaInfo(
            output_jar = output_jar,
            compile_jar = compile_jar,
            source_jar = source_jar,
            jdeps = ctx.outputs.jdeps,
            deps = compile_deps.deps,
            runtime_deps = [_java_info(d) for d in ctx.attr.runtime_deps],
            exports = [_java_info(d) for d in getattr(ctx.attr, "exports", [])],
            neverlink = getattr(ctx.attr, "neverlink", False),
        ),
        kt = _KtJvmInfo(
            srcs = ctx.files.srcs,
            module_name = _utils.derive_module_name(ctx),
            language_version = toolchains.kt.api_version,
            exported_compiler_plugins = _collect_plugins_for_export(
                getattr(ctx.attr, "exported_compiler_plugins", []),
                getattr(ctx.attr, "exports", []),
            ),
            # intellij aspect needs this.
            outputs = struct(
                jdeps = ctx.outputs.jdeps,
                jars = [struct(
                    class_jar = output_jar,
                    ijar = compile_jar,
                    source_jars = [source_jar],
                )],
            ),
        ),
    )

"""Runs the necessary KotlinBuilder and JavaBuilder actions to compile a jar
"""
def _run_kt_java_builder_actions(ctx, rule_kind, toolchains, srcs, generated_src_jars, friend, compile_deps, annotation_processors, transitive_runtime_jars, plugins, compile_jar):
    compile_jars = []
    output_jars = []
    kt_stubs_for_java = []

    # Run KAPT
    if srcs.kt and annotation_processors:
        kapt_generated_src_jar = ctx.actions.declare_file(ctx.label.name + "-kapt-generated-src.jar")
        kapt_generated_stub_jar = ctx.actions.declare_file(ctx.label.name + "-kapt-generated-stub.jar")
        kapt_generated_class_jar = ctx.actions.declare_file(ctx.label.name + "-kapt-generated-class.jar")
        _run_kt_builder_action(
            ctx = ctx,
            rule_kind = rule_kind,
            toolchains = toolchains,
            srcs = srcs,
            generated_src_jars = [],
            friend = friend,
            compile_deps = compile_deps,
            annotation_processors = annotation_processors,
            transitive_runtime_jars = transitive_runtime_jars,
            plugins = plugins,
            outputs = {
                "generated_java_srcjar": kapt_generated_src_jar,
                "kapt_generated_stub_jar": kapt_generated_stub_jar,
                "kapt_generated_class_jar": kapt_generated_class_jar,
            },
            build_java = False,
            build_kotlin = False,
            mnemonic = "KotlinKapt",
        )
        generated_src_jars.append(kapt_generated_src_jar)
        output_jars.append(kapt_generated_class_jar)
        kt_stubs_for_java.append(JavaInfo(compile_jar=kapt_generated_stub_jar, output_jar=kapt_generated_stub_jar, neverlink = True))

    java_infos = []
    # Build Kotlin
    if srcs.kt or srcs.src_jars:
        kt_runtime_jar = ctx.actions.declare_file(ctx.label.name + "-kt.jar")
        kt_jdeps = ctx.actions.declare_file(ctx.label.name + "-kt.jdeps")
        if not "kt_abi_plugin_incompatible" in ctx.attr.tags:
            kt_compile_jar = ctx.actions.declare_file(ctx.label.name + "-kt.abi.jar")
            outputs = {
                "output": kt_runtime_jar,
                "abi_jar": kt_compile_jar,
                "kotlin_output_jdeps": kt_jdeps,
            }
        else:
            kt_compile_jar = kt_runtime_jar
            outputs = {
                "output": kt_runtime_jar,
                "kotlin_output_jdeps": kt_jdeps,
            }

        _run_kt_builder_action(
            ctx = ctx,
            rule_kind = rule_kind,
            toolchains = toolchains,
            srcs = srcs,
            generated_src_jars = generated_src_jars,
            friend = friend,
            compile_deps = compile_deps,
            annotation_processors = [],
            transitive_runtime_jars = transitive_runtime_jars,
            plugins = plugins,
            outputs = outputs,
            build_java = False,
            build_kotlin = True,
            mnemonic = "KotlinCompile",
        )

        compile_jars.append(kt_compile_jar)
        output_jars.append(kt_runtime_jar)
        if not annotation_processors:
            kt_stubs_for_java.append(JavaInfo(compile_jar=kt_compile_jar, output_jar=kt_runtime_jar, neverlink = True))

        kt_java_info = JavaInfo(
           output_jar = kt_runtime_jar,
           compile_jar = kt_compile_jar,
           jdeps = kt_jdeps,
           deps = compile_deps.deps,
           runtime_deps = [d[JavaInfo] for d in ctx.attr.runtime_deps],
           exports = [d[JavaInfo] for d in getattr(ctx.attr, "exports", [])],
           neverlink = getattr(ctx.attr, "neverlink", False),
        )
        java_infos.append(kt_java_info)

    # Build Java
    # If there is Java source or KAPT generated Java source compile that Java and fold it into
    # the final ABI jar. Otherwise just use the KT ABI jar as final ABI jar.
    if srcs.java or generated_src_jars:
        javac_opts = _javac_options_provider_to_flags(toolchains.kt.javac_options)

        # Kotlin takes care of annotation processing. Note that JavaBuilder "discovers"
        # annotation processors in `deps` also.
        if len(srcs.kt) > 0:
            javac_opts += ["-proc:none"]

        java_info = java_common.compile(
            ctx,
            source_files = srcs.java,
            source_jars = generated_src_jars,
            output = ctx.actions.declare_file(ctx.label.name + "-java.jar"),
            deps = compile_deps.deps + kt_stubs_for_java,
            java_toolchain = toolchains.java,
            javac_opts = javac_opts,
            host_javabase = toolchains.java_runtime,
            neverlink = getattr(ctx.attr, "neverlink", False)
        )
        compile_jars = compile_jars + [
            jars.ijar
            for jars in java_info.outputs.jars
        ]
        output_jars = output_jars + [
            jars.class_jar
            for jars in java_info.outputs.jars
        ]
        java_infos.append(java_info)

    # Merge ABI jars into final compile jar.
    _fold_jars_action(
        ctx,
        rule_kind = rule_kind,
        toolchains = toolchains,
        output_jar = compile_jar,
        action_type = "Abi",
        input_jars = compile_jars,
    )

    jdeps = []
    for java_info in java_infos:
        if java_info.outputs.jdeps:
            jdeps.append(java_info.outputs.jdeps);

    _run_merge_jdeps_action(
        ctx = ctx,
        rule_kind = rule_kind,
        toolchains = toolchains,
        jdeps = jdeps,
        outputs = {
            "output": ctx.outputs.jdeps,
        },
    )

    return output_jars

def export_only_providers(ctx, actions, attr, outputs):
    """_export_only_providers creates a series of forwarding providers without compilation overhead.

    Args:
        ctx: kt_compiler_ctx
        actions: invoking rule actions,
        attr: kt_compiler_attributes,
        outputs: kt_compiler_outputs
    Returns:
        kt_compiler_result
    """
    toolchains = _compiler_toolchains(ctx)

    # satisfy the outputs requirement. should never execute during normal compilation.
    actions.symlink(
        output = outputs.jar,
        target_file = toolchains.kt.empty_jar,
    )

    actions.symlink(
        output = outputs.srcjar,
        target_file = toolchains.kt.empty_jar,
    )

    actions.symlink(
        output = outputs.jdeps,
        target_file = toolchains.kt.empty_jdeps,
    )

    java = JavaInfo(
        output_jar = toolchains.kt.empty_jar,
        compile_jar = toolchains.kt.empty_jar,
        deps = [_java_info(d) for d in attr.deps + attr.plugins],
        exports = [_java_info(d) for d in getattr(attr, "exports", [])],
        neverlink = getattr(attr, "neverlink", False),
    )

    return struct(
        java = java,
        kt = _KtJvmInfo(
            module_name = _utils.derive_module_name(ctx),
            language_version = toolchains.kt.api_version,
            exported_compiler_plugins = _collect_plugins_for_export(
                getattr(attr, "exported_compiler_plugins", []),
                getattr(attr, "exports", []),
            ),
        ),
    )
