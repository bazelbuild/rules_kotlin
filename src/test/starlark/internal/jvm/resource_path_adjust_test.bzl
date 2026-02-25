load(
    "@bazel_skylib//lib:unittest.bzl",
    "asserts",
    "unittest",
)
load(
    "//kotlin/internal/jvm:compile.bzl",
    "adjust_resources_path_by_strip_prefix_for_testing",
)

def _strip_prefix_with_trailing_slash_test_impl(ctx):
    env = unittest.begin(ctx)

    adjusted_path = adjust_resources_path_by_strip_prefix_for_testing(
        "pkg/test_resources/resource.txt",
        "test_resources/",
    )
    asserts.equals(
        env,
        expected = "resource.txt",
        actual = adjusted_path,
        msg = "resource_strip_prefix with trailing slash should not produce a leading slash",
    )

    return unittest.end(env)

strip_prefix_with_trailing_slash_test = unittest.make(
    _strip_prefix_with_trailing_slash_test_impl,
)

def resource_path_adjust_test_suite(name):
    unittest.suite(
        name,
        strip_prefix_with_trailing_slash_test,
    )
