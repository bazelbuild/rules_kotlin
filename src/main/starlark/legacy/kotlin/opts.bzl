_KOPTS = {
    "warn": struct(
        args = dict(
            default = "report",
            doc = "Control warning behaviour.",
            values = ["off", "report", "error"],
        ),
        type = attr.string,
        value_to_flag = {
            "off": ["-nowarn"],
            "report": None,
            "error": ["-Werror"],
        },
    ),
    "include_stdlibs": struct(
        args = dict(
            default = "all",
            doc = "Don't automatically include the Kotlin standard libraries into the classpath (stdlib and reflect).",
            values = ["all", "stdlib", "none"],
        ),
        type = attr.string,
        value_to_flag = {
            "all": None,
            "stdlib": ["-no-reflect"],
            "none": ["-no-stdlib"],
        },
    ),
    "x_use_experimental": struct(
        args = dict(
            default = True,
            doc = "Allow the experimental language features.",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-Xuse-experimental=kotlin.Experimental"],
        },
    ),
    "x_skip_prerelease_check": struct(
        args = dict(
            default = False,
            doc = "Suppress errors thrown when using pre-release classes.",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-Xskip-prerelease-check"],
        },
    ),
    "x_inline_classes": struct(
        args = dict(
            default = False,
            doc = "Enable experimental inline classes",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-Xinline-classes"],
        },
    ),
    "x_allow_result_return_type": struct(
        args = dict(
            default = False,
            doc = "Enable kotlin.Result as a return type",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-Xallow-result-return-type"],
        },
    ),
    "x_jvm_default": struct(
        args = dict(
            default = "off",
            doc = "Specifies that a JVM default method should be generated for non-abstract Kotlin interface member.",
            values = ["off", "enable", "compatibility"],
        ),
        type = attr.string,
        value_to_flag = {
            "off": None,
            "enable": ["-Xjvm-default=enable"],
            "compatibility": ["-Xjvm-default=compatibility"],
        },
    ),
    "x_no_optimized_callable_references": struct(
        args = dict(
            default = False,
            doc = "Do not use optimized callable reference superclasses. Available from 1.4.",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-Xno-optimized-callable-reference"],
        },
    ),
    "java_parameters": struct(
        args = dict(
            default = False,
            doc = "Generate metadata for Java 1.8+ reflection on method parameters.",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-java-parameters"],
        },
    ),
    "x_multi_platform": struct(
        args = dict(
            default = False,
            doc = "Enable experimental language support for multi-platform projects",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["-Xmulti-platform"],
        },
    ),
    "x_explicit_api_mode": struct(
        args = dict(
            default = "off",
            doc = "Enable explicit API mode for Kotlin libraries.",
            values = ["off", "warning", "strict"],
        ),
        type = attr.string,
        value_to_flag = {
            "off": None,
            "warning": ["-Xexplicit-api=warning"],
            "strict": ["-Xexplicit-api=strict"],
        },
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
    attrs = {
        n: o.type(**o.args)
        for n, o in _KOPTS.items()
    },
)

def kotlinc_options_to_flags(kotlinc_options):
    """Translate KotlincOptions to worker flags

    Args:
        kotlinc_options maybe containing KotlincOptions
    Returns:
        list of flags to add to the command line.
    """
    if not kotlinc_options:
        return ""

    flags = []
    for n, o in _KOPTS.items():
        value = getattr(kotlinc_options, n, None)
        flag = o.value_to_flag.get(value, None)
        if flag:
            flags.extend(flag)
    return flags
