load("//src/main/starlark/core/options:convert.bzl", "convert")

def _kapt_options_impl(ctx):
    return struct(
        providers = [
            KaptOptions(**{n: getattr(ctx.attr, n, None) for n in _KAPTOPTS}),
        ],
    )

_KAPTOPTS = {
    "correct_error_types": struct(
        args = dict(
            default = False,
            doc = "Suppress errors thrown when using pre-release classes.",
        ),
        type = attr.bool,
        value_to_flag = {
            True: ["correct_error_types"],
        },
    ),
    "apt_mode": struct(
        args = dict(
            default = "stubsAndApt",
            doc = """
            To configure the level of annotation processing, set one of the following as
            the aptMode \"stubs\", \"apt\", \"stubsAndApt\"
            https://kotlinlang.org/docs/kapt.html#using-in-cli
            """,
            values = ["stubs", "apt", "stubsAndApt"],
        ),
        type = attr.string,
        value_to_flag = {
            "stubsAndApt": ["apt_mode=stubsAndApt"],
            "apt": ["apt_mode=apt"],
            "stubs": ["apt_mode=stubs"],
        },
    ),
}

KaptOptions = provider(
    fields = {
        name: o.args["doc"]
        for name, o in _KAPTOPTS.items()
    },
)

kapt_options = rule(
    implementation = _kapt_options_impl,
    doc = "Define Kapt options.",
    provides = [KaptOptions],
    attrs = {n: o.type(**o.args) for n, o in _KAPTOPTS.items()},
)

def kapt_options_to_flag(kapt_options):
    """Translate KaptOptions to worker flags

    Args:
        correct_error_types correct Error Types https://kotlinlang.org/docs/kapt.html#non-existent-type-correction
        apt_mode https://kotlinlang.org/docs/kapt.html#using-in-cli apt_mode
    Returns:
        list of flags to add to the command line.
    """
    return convert.kapt_options_to_flag(_KAPTOPTS, kapt_options)
