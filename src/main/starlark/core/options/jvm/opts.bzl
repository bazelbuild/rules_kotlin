load("//src/main/starlark/core/options:convert.bzl", "convert")

def _map_javac_opts(values):
    return ["%s" % v for v in values]

_JOPTS = {
    "javac_opts": struct(
        args = dict(
            default = [],
            doc = "List of Javac opts to pass to the compiler.",
        ),
        type = attr.string_list,
        value_to_flag = None,
        map_value_to_flag = _map_javac_opts,
    ),
}

def _javac_options_impl(ctx):
    return struct(
        providers = [
            JavacOptions(**{n: getattr(ctx.attr, n, None) for n in _JOPTS}),
        ],
    )

JavacOptions = provider(
    fields = {
        name: o.args["doc"]
        for name, o in _JOPTS.items()
    },
)

kt_javac_options = rule(
    implementation = _javac_options_impl,
    doc = "Define java compiler options for `kt_jvm_*` rules with java sources.",
    provides = [JavacOptions],
    attrs = {n: o.type(**o.args) for n, o in _JOPTS.items()},
)

def javac_options_to_flags(javac_options):
    """Translate JavacOptions to worker flags

    Args:
        javac_options of type JavacOptions or None
    Returns:
        list of flags to add to the command line.
    """
    return convert.convert_opts_to_flags(_JOPTS, javac_options)
