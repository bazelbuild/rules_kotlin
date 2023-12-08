load("@rules_testing//lib:test_suite.bzl", "test_suite", "analysis_test")

def _add_plugin_options_test(name):
  analysis_test(
    name = name,
    impl = _foo_test_impl,
    attrs = {"myattr": attr.string(default="default")}
  )




def _assert_plugin_options_impl(env):
  env.expect.that_str(...).equals(...)

def my_test_suite(name):
  test_suite(
    name = name,
    tests = [
      _foo_test,
    ]
  )