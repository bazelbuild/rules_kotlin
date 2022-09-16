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

    flags = []
    for n, o in jopts.items():
        value = getattr(javac_options, n, None)
        if derive.info in o.value_to_flag:
            info = o.value_to_flag[derive.info]
            flags.extend(info.derive(info.ctx, value))
        elif o.value_to_flag.get(value, None):
            flags.extend(o.value_to_flag[value])

    return flags

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

    flags = []
    for n, o in kopts.items():
        value = getattr(kotlinc_options, n, None)
        flag = o.value_to_flag.get(value, None) if o.value_to_flag else o.map_value_to_flag(value)
        if flag:
            flags.extend(flag)
    return flags

convert = struct(
    kotlinc_options_to_flags = _kotlinc_options_to_flags,
    javac_options_to_flags = _javac_options_to_flags,
)
