load(
    "@bazel_skylib//lib:unittest.bzl",
    "asserts",
    "unittest",
)
load("//src/main/starlark/core/plugin:payload.bzl", "plugin_payload")

def _plugins_payload_json_encodes_empty_plugins_test_impl(ctx):
    env = unittest.begin(ctx)

    payload_json = plugin_payload.plugins_payload_json([])
    asserts.true(
        env,
        payload_json.startswith("{") and payload_json.endswith("}"),
        msg = "plugins payload should serialize to a JSON object",
    )
    asserts.true(
        env,
        "\"plugins\"" in payload_json,
        msg = "plugins payload should contain the plugins key",
    )

    return unittest.end(env)

plugins_payload_json_encodes_empty_plugins_test = unittest.make(
    _plugins_payload_json_encodes_empty_plugins_test_impl,
)

def plugin_payload_test_suite(name):
    unittest.suite(
        name,
        plugins_payload_json_encodes_empty_plugins_test,
    )
