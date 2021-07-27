_value_to_flag_info = provider(
    fields = {
        "ctx": "_derive_flag_ctx",
        "derive": "function(ctx, value) -> [] ",
    },
)

_derive_flag_ctx = provider(
    fields = {"name": "flag name for the compiler"},
)

def _derive_repeated_flag(ctx, value):
    return ["%s%s" % (ctx.name, v) for v in value]

def _repeated_values_for(name):
    return _value_to_flag_info(
        ctx = _derive_flag_ctx,
        derive = _derive_repeated_flag,
    )

derive = struct(
    info = _value_to_flag_info,
    repeated_values_for = _repeated_values_for,
)
