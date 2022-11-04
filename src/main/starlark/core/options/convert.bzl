load("//src/main/starlark/core/options:derive.bzl", "derive")

def _javac_options_to_flags(jopts, javac_options):
    """Translate JavacOptions to worker flags

    Args:
        jopts dict of name to struct
        javac_options of type JavacOptions or None
    Returns:
        list of flags to add to the command line.
    """
    if not javac_options:
        return ""

    return _to_flags(opts = jopts, attr_provider = javac_options)

def _kotlinc_options_to_flags(kopts, kotlinc_options):
    """Translate KotlincOptions to worker flags

    Args:
        kopts list of kotlin options.
        kotlinc_options maybe containing KotlincOptions
    Returns:
        list of flags to add to the command line.
    """
    if not kotlinc_options:
        return ""

    return _to_flags(opts = kopts, attr_provider = kotlinc_options)

def _to_flags(opts, attr_provider):
    """Translate options to flags

    Args:
        opts dict of name to struct
        attr options provider
    Returns:
        list of flags to add to the command line.
    """
    flags = []
    for n, o in opts.items():
        value = getattr(attr_provider, n, None)
        if o.value_to_flag and derive.info in o.value_to_flag:
            info = o.value_to_flag[derive.info]
            flag = info.derive(info.ctx, value)
        elif o.value_to_flag:
            flag = o.value_to_flag.get(value, None)
        else:
            flag = o.map_value_to_flag(value)
        if flag:
            flags.extend(flag)
    return flags

convert = struct(
    kotlinc_options_to_flags = _kotlinc_options_to_flags,
    javac_options_to_flags = _javac_options_to_flags,
)
