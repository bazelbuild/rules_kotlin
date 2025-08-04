load("//kotlin/internal:defs.bzl", _KtKlibInfo = "KtKlibInfo", _NATIVE_TOOLCHAIN_TYPE = "NATIVE_TOOLCHAIN_TYPE", _TOOLCHAIN_TYPE = "TOOLCHAIN_TYPE")
load("//kotlin/internal/utils:utils.bzl", "utils")

def _kt_klib_library(ctx):
    module_name = utils.derive_module_name(ctx)
    builder_args = utils.init_args(ctx, "kt_klib_library", module_name)

    klib = ctx.actions.declare_file("{}.klib".format(ctx.label.name))
    outputs = [klib]

    toolchains = ctx.toolchains[_TOOLCHAIN_TYPE]

    # Retrieve konan.home from the chosen toolchain's distribution
    native_toolchain_info = ctx.toolchains[_NATIVE_TOOLCHAIN_TYPE].kotlin_native_info
    konan_home = native_toolchain_info.konan_home

    deps_klibs = []
    transitive_klibs = []
    for dep in ctx.attr.deps:
        deps_klibs.append(dep[_KtKlibInfo].klibs)
        deps_klibs.append(dep[_KtKlibInfo].transitive_klibs)
        transitive_klibs.append(dep[_KtKlibInfo].transitive_klibs)

    libraries = depset(transitive = deps_klibs)
    builder_args.add_all("--sources", ctx.files.srcs)
    builder_inputs, _, input_manifests = ctx.resolve_command(tools = [toolchains.kotlinbuilder])

    builder_args.add("--strict_kotlin_deps", "off")
    builder_args.add("--reduced_classpath_mode", "off")
    builder_args.add("--output_klib", klib.path)

    libraries = depset(transitive = deps_klibs)
    builder_args.add_all("--klibs", libraries, omit_if_empty = False)
    builder_args.add("--konan_home", konan_home.path)

    ctx.actions.run(
        mnemonic = "KotlinKlibCompile",
        inputs = depset(builder_inputs + ctx.files.srcs, transitive = [libraries, native_toolchain_info.konan_home_files]),
        outputs = outputs,
        executable = toolchains.kotlinbuilder.files_to_run.executable,
        tools = [
            toolchains.kotlinbuilder.files_to_run,
        ],
        execution_requirements = {"supports-workers": "1"},
        arguments = [ctx.actions.args().add_all(toolchains.builder_args), builder_args],
        progress_message = "Compiling Kotlin to Klib %%{label} { kt: %d }" % len(ctx.files.srcs),
        input_manifests = input_manifests,
        env = {
            "REPOSITORY_NAME": utils.builder_workspace_name(ctx),
        },
    )

    return [
        DefaultInfo(files = depset(outputs)),
        _KtKlibInfo(
            klibs = depset(outputs),
            transitive_klibs = depset(transitive = transitive_klibs),
        ),
    ]

kt_klib_library = rule(
    implementation = _kt_klib_library,
    doc = """
This rule is intended to leverage the new Kotlin IR backend to allow for compiling platform-independent Kotlin code
to be shared between Kotlin code for different platforms (JS/JVM/WASM etc.). It produces a klib file as the output.
    """,
    attrs = {
        "srcs": attr.label_list(
            doc = "A list of source files to be compiled to klib",
            allow_files = [".kt"],
        ),
        "deps": attr.label_list(
            doc = "A list of other kt_klib_library targets that this library depends on for compilation",
            providers = [_KtKlibInfo],
        ),
    },
    toolchains = [_TOOLCHAIN_TYPE, _NATIVE_TOOLCHAIN_TYPE],
    provides = [_KtKlibInfo],
)
