load("//src/main/starlark/core/options:convert.bzl", "convert")

def _map_kotlinc_opts(values):
    return ["%s" % v for v in values]

def _map_jvm_target_to_flag(version):
    if not version:
        return None
    return ["-jvm-target=%s" % version]

_KOPTS = {
    "kotlinc_opts": struct(
        args = dict(
            default = [],
            doc = "List of Kotlinc opts to pass to the compiler.",
        ),
        type = attr.string_list,
        value_to_flag = None,
        map_value_to_flag = _map_kotlinc_opts,
    ),
    "jvm_target": struct(
        args = dict(
            default = "",
            doc = "The -jvm_target flag. This is only tested at 1.8.",
            values = ["1.6", "1.8", "9", "10", "11", "12", "13", "15", "16", "17"],
        ),
        type = attr.string,
        value_to_flag = None,
        map_value_to_flag = _map_jvm_target_to_flag,
    ),
}

KotlincOptions = provider(
    fields = {
        name: o.args["doc"]
        for name, o in _KOPTS.items()
    },
)

def _kotlinc_options_impl(ctx):
    return struct(
        providers = [
            KotlincOptions(**{n: getattr(ctx.attr, n, None) for n in _KOPTS}),
        ],
    )

kt_kotlinc_options = rule(
    implementation = _kotlinc_options_impl,
    doc = "Define kotlin compiler options.",
    provides = [KotlincOptions],
    attrs = {n: o.type(**o.args) for n, o in _KOPTS.items()},
)

def kotlinc_options_to_flags(kotlinc_options):
    """Translate KotlincOptions to worker flags

    Args:
        kotlinc_options maybe containing KotlincOptions
    Returns:
        list of flags to add to the command line.
    """
    return convert.convert_opts_to_flags(_KOPTS, kotlinc_options)
