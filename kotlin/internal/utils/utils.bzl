load(
    "//kotlin/internal:defs.bzl",
    _TOOLCHAIN_TYPE = "TOOLCHAIN_TYPE",
)

def _restore_label(l):
    """Restore a label struct to a canonical label string."""
    lbl = l.workspace_root
    if lbl.startswith("external/"):
        lbl = lbl.replace("external/", "@")
    return lbl + "//" + l.package + ":" + l.name

# TODO unexport this once init builder args can take care of friends.
def _derive_module_name(ctx):
    """Gets the `module_name` attribute if it's set in the ctx, otherwise derive a unique module name using the elements
    found in the label."""
    module_name = getattr(ctx.attr, "module_name", "")
    if module_name == "":
        module_name = (ctx.label.package.lstrip("/").replace("/", "_") + "-" + ctx.label.name.replace("/", "_"))
    return module_name

def _init_builder_args(ctx, rule_kind, module_name):
    """Initialize an arg object for a task that will be executed by the Kotlin Builder."""
    toolchain = ctx.toolchains[_TOOLCHAIN_TYPE]

    args = ctx.actions.args()
    args.set_param_file_format("multiline")
    args.use_param_file("--flagfile=%s", use_always = True)

    args.add("--target_label", ctx.label)
    args.add("--rule_kind", rule_kind)
    args.add("--kotlin_module_name", module_name)

    args.add("--kotlin_jvm_target", toolchain.jvm_target)
    args.add("--kotlin_api_version", toolchain.api_version)
    args.add("--kotlin_language_version", toolchain.language_version)
    args.add("--kotlin_passthrough_flags", "-Xcoroutines=%s" % toolchain.coroutines)

    debug = depset(toolchain.debug)
    for tag in ctx.attr.tags:
        if tag == "trace":
            debug = debug + [tag]
        if tag == "timings":
            debug = debug + [tag]
    args.add("--kotlin_debug_tags", debug)

    return args

utils = struct(
    init_args = _init_builder_args,
    restore_label = _restore_label,
    derive_module_name = _derive_module_name,
)
