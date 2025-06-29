load(
    "//kotlin/internal:defs.bzl",
    _KtJsInfo = "KtJsInfo",
    _TOOLCHAIN_TYPE = "TOOLCHAIN_TYPE",
)
load(":impl.bzl", "kt_js_import_impl", "kt_js_library_impl")

kt_js_import = rule(
    implementation = kt_js_import_impl,
    attrs = {
        "klibs": attr.label_list(
            doc = "One or more klib targets that model a kotlin/js library",
            allow_files = [".klib"],
        ),
    },
    toolchains = [_TOOLCHAIN_TYPE],
    provides = [_KtJsInfo],
)

kt_js_library = rule(
    implementation = kt_js_library_impl,
    attrs = {
        "srcs": attr.label_list(
            doc = "A list of source files to be transpiled to JS",
            allow_files = [".kt"],
        ),
        "deps": attr.label_list(
            doc = "A list of other kt_js_library that this library depends on for compilation",
            providers = [_KtJsInfo],
        ),
        "output_kind": attr.string(
            values = ["klib", "js"],
            default = "klib",
            doc = "The output to be generated with the rule, either klib or js",
        ),
        "sourcemap": attr.bool(
            default = False,
            doc = "Indicates whether sourcemaps (.js.map) files should be emitted if output_kind is set to js",
        ),
        "_js_stdlibs": attr.label_list(
            default = [Label("//kotlin/compiler:kotlin-stdlib-js-klib")],
            providers = [_KtJsInfo],
        ),
    },
    toolchains = [_TOOLCHAIN_TYPE],
    provides = [_KtJsInfo],
)
