load("//src/main/starlark/core/compile:common.bzl", "JAVA_RUNTIME_TOOLCHAIN_TYPE", "JAVA_TOOLCHAIN_TYPE")
load(
    "//src/main/starlark/core/options:opts.kotlinc.bzl",
    "KotlincOptions",
)
load(":compile.bzl", "build_deploy_jar", "compile_kotlin_for_jvm", "write_jvm_launcher")

KotlincJvmCompileInfo = provider(
    doc = "Necessary compilation info for compiling kotlin for the jvm",
    fields = {
        "jvm_target": "java target byte compatiblity.",
        "api_version": "kotlin api version.",
        "language_version": "Kotlin source version.",
        "compile_mnemonic": "Mnemonic for the compile action(s).",
        "executable_zip": "Executable for creating zip files.",
        "kotlinc": "kotlinc executable",
        "single_jar": "single jar executable",
        "java_stub_template": "Launcher template for running kotlin jars",
        "java_runtime": "Current kotlin java runtime",
        "kotlin_stdlib": "Koltin standard libs",
    },
)

def _cli_toolchain(ctx):
    java_runtime = ctx.toolchains[JAVA_RUNTIME_TOOLCHAIN_TYPE].java_runtime
    java_toolchain = ctx.toolchains[JAVA_TOOLCHAIN_TYPE].java

    toolchain_info = KotlincJvmCompileInfo(
        jvm_target = getattr(java_runtime, "version", 11),  # default to 11 on old bazel. If it even works.
        api_version = ".".join(ctx.attr.api_version.split(".")[:2]),
        language_version = ".".join(ctx.attr.api_version.split(".")[:2]),
        executable_zip = ctx.attr.zip[DefaultInfo].files_to_run,
        kotlinc = ctx.attr.kotlinc[DefaultInfo].files_to_run,
        compile_mnemonic = "CliKotlinc",
        single_jar = java_toolchain.single_jar,
        java_stub_template = ctx.files.java_stub_template[0],
        java_runtime = java_runtime,
        kotlin_stdlib = java_common.merge([j[JavaInfo] for j in ctx.attr.kotlin_stdlibs]),
    )
    return [
        platform_common.ToolchainInfo(
            compile = _partial(compile_kotlin_for_jvm, toolchain_info = toolchain_info),
            launch = _partial(write_jvm_launcher, toolchain_info = toolchain_info),
            deploy = _partial(build_deploy_jar, toolchain_info = toolchain_info),
        ),
    ]

cli_toolchain = rule(
    doc = """The kotlin toolchain. This should not be created directly `define_kt_toolchain` should be used. The
  rules themselves define the toolchain using that macro.""",
    implementation = _cli_toolchain,
    attrs = {
        "language_version": attr.string(
            doc = "this is the -language_version flag [see](https://kotlinlang.org/docs/reference/compatibility.html)",
        ),
        "api_version": attr.string(
            doc = "this is the -api_version flag [see](https://kotlinlang.org/docs/reference/compatibility.html).",
        ),
        "zip": attr.label(
            executable = True,
            cfg = "exec",
        ),
        "kotlinc": attr.label(
            executable = True,
            cfg = "exec",
        ),
        "kotlin_stdlibs": attr.label_list(
            cfg = "exec",
            providers = [JavaInfo],
        ),
        "java_stub_template": attr.label(
            cfg = "exec",
            default = Label("@bazel_tools//tools/java:java_stub_template.txt"),
            allow_single_file = True,
        ),
    },
    toolchains = [
        JAVA_RUNTIME_TOOLCHAIN_TYPE,
        JAVA_TOOLCHAIN_TYPE,
    ],
)

def _partial(function, **defaults):
    def partial(**call):
        resolved = dict(defaults)
        resolved.update(call)
        return function(**resolved)

    return partial
