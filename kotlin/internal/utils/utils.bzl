load("@bazel_skylib//lib:dicts.bzl", "dicts")
load(
    "//kotlin/internal:defs.bzl",
    _TOOLCHAIN_TYPE = "TOOLCHAIN_TYPE",
)

def _derive_module_name(ctx):
    """Gets the `module_name` attribute if it's set in the ctx, otherwise derive a unique module name using the elements
    found in the label."""
    module_name = getattr(ctx.attr, "module_name", "")
    if module_name == "":
        package = ctx.label.package.lstrip("/").replace("/", "_")
        name = ctx.label.name.replace("/", "_")

        # Only add separator if package is not empty to avoid leading dash
        if package:
            module_name = package + "-" + name
        else:
            module_name = name
    return module_name

def _init_builder_args(ctx, rule_kind, module_name = None, kotlinc_options = None):
    """Initialize an arg object for a task that will be executed by the Kotlin Builder."""
    toolchain = ctx.toolchains[_TOOLCHAIN_TYPE]

    args = ctx.actions.args()
    args.set_param_file_format("multiline")
    args.use_param_file("--flagfile=%s", use_always = True)

    if module_name == None:
        module_name = _derive_module_name(ctx)

    args.add("--target_label", ctx.label)
    args.add("--rule_kind", rule_kind)
    args.add("--kotlin_module_name", module_name)

    kotlin_jvm_target = kotlinc_options.jvm_target if (kotlinc_options and kotlinc_options.jvm_target) else toolchain.jvm_target
    args.add("--kotlin_jvm_target", kotlin_jvm_target)
    args.add("--kotlin_api_version", toolchain.api_version)
    args.add("--kotlin_language_version", toolchain.language_version)

    debug = toolchain.debug
    for tag in ctx.attr.tags:
        if tag == "trace":
            debug = debug + [tag]
        if tag == "timings":
            debug = debug + [tag]
    args.add_all("--kotlin_debug_tags", debug, omit_if_empty = False)

    return args

utils = struct(
    add_dicts = dicts.add,
    init_args = _init_builder_args,
)
