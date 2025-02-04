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
    "@rules_java//java:defs.bzl",
    "JavaInfo",
    "java_common",
)
load(
    "//kotlin/internal:defs.bzl",
    _JAVA_RUNTIME_TOOLCHAIN_TYPE = "JAVA_RUNTIME_TOOLCHAIN_TYPE",
    _JAVA_TOOLCHAIN_TYPE = "JAVA_TOOLCHAIN_TYPE",
    _KtCompilerPluginInfo = "KtCompilerPluginInfo",
    _KtJvmInfo = "KtJvmInfo",
    _KtPluginConfiguration = "KtPluginConfiguration",
    _TOOLCHAIN_TYPE = "TOOLCHAIN_TYPE",
)
load(
    "//kotlin/internal:opts.bzl",
    "JavacOptions",
    "KotlincOptions",
    "javac_options_to_flags",
    "kotlinc_options_to_flags",
)
load(
    "//kotlin/internal/jvm:associates.bzl",
    _associate_utils = "associate_utils",
)
load(
    "//kotlin/internal/jvm:plugins.bzl",
    "is_ksp_processor_generating_java",
    _plugin_mappers = "mappers",
)
load(
    "//kotlin/internal/utils:sets.bzl",
    _sets = "sets",
)
load(
    "//kotlin/internal/utils:utils.bzl",
    _utils = "utils",
)

# UTILITY ##############################################################################################################
def find_java_toolchain(ctx, target):
    if _JAVA_TOOLCHAIN_TYPE in ctx.toolchains:
        return ctx.toolchains[_JAVA_TOOLCHAIN_TYPE].java
    return target[java_common.JavaToolchainInfo]

def find_java_runtime_toolchain(ctx, target):
    if _JAVA_RUNTIME_TOOLCHAIN_TYPE in ctx.toolchains:
        return ctx.toolchains[_JAVA_RUNTIME_TOOLCHAIN_TYPE].java_runtime
    return target[java_common.JavaRuntimeInfo]

def _java_info(target):
    return target[JavaInfo] if JavaInfo in target else None

def _deps_artifacts(toolchains, targets):
    """Collect Jdeps artifacts if required."""
    if not toolchains.kt.experimental_report_unused_deps == "off":
        deps_artifacts = [t[JavaInfo].outputs.jdeps for t in targets if JavaInfo in t and t[JavaInfo].outputs.jdeps]
    else:
        deps_artifacts = []

    return depset(deps_artifacts)

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

def _jvm_deps(ctx, toolchains, associate_deps, deps, exports = [], runtime_deps = []):
    """Encapsulates jvm dependency metadata."""
    diff = _sets.intersection(
        _sets.copy_of([x.label for x in associate_deps]),
        _sets.copy_of([x.label for x in deps]),
    )
    if diff:
        fail(
            "\n------\nTargets should only be put in associates= or deps=, not both:\n%s" %
            ",\n ".join(["    %s" % x for x in list(diff)]),
        )
    dep_infos = [_java_info(d) for d in associate_deps + deps] + [toolchains.kt.jvm_stdlibs]

    associates = _associate_utils.get_associates(ctx, associates = associate_deps)

    # Reduced classpath, exclude transitive deps from compilation
    if (toolchains.kt.experimental_prune_transitive_deps and
        not "kt_experimental_prune_transitive_deps_incompatible" in ctx.attr.tags):
        transitive = [
            d.compile_jars
            for d in dep_infos
        ]
    else:
        transitive = [
            d.compile_jars
            for d in dep_infos
        ] + [
            d.transitive_compile_time_jars
            for d in dep_infos
        ]

    return struct(
        module_name = associates.module_name,
        deps = dep_infos,
        exports = [_java_info(d) for d in exports],
        associate_jars = associates.jars,
        compile_jars = depset(transitive = transitive),
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
    "src/main/java",
    "src/main/resources",
    "src/test/java",
    "src/test/resources",
    "kotlin",
]

def _adjust_resources_path_by_strip_prefix(path, resource_strip_prefix):
    if not path.startswith(resource_strip_prefix):
        fail("Resource file %s is not under the specified prefix to strip" % path)

    clean_path = path[len(resource_strip_prefix):]
    return clean_path

def _adjust_resources_path_by_default_prefixes(path):
    for cp in _CONVENTIONAL_RESOURCE_PATHS:
        _, _, rel_path = path.partition(cp)
        if rel_path:
            return rel_path
    return path

def _adjust_resources_path(path, resource_strip_prefix):
    if resource_strip_prefix:
        return _adjust_resources_path_by_strip_prefix(path, resource_strip_prefix)
    else:
        return _adjust_resources_path_by_default_prefixes(path)

def _format_compile_plugin_options(o):
    """Format compiler option into id:value for cmd line."""
    return [
        "%s:%s" % (o.id, o.value),
    ]

def _new_plugins_from(targets):
    """Returns a struct containing the plugin metadata for the given targets.

    Args:
        targets: A list of targets.
    Returns:
        A struct containing the plugins for the given targets in the format:
        {
            stubs_phase = {
                classpath = depset,
                options= List[KtCompilerPluginOption],
            ),
            compile = {
                classpath = depset,
                options = List[KtCompilerPluginOption],
            },
        }
    """

    all_plugins = {}
    plugins_without_phase = []
    for t in targets:
        if _KtCompilerPluginInfo not in t:
            continue
        plugin = t[_KtCompilerPluginInfo]
        if not (plugin.stubs or plugin.compile):
            plugins_without_phase.append("%s: %s" % (t.label, plugin.id))
        if plugin.id in all_plugins:
            # This need a more robust error messaging.
            fail("has multiple plugins with the same id: %s." % plugin.id)
        all_plugins[plugin.id] = plugin

    if plugins_without_phase:
        fail("has plugin without a phase defined: %s" % cfgs_without_plugin)

    all_plugin_cfgs = {}
    cfgs_without_plugin = []
    for t in targets:
        if _KtPluginConfiguration not in t:
            continue
        cfg = t[_KtPluginConfiguration]
        if cfg.id not in all_plugins:
            cfgs_without_plugin.append("%s: %s" % (t.label, cfg.id))
        all_plugin_cfgs[cfg.id] = cfg

    if cfgs_without_plugin:
        fail("has plugin configurations without corresponding plugins: %s" % cfgs_without_plugin)

    return struct(
        stubs_phase = _new_plugin_from(all_plugin_cfgs, [p for p in all_plugins.values() if p.stubs]),
        compile_phase = _new_plugin_from(all_plugin_cfgs, [p for p in all_plugins.values() if p.compile]),
    )

def _new_plugin_from(all_cfgs, plugins_for_phase):
    classpath = []
    data = []
    options = []
    for p in plugins_for_phase:
        classpath.append(p.classpath)
        options.extend(p.options)
        if p.id in all_cfgs:
            cfg = all_cfgs[p.id]
            classpath.append(cfg.classpath)
            data.append(cfg.data)
            options.extend(cfg.options)

    return struct(
        classpath = depset(transitive = classpath),
        data = depset(transitive = data),
        options = options,
    )

# INTERNAL ACTIONS #####################################################################################################
def _fold_jars_action(ctx, rule_kind, toolchains, output_jar, input_jars, action_type = ""):
    """Set up an action to Fold the input jars into a normalized output jar."""
    args = ctx.actions.args()
    args.add_all([
        "--normalize",
        "--compression",
        "--exclude_build_data",
        "--add_missing_directories",
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
        progress_message = "Merging Kotlin output jar %%{label}%s from %d inputs" % (
            "" if not action_type else " (%s)" % action_type,
            len(input_jars),
        ),
        toolchain = _TOOLCHAIN_TYPE,
    )

def _resourcejar_args_action(ctx):
    res_cmd = []
    for f in ctx.files.resources:
        target_path = _adjust_resources_path(f.short_path, ctx.attr.resource_strip_prefix)
        if target_path[0] == "/":
            target_path = target_path[1:]
        line = "{target_path}={f_path}\n".format(
            target_path = target_path,
            f_path = f.path,
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
        progress_message = "Creating intermediate resource jar %{label}",
    )
    return resources_jar_output

def _run_merge_jdeps_action(ctx, toolchains, jdeps, outputs, deps):
    """Creates a Jdeps merger action invocation."""
    args = ctx.actions.args()
    args.set_param_file_format("multiline")
    args.use_param_file("--flagfile=%s", use_always = True)

    args.add("--target_label", ctx.label)

    for f, path in outputs.items():
        args.add("--" + f, path)

    args.add_all("--inputs", jdeps, omit_if_empty = True)
    args.add("--report_unused_deps", toolchains.kt.experimental_report_unused_deps)

    mnemonic = "JdepsMerge"
    progress_message = "%s %%{label} { jdeps: %d }" % (
        mnemonic,
        len(jdeps),
    )

    inputs = depset(jdeps)
    if not toolchains.kt.experimental_report_unused_deps == "off":
        # For sandboxing to work, and for this action to be deterministic, the compile jars need to be passed as inputs
        inputs = depset(jdeps, transitive = [depset([], transitive = [dep.transitive_compile_time_jars for dep in deps])])

    ctx.actions.run(
        mnemonic = mnemonic,
        inputs = inputs,
        tools = [toolchains.kt.jdeps_merger.files_to_run, toolchains.kt.jvm_stdlibs.compile_jars],
        outputs = [f for f in outputs.values()],
        executable = toolchains.kt.jdeps_merger.files_to_run.executable,
        execution_requirements = toolchains.kt.execution_requirements,
        arguments = [
            ctx.actions.args().add_all(toolchains.kt.builder_args),
            args,
        ],
        progress_message = progress_message,
        toolchain = _TOOLCHAIN_TYPE,
    )

def _run_kapt_builder_actions(
        ctx,
        rule_kind,
        toolchains,
        srcs,
        compile_deps,
        deps_artifacts,
        annotation_processors,
        transitive_runtime_jars,
        plugins):
    """Runs KAPT using the KotlinBuilder tool
    Returns:
        A struct containing KAPT outputs
    """
    ap_generated_src_jar = ctx.actions.declare_file(ctx.label.name + "-kapt-gensrc.jar")
    kapt_generated_stub_jar = ctx.actions.declare_file(ctx.label.name + "-kapt-generated-stub.jar")
    kapt_generated_class_jar = ctx.actions.declare_file(ctx.label.name + "-kapt-generated-class.jar")

    _run_kt_builder_action(
        ctx = ctx,
        rule_kind = rule_kind,
        toolchains = toolchains,
        srcs = srcs,
        generated_src_jars = [],
        compile_deps = compile_deps,
        deps_artifacts = deps_artifacts,
        annotation_processors = annotation_processors,
        transitive_runtime_jars = transitive_runtime_jars,
        plugins = plugins,
        outputs = {
            "generated_java_srcjar": ap_generated_src_jar,
            "kapt_generated_stub_jar": kapt_generated_stub_jar,
            "kapt_generated_class_jar": kapt_generated_class_jar,
        },
        build_kotlin = False,
        mnemonic = "KotlinKapt",
    )

    return struct(
        ap_generated_src_jar = ap_generated_src_jar,
        kapt_generated_stub_jar = kapt_generated_stub_jar,
        kapt_generated_class_jar = kapt_generated_class_jar,
    )

def _run_ksp_builder_actions(
        ctx,
        rule_kind,
        toolchains,
        srcs,
        compile_deps,
        deps_artifacts,
        annotation_processors,
        transitive_runtime_jars,
        plugins):
    """Runs KSP using the KotlinBuilder tool

    Returns:
        A struct containing KSP outputs
    """
    ksp_generated_java_srcjar = ctx.actions.declare_file(ctx.label.name + "-ksp-kt-gensrc.jar")

    _run_kt_builder_action(
        ctx = ctx,
        rule_kind = rule_kind,
        toolchains = toolchains,
        srcs = srcs,
        generated_src_jars = [],
        compile_deps = compile_deps,
        deps_artifacts = deps_artifacts,
        annotation_processors = annotation_processors,
        transitive_runtime_jars = transitive_runtime_jars,
        plugins = plugins,
        outputs = {
            "ksp_generated_java_srcjar": ksp_generated_java_srcjar,
        },
        build_kotlin = False,
        mnemonic = "KotlinKsp",
    )

    return struct(ksp_generated_class_jar = ksp_generated_java_srcjar)

def _run_kt_builder_action(
        ctx,
        mnemonic,
        rule_kind,
        toolchains,
        srcs,
        generated_src_jars,
        compile_deps,
        deps_artifacts,
        annotation_processors,
        transitive_runtime_jars,
        plugins,
        outputs,
        build_kotlin = True):
    """Creates a KotlinBuilder action invocation."""
    if not mnemonic:
        fail("Error: A `mnemonic` must be provided for every invocation of `_run_kt_builder_action`!")

    kotlinc_options = ctx.attr.kotlinc_opts[KotlincOptions] if ctx.attr.kotlinc_opts else toolchains.kt.kotlinc_options
    javac_options = ctx.attr.javac_opts[JavacOptions] if ctx.attr.javac_opts else toolchains.kt.javac_options

    args = _utils.init_args(ctx, rule_kind, compile_deps.module_name, kotlinc_options)

    for f, path in outputs.items():
        args.add("--" + f, path)

    # Unwrap kotlinc_options/javac_options options or default to the ones being provided by the toolchain
    args.add_all("--kotlin_passthrough_flags", kotlinc_options_to_flags(kotlinc_options))
    args.add_all("--javacopts", javac_options_to_flags(javac_options))
    args.add_all("--direct_dependencies", _java_infos_to_compile_jars(compile_deps.deps))
    args.add("--strict_kotlin_deps", toolchains.kt.experimental_strict_kotlin_deps)
    args.add_all("--classpath", compile_deps.compile_jars)
    args.add("--reduced_classpath_mode", toolchains.kt.experimental_reduce_classpath_mode)
    args.add_all("--sources", srcs.all_srcs, omit_if_empty = True)
    args.add_all("--source_jars", srcs.src_jars + generated_src_jars, omit_if_empty = True)
    args.add_all("--deps_artifacts", deps_artifacts, omit_if_empty = True)
    args.add_all("--kotlin_friend_paths", compile_deps.associate_jars, omit_if_empty = True)
    args.add("--instrument_coverage", ctx.coverage_instrumented())

    # Collect and prepare plugin descriptor for the worker.
    args.add_all(
        "--processors",
        annotation_processors,
        map_each = _plugin_mappers.kt_plugin_to_processor,
        omit_if_empty = True,
        uniquify = True,
    )

    args.add_all(
        "--processorpath",
        annotation_processors,
        map_each = _plugin_mappers.kt_plugin_to_processorpath,
        omit_if_empty = True,
        uniquify = True,
    )

    args.add_all(
        "--stubs_plugin_classpath",
        plugins.stubs_phase.classpath,
        omit_if_empty = True,
    )

    args.add_all(
        "--stubs_plugin_options",
        plugins.stubs_phase.options,
        map_each = _format_compile_plugin_options,
        omit_if_empty = True,
    )

    args.add_all(
        "--compiler_plugin_classpath",
        plugins.compile_phase.classpath,
        omit_if_empty = True,
    )

    args.add_all(
        "--compiler_plugin_options",
        plugins.compile_phase.options,
        map_each = _format_compile_plugin_options,
        omit_if_empty = True,
    )

    if not "kt_remove_private_classes_in_abi_plugin_incompatible" in ctx.attr.tags and toolchains.kt.experimental_remove_private_classes_in_abi_jars == True:
        args.add("--remove_private_classes_in_abi_jar", "true")

    if not "kt_treat_internal_as_private_in_abi_plugin_incompatible" in ctx.attr.tags and toolchains.kt.experimental_treat_internal_as_private_in_abi_jars == True:
        if not "kt_remove_private_classes_in_abi_plugin_incompatible" in ctx.attr.tags and toolchains.kt.experimental_remove_private_classes_in_abi_jars == True:
            args.add("--treat_internal_as_private_in_abi_jar", "true")
        else:
            fail(
                "experimental_treat_internal_as_private_in_abi_jars without experimental_remove_private_classes_in_abi_jars is invalid." +
                "\nTo remove internal symbols from kotlin abi jars ensure experimental_remove_private_classes_in_abi_jars " +
                "and experimental_treat_internal_as_private_in_abi_jars are both enabled in define_kt_toolchain." +
                "\nAdditionally ensure the target does not contain the kt_remove_private_classes_in_abi_plugin_incompatible tag.",
            )

    args.add("--build_kotlin", build_kotlin)

    progress_message = "%s %%{label} { kt: %d, java: %d, srcjars: %d } for %s" % (
        mnemonic,
        len(srcs.kt),
        len(srcs.java),
        len(srcs.src_jars),
        ctx.var.get("TARGET_CPU", "UNKNOWN CPU"),
    )

    ctx.actions.run(
        mnemonic = mnemonic,
        inputs = depset(
            srcs.all_srcs + srcs.src_jars + generated_src_jars,
            transitive = [
                compile_deps.associate_jars,
                compile_deps.compile_jars,
                transitive_runtime_jars,
                deps_artifacts,
                plugins.stubs_phase.classpath,
                plugins.compile_phase.classpath,
            ],
        ),
        tools = [
            toolchains.kt.kotlinbuilder.files_to_run,
            toolchains.kt.kotlin_home.files_to_run,
        ],
        outputs = [f for f in outputs.values()],
        executable = toolchains.kt.kotlinbuilder.files_to_run.executable,
        execution_requirements = _utils.add_dicts(
            toolchains.kt.execution_requirements,
            {"worker-key-mnemonic": mnemonic},
        ),
        arguments = [ctx.actions.args().add_all(toolchains.kt.builder_args), args],
        progress_message = progress_message,
        env = {
            "LC_CTYPE": "en_US.UTF-8",  # For Java source files
            "REPOSITORY_NAME": _utils.builder_workspace_name(ctx),
        },
        toolchain = _TOOLCHAIN_TYPE,
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
    compile_deps = _jvm_deps(
        ctx,
        toolchains = toolchains,
        associate_deps = ctx.attr.associates,
        deps = ctx.attr.deps,
        exports = getattr(ctx.attr, "exports", []),
        runtime_deps = getattr(ctx.attr, "runtime_deps", []),
    )

    annotation_processors = _plugin_mappers.targets_to_annotation_processors(ctx.attr.plugins + ctx.attr.deps)
    ksp_annotation_processors = _plugin_mappers.targets_to_ksp_annotation_processors(ctx.attr.plugins + ctx.attr.deps)
    transitive_runtime_jars = _plugin_mappers.targets_to_transitive_runtime_jars(ctx.attr.plugins + ctx.attr.deps)
    plugins = _new_plugins_from(ctx.attr.plugins + _exported_plugins(deps = ctx.attr.deps))

    deps_artifacts = _deps_artifacts(toolchains, ctx.attr.deps + ctx.attr.associates)

    generated_src_jars = []
    annotation_processing = None
    compile_jar = ctx.actions.declare_file(ctx.label.name + ".abi.jar")
    output_jdeps = None
    if toolchains.kt.jvm_emit_jdeps:
        output_jdeps = ctx.actions.declare_file(ctx.label.name + ".jdeps")

    outputs_struct = _run_kt_java_builder_actions(
        ctx = ctx,
        rule_kind = rule_kind,
        toolchains = toolchains,
        srcs = srcs,
        generated_kapt_src_jars = [],
        generated_ksp_src_jars = [],
        compile_deps = compile_deps,
        deps_artifacts = deps_artifacts,
        annotation_processors = annotation_processors,
        ksp_annotation_processors = ksp_annotation_processors,
        transitive_runtime_jars = transitive_runtime_jars,
        plugins = plugins,
        compile_jar = compile_jar,
        output_jdeps = output_jdeps,
    )
    output_jars = outputs_struct.output_jars
    generated_src_jars = outputs_struct.generated_src_jars
    annotation_processing = outputs_struct.annotation_processing

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
        output_source_jar = ctx.outputs.srcjar,
        sources = srcs.kt + srcs.java,
        source_jars = srcs.src_jars + generated_src_jars,
        java_toolchain = toolchains.java,
    )

    generated_source_jar = java_common.pack_sources(
        ctx.actions,
        output_source_jar = ctx.actions.declare_file(ctx.label.name + "-gensrc.jar"),
        source_jars = generated_src_jars,
        java_toolchain = toolchains.java,
    ) if generated_src_jars else None

    java_info = JavaInfo(
        output_jar = output_jar,
        compile_jar = compile_jar,
        source_jar = source_jar,
        jdeps = output_jdeps,
        deps = compile_deps.deps,
        runtime_deps = compile_deps.runtime_deps,
        exports = compile_deps.exports,
        neverlink = getattr(ctx.attr, "neverlink", False),
        generated_source_jar = generated_source_jar,
    )

    instrumented_files = coverage_common.instrumented_files_info(
        ctx,
        source_attributes = ["srcs"],
        dependency_attributes = ["deps", "exports", "associates"],
        extensions = ["kt", "java"],
    )

    return struct(
        java = java_info,
        instrumented_files = instrumented_files,
        kt = _KtJvmInfo(
            srcs = ctx.files.srcs,
            module_name = compile_deps.module_name,
            module_jars = compile_deps.associate_jars,
            language_version = toolchains.kt.api_version,
            exported_compiler_plugins = _collect_plugins_for_export(
                getattr(ctx.attr, "exported_compiler_plugins", []),
                getattr(ctx.attr, "exports", []),
            ),
            # intellij aspect needs this.
            outputs = struct(
                jdeps = output_jdeps,
                jars = [struct(
                    class_jar = output_jar,
                    ijar = compile_jar,
                    source_jars = [source_jar],
                )],
            ),
            transitive_compile_time_jars = java_info.transitive_compile_time_jars,
            transitive_source_jars = java_info.transitive_source_jars,
            annotation_processing = annotation_processing,
            additional_generated_source_jars = generated_src_jars,
            all_output_jars = output_jars,
        ),
    )

def _run_kt_java_builder_actions(
        ctx,
        rule_kind,
        toolchains,
        srcs,
        generated_kapt_src_jars,
        generated_ksp_src_jars,
        compile_deps,
        deps_artifacts,
        annotation_processors,
        ksp_annotation_processors,
        transitive_runtime_jars,
        plugins,
        compile_jar,
        output_jdeps):
    """Runs the necessary KotlinBuilder and JavaBuilder actions to compile a jar

    Returns:
        A struct containing the a list of output_jars and a struct annotation_processing jars
    """
    compile_jars = []
    output_jars = []
    kt_stubs_for_java = []
    has_kt_sources = srcs.kt or srcs.src_jars

    # Run KAPT
    if has_kt_sources and annotation_processors:
        kapt_outputs = _run_kapt_builder_actions(
            ctx,
            rule_kind = rule_kind,
            toolchains = toolchains,
            srcs = srcs,
            compile_deps = compile_deps,
            deps_artifacts = deps_artifacts,
            annotation_processors = annotation_processors,
            transitive_runtime_jars = transitive_runtime_jars,
            plugins = plugins,
        )
        generated_kapt_src_jars.append(kapt_outputs.ap_generated_src_jar)
        output_jars.append(kapt_outputs.kapt_generated_class_jar)
        kt_stubs_for_java.append(
            JavaInfo(
                compile_jar = kapt_outputs.kapt_generated_stub_jar,
                output_jar = kapt_outputs.kapt_generated_stub_jar,
                neverlink = True,
            ),
        )

    # Run KSP
    if has_kt_sources and ksp_annotation_processors:
        ksp_outputs = _run_ksp_builder_actions(
            ctx,
            rule_kind = rule_kind,
            toolchains = toolchains,
            srcs = srcs,
            compile_deps = compile_deps,
            deps_artifacts = deps_artifacts,
            annotation_processors = ksp_annotation_processors,
            transitive_runtime_jars = transitive_runtime_jars,
            plugins = plugins,
        )
        generated_ksp_src_jars.append(ksp_outputs.ksp_generated_class_jar)

    java_infos = []

    # Build Kotlin
    if has_kt_sources:
        kt_runtime_jar = ctx.actions.declare_file(ctx.label.name + "-kt.jar")
        if not "kt_abi_plugin_incompatible" in ctx.attr.tags and toolchains.kt.experimental_use_abi_jars == True:
            kt_compile_jar = ctx.actions.declare_file(ctx.label.name + "-kt.abi.jar")
            outputs = {
                "output": kt_runtime_jar,
                "abi_jar": kt_compile_jar,
            }
        else:
            kt_compile_jar = kt_runtime_jar
            outputs = {
                "output": kt_runtime_jar,
            }

        kt_jdeps = None
        if toolchains.kt.jvm_emit_jdeps:
            kt_jdeps = ctx.actions.declare_file(ctx.label.name + "-kt.jdeps")
            outputs["kotlin_output_jdeps"] = kt_jdeps

        _run_kt_builder_action(
            ctx = ctx,
            rule_kind = rule_kind,
            toolchains = toolchains,
            srcs = srcs,
            generated_src_jars = generated_kapt_src_jars + generated_ksp_src_jars,
            compile_deps = compile_deps,
            deps_artifacts = deps_artifacts,
            annotation_processors = [],
            transitive_runtime_jars = transitive_runtime_jars,
            plugins = plugins,
            outputs = outputs,
            build_kotlin = True,
            mnemonic = "KotlinCompile",
        )

        compile_jars.append(kt_compile_jar)
        output_jars.append(kt_runtime_jar)
        if not annotation_processors or not srcs.kt:
            kt_stubs_for_java.append(JavaInfo(compile_jar = kt_compile_jar, output_jar = kt_runtime_jar, neverlink = True))

        kt_java_info = JavaInfo(
            output_jar = kt_runtime_jar,
            compile_jar = kt_compile_jar,
            jdeps = kt_jdeps,
            deps = compile_deps.deps,
            runtime_deps = compile_deps.runtime_deps,
            exports = compile_deps.exports,
            neverlink = getattr(ctx.attr, "neverlink", False),
        )
        java_infos.append(kt_java_info)

    # Build Java
    # If there is Java source or KAPT/KSP generated Java source compile that Java and fold it into
    # the final ABI jar. Otherwise just use the KT ABI jar as final ABI jar.
    ksp_generated_java_src_jars = generated_ksp_src_jars and is_ksp_processor_generating_java(ctx.attr.plugins)
    if srcs.java or generated_kapt_src_jars or srcs.src_jars or ksp_generated_java_src_jars:
        javac_opts = javac_options_to_flags(ctx.attr.javac_opts[JavacOptions] if ctx.attr.javac_opts else toolchains.kt.javac_options)

        # Kotlin takes care of annotation processing. Note that JavaBuilder "discovers"
        # annotation processors in `deps` also.
        if len(srcs.kt) > 0:
            javac_opts.append("-proc:none")
        java_info = java_common.compile(
            ctx,
            source_files = srcs.java,
            source_jars = generated_kapt_src_jars + srcs.src_jars + generated_ksp_src_jars,
            output = ctx.actions.declare_file(ctx.label.name + "-java.jar"),
            deps = compile_deps.deps + kt_stubs_for_java,
            java_toolchain = toolchains.java,
            plugins = _plugin_mappers.targets_to_annotation_processors_java_plugin_info(ctx.attr.plugins),
            javac_opts = javac_opts,
            neverlink = getattr(ctx.attr, "neverlink", False),
            strict_deps = toolchains.kt.experimental_strict_kotlin_deps,
        )
        ap_generated_src_jar = java_info.annotation_processing.source_jar
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

    if toolchains.kt.jvm_emit_jdeps:
        jdeps = []
        for java_info in java_infos:
            if java_info.outputs.jdeps:
                jdeps.append(java_info.outputs.jdeps)

        if jdeps:
            _run_merge_jdeps_action(
                ctx = ctx,
                toolchains = toolchains,
                jdeps = jdeps,
                deps = compile_deps.deps,
                outputs = {"output": output_jdeps},
            )
        else:
            ctx.actions.symlink(
                output = output_jdeps,
                target_file = toolchains.kt.empty_jdeps,
            )

    annotation_processing = None
    if annotation_processors:
        outputs_list = [java_info.outputs for java_info in java_infos]
        annotation_processing = _create_annotation_processing(
            annotation_processors = annotation_processors,
            ap_class_jar = [jars.class_jar for outputs in outputs_list for jars in outputs.jars][0],
            ap_source_jar = ap_generated_src_jar,
        )

    return struct(
        output_jars = output_jars,
        generated_src_jars = generated_kapt_src_jars + generated_ksp_src_jars,
        annotation_processing = annotation_processing,
    )

def _create_annotation_processing(annotation_processors, ap_class_jar, ap_source_jar):
    """Creates the annotation_processing field for Kt to match what JavaInfo

    The Bazel Plugin IDE logic is based on this assumption in order to locate the Annotation
    Processor generated source code.

    See https://docs.bazel.build/versions/master/skylark/lib/JavaInfo.html#annotation_processing
    """
    if annotation_processors:
        return struct(
            enabled = True,
            class_jar = ap_class_jar,
            source_jar = ap_source_jar,
        )
    return None

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

    output_jdeps = None
    if toolchains.kt.jvm_emit_jdeps:
        output_jdeps = ctx.actions.declare_file(ctx.label.name + ".jdeps")
        actions.symlink(
            output = output_jdeps,
            target_file = toolchains.kt.empty_jdeps,
        )

    java = JavaInfo(
        output_jar = toolchains.kt.empty_jar,
        compile_jar = toolchains.kt.empty_jar,
        deps = [_java_info(d) for d in attr.deps],
        exports = [_java_info(d) for d in getattr(attr, "exports", [])],
        neverlink = getattr(attr, "neverlink", False),
        jdeps = output_jdeps,
    )

    return struct(
        java = java,
        kt = _KtJvmInfo(
            module_name = _utils.derive_module_name(ctx),
            module_jars = [],
            language_version = toolchains.kt.api_version,
            exported_compiler_plugins = _collect_plugins_for_export(
                getattr(attr, "exported_compiler_plugins", []),
                getattr(attr, "exports", []),
            ),
        ),
        instrumented_files = coverage_common.instrumented_files_info(
            ctx,
            source_attributes = ["srcs"],
            dependency_attributes = ["deps", "exports", "associates"],
            extensions = ["kt", "java"],
        ),
    )
