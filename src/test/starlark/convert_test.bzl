load(
    "@bazel_skylib//lib:unittest.bzl",
    "asserts",
    "unittest",
)
load("//src/main/starlark/core/options:convert.bzl", "convert")
load("//src/main/starlark/core/options:derive.bzl", "derive")

def _test_map_value_to_flag(value):
    return ["-flag={}".format(value)]

def _test_map_value_to_flag_repeated(values):
    return ["-flag-repeated={}".format(v) for v in values]

def _passthrough_args(values):
    """Pass through extra args as-is (like extra_kotlinc_args)."""
    if not values:
        return None
    return values

_TEST_OPTS = {
    "map_value_to_flag_repeated_test": struct(
        value_to_flag = None,
        map_value_to_flag = _test_map_value_to_flag_repeated,
    ),
    "map_value_to_flag_test": struct(
        value_to_flag = None,
        map_value_to_flag = _test_map_value_to_flag,
    ),
    "value_to_flag_derive_test": struct(value_to_flag = {
        derive.info: derive.repeated_values_for("-bar:"),
    }),
    "value_to_flag_test": struct(value_to_flag = {
        "options1": ["-foo-options"],
    }),
}

# Test options for tristate boolean behavior (like generated KOPTS)
_TRISTATE_OPTS = {
    "tristate_bool": struct(
        args = dict(
            doc = "Test tristate boolean",
            values = ["true", "false"],
        ),
        value_to_flag = {"true": ["-Xtest-flag"]},
    ),
}

# Test options for extra_kotlinc_args passthrough
_PASSTHROUGH_OPTS = {
    "extra_kotlinc_args": struct(
        args = dict(
            default = [],
            doc = "Pass extra arguments directly to kotlinc.",
        ),
        value_to_flag = None,
        map_value_to_flag = _passthrough_args,
    ),
}

def _convert_options_to_flags_empty_options_test(ctx):
    """Asserts that the converts return None when the
    attr_provider doesn't exist
    """
    env = unittest.begin(ctx)

    asserts.true(env, not convert.kotlinc_options_to_flags({}, None))
    asserts.true(env, not convert.javac_options_to_flags({}, None))

    return unittest.end(env)

convert_options_to_flags_empty_options_test = unittest.make(
    _convert_options_to_flags_empty_options_test,
)

def _convert_options_to_flags_test(ctx):
    """Tests the _to_flags mapper paths
    """
    env = unittest.begin(ctx)

    attrs = struct(
        value_to_flag_test = "options1",
        value_to_flag_derive_test = ["1", "2", "3"],
        map_value_to_flag_test = "1",
        map_value_to_flag_repeated_test = ["1", "2", "3"],
    )
    asserts.equals(
        env,
        expected = convert.kotlinc_options_to_flags(_TEST_OPTS, attrs),
        actual = [
            "-flag-repeated=1",
            "-flag-repeated=2",
            "-flag-repeated=3",
            "-flag=1",
            "-bar:1",
            "-bar:2",
            "-bar:3",
            "-foo-options",
        ],
    )

    return unittest.end(env)

convert_options_to_flags_test = unittest.make(
    _convert_options_to_flags_test,
)

def _tristate_true_passes_flag_test(ctx):
    """Tests that tristate 'true' value passes the flag."""
    env = unittest.begin(ctx)

    attrs = struct(tristate_bool = "true")
    flags = convert.kotlinc_options_to_flags(_TRISTATE_OPTS, attrs)

    asserts.equals(env, ["-Xtest-flag"], flags)

    return unittest.end(env)

tristate_true_passes_flag_test = unittest.make(_tristate_true_passes_flag_test)

def _tristate_false_passes_no_flag_test(ctx):
    """Tests that tristate 'false' value does not pass any flag."""
    env = unittest.begin(ctx)

    attrs = struct(tristate_bool = "false")
    flags = convert.kotlinc_options_to_flags(_TRISTATE_OPTS, attrs)

    # "false" is not in value_to_flag, so no flag should be passed
    asserts.equals(env, [], flags)

    return unittest.end(env)

tristate_false_passes_no_flag_test = unittest.make(_tristate_false_passes_no_flag_test)

def _tristate_empty_passes_no_flag_test(ctx):
    """Tests that tristate empty/unset value does not pass any flag."""
    env = unittest.begin(ctx)

    attrs = struct(tristate_bool = "")
    flags = convert.kotlinc_options_to_flags(_TRISTATE_OPTS, attrs)

    # Empty string is not in value_to_flag, so no flag should be passed
    asserts.equals(env, [], flags)

    return unittest.end(env)

tristate_empty_passes_no_flag_test = unittest.make(_tristate_empty_passes_no_flag_test)

def _extra_kotlinc_args_passthrough_test(ctx):
    """Tests that extra_kotlinc_args passes arguments through unchanged."""
    env = unittest.begin(ctx)

    attrs = struct(extra_kotlinc_args = ["-Xwhen-guards", "-Xnew-flag=value", "-Xanother"])
    flags = convert.kotlinc_options_to_flags(_PASSTHROUGH_OPTS, attrs)

    # Arguments should be passed through exactly as provided
    asserts.equals(env, ["-Xwhen-guards", "-Xnew-flag=value", "-Xanother"], flags)

    return unittest.end(env)

extra_kotlinc_args_passthrough_test = unittest.make(_extra_kotlinc_args_passthrough_test)

def _extra_kotlinc_args_empty_test(ctx):
    """Tests that empty extra_kotlinc_args passes no flags."""
    env = unittest.begin(ctx)

    attrs = struct(extra_kotlinc_args = [])
    flags = convert.kotlinc_options_to_flags(_PASSTHROUGH_OPTS, attrs)

    # Empty list should result in no flags
    asserts.equals(env, [], flags)

    return unittest.end(env)

extra_kotlinc_args_empty_test = unittest.make(_extra_kotlinc_args_empty_test)

def convert_test_suite(name):
    unittest.suite(
        name,
        convert_options_to_flags_empty_options_test,
        convert_options_to_flags_test,
        tristate_true_passes_flag_test,
        tristate_false_passes_no_flag_test,
        tristate_empty_passes_no_flag_test,
        extra_kotlinc_args_passthrough_test,
        extra_kotlinc_args_empty_test,
    )
