load("//:providers.bzl", "KtJsInfo", "KtJvmInfo")

def _kt_repo_import_impl(ctx):
    artifact = struct(
        class_jar = ctx.file.jar,
        ijar = None,
        source_jars = [ctx.file.srcjar] if ctx.file.srcjar else [],
    )
    label = ctx.label
    module_name = label.package.lstrip("/").replace("/", "_") + "-" + label.name.replace("/", "_")
    kt_jvm_info = KtJvmInfo(
        module_name = module_name,
        module_jars = [
            artifact.class_jar,
        ],
        srcs = artifact.source_jars,
        exported_compiler_plugins = depset(),
        outputs = struct(jars = [artifact]),
    )
    kt_providers = [kt_jvm_info]
    if ctx.files.js:
        kt_providers.append(
            KtJsInfo(
                js = ctx.file.js,
                js_map = ctx.file.js_map,
                jar = artifact.class_jar,
                srcjar = ctx.file.srcjar if ctx.file.srcjar else None,
            ),
        )

    return struct(
        kt = kt_jvm_info,  # needed for ijwb
        providers = [
            DefaultInfo(
                files = depset(direct = [artifact.class_jar]),
                runfiles = ctx.runfiles(
                    files = [artifact.class_jar] + artifact.source_jars,
                ).merge_all([d[DefaultInfo].default_runfiles for d in ctx.attr.deps]),
            ),
            JavaInfo(
                output_jar = artifact.class_jar,
                compile_jar = artifact.class_jar,
                source_jar = ctx.file.srcjar if ctx.file.srcjar else None,
                runtime_deps = [],
                deps = [dep[JavaInfo] for dep in ctx.attr.deps if JavaInfo in dep],
                exports = [],
                neverlink = ctx.attr.neverlink,
            ),
        ] + kt_providers,
    )

kt_repo_import = rule(
    implementation = _kt_repo_import_impl,
    attrs = {
        "jar": attr.label(
            doc = """The jar listed here is equivalent to an export attribute.""",
            allow_single_file = True,
            cfg = "target",
            mandatory = True,
        ),
        "srcjar": attr.label(
            doc = """The sources for the class jar.""",
            allow_single_file = True,
            cfg = "target",
        ),
        "neverlink": attr.bool(
            default = False,
        ),
        "js": attr.label(
            allow_single_file = [".js"],
        ),
        "js_map": attr.label(
            allow_single_file = [".js.map"],
        ),
        "deps": attr.label_list(
            doc = """Compile and runtime dependencies""",
            default = [],
            providers = [JavaInfo],
        ),
    },
    provides = [JavaInfo, KtJvmInfo],
)
