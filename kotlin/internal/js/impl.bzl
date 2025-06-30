load(
    "//kotlin/internal:defs.bzl",
    _KtJsInfo = "KtJsInfo",
    _TOOLCHAIN_TYPE = "TOOLCHAIN_TYPE",
)
load("//kotlin/internal/utils:utils.bzl", "utils")

def kt_js_import_impl(ctx):
    return [
        DefaultInfo(files = depset(ctx.files.klibs)),
        _KtJsInfo(
            klibs = depset(ctx.files.klibs),
        ),
    ]

def kt_js_library_impl(ctx):
    if ctx.attr.output_kind == "klib" and ctx.attr.sourcemap:
        fail("sourcemap can be only generated with js output_kind")

    js_file = None
    klib = None
    outputs = []
    module_name = utils.derive_module_name(ctx)
    builder_args = utils.init_args(ctx, "kt_js_library", module_name)

    klib = ctx.actions.declare_file("{}.klib".format(ctx.label.name))
    builder_args.add("--kotlin_output_js_klib", klib.path)
    outputs.append(klib)
    if ctx.attr.output_kind == "js":
        js_file = ctx.actions.declare_file("{}.js".format(ctx.label.name))
        builder_args.add("--kotlin_output_js", js_file.path)
        outputs.append(js_file)

    toolchains = ctx.toolchains[_TOOLCHAIN_TYPE]
    deps_klibs = []
    for dep in ctx.attr.deps:
        deps_klibs.append(dep[_KtJsInfo].klibs)
    libraries = depset(transitive = [ctx.attr._js_stdlib[_KtJsInfo].klibs] + deps_klibs)
    builder_args.add_all("--sources", ctx.files.srcs)
    builder_inputs, _, input_manifests = ctx.resolve_command(tools = [toolchains.kotlinbuilder, toolchains.kotlin_home])

    builder_args.add_all(
        "--kotlin_js_passthrough_flags",
        ["--target={}".format(ctx.attr.es_target)],
    )
    builder_args.add("--strict_kotlin_deps", "off")
    builder_args.add("--reduced_classpath_mode", "off")
    builder_args.add_all("--kotlin_js_libraries", libraries, omit_if_empty = False)

    ctx.actions.run(
        mnemonic = "KotlinJsCompile",
        inputs = depset(builder_inputs + ctx.files.srcs, transitive = [libraries]),
        outputs = outputs,
        executable = toolchains.kotlinbuilder.files_to_run.executable,
        tools = [
            toolchains.kotlinbuilder.files_to_run,
            toolchains.kotlin_home.files_to_run,
        ],
        execution_requirements = {"supports-workers": "1"},
        arguments = [ctx.actions.args().add_all(toolchains.builder_args), builder_args],
        progress_message = "Compiling Kotlin to JS %%{label} { kt: %d }" % len(ctx.files.srcs),
        input_manifests = input_manifests,
        env = {
            "REPOSITORY_NAME": utils.builder_workspace_name(ctx),
        },
    )

    return [
        DefaultInfo(
            files = depset(outputs),
        ),
        _KtJsInfo(
            js_file = depset([js_file]),
            klibs = depset(direct = [klib]),
        ),
    ]
