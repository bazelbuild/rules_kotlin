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
load("//kotlin/internal/utils:utils.bzl", "utils")

def kt_js_import_impl(ctx):
    klibs = []
    for dep in ctx.attr.klibs:
        klibs.append(dep[DefaultInfo].files)

    return [
        DefaultInfo(files = depset(transitive = klibs)),
        _KtJsInfo(
            klibs = klibs,
        ),
    ]

def _collect_kt_and_klib_inputs_and_args(ctx):
    klibs = []
    for dep in ctx.attr.deps:
        klibs.append(depset(direct = dep[_KtJsInfo].klibs, transitive = dep[_KtJsInfo].transitive_klibs))

    inputs = depset()

def kt_js_library_impl(ctx):
    if ctx.attr.output_kind == "klib" and ctx.attr.sourcemap:
        fail("sourcemap can be only generated with js output_kind")

    js_file = None
    source_map = None
    klib = None
    outputs = []
    module_name = utils.derive_module_name(ctx)
    builder_args = utils.init_args(ctx, "kt_js_library", module_name, js = True)

    if ctx.attr.output_kind == "js":
        js_file = ctx.actions.declare_file("{}.js".format(ctx.label.name))
        source_map = ctx.actions.declare_file("{}.js.map".format(ctx.label.name))
        builder_args.add("--kotlin_output_js", js_file.path)
        outputs.append(js_file)
    else:
        klib = ctx.actions.declare_file("{}.klib".format(ctx.label.name))
        builder_args.add("--kotlin_output_js_klib", klib.path)
        outputs.append(klib)

    toolchain = ctx.toolchains[_TOOLCHAIN_TYPE]

    libraries = depset([d[_KtJsInfo].klib for d in ctx.attr.deps])

    builder_args.add_all("--sources", ctx.files.srcs)
    inputs, _, input_manifests = ctx.resolve_command(tools = [toolchain.kotlinbuilder, toolchain.kotlin_home])

    kotlinc_options = toolchain.kotlinc_options

    builder_args.add_all(
        "--kotlin_js_passthrough_flags",
        kotlinc_options_to_flags(kotlinc_options),
    )
    ctx.actions.run(
        mnemonic = "KotlinCompile",
        inputs = depset(inputs + ctx.files.srcs, transitive = [libraries]),
        outputs = outputs,
        executable = toolchain.kotlinbuilder.files_to_run.executable,
        execution_requirements = {"supports-workers": "1"},
        arguments = [builder_args],
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
            js_file = js_file,
            klibs = [klib],
        ),
    ]
