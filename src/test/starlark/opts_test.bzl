load("@bazel_skylib//lib:unittest.bzl", "asserts", "unittest")
load("//src/main/starlark/core/options:opts.javac.bzl", "javac_options_to_flags")
load("//src/main/starlark/core/options:opts.kotlinc.bzl", "kotlinc_options_to_flags")

# x_backend_threads and x_optin require non-None values (map_value_to_flag fails with None)
def _kopts(**kwargs):
    base = {"x_backend_threads": 1, "x_optin": []}
    base.update(kwargs)
    return struct(**base)

def _kotlinc_flags_impl(ctx):
    env = unittest.begin(ctx)

    # map_value_to_flag path
    asserts.equals(env, ["-jvm-target=17"], kotlinc_options_to_flags(_kopts(jvm_target = "17")))
    asserts.equals(env, ["-Xbackend-threads=4"], kotlinc_options_to_flags(_kopts(x_backend_threads = 4)))
    asserts.equals(env, ["-opt-in=A", "-opt-in=B"], kotlinc_options_to_flags(_kopts(x_optin = ["A", "B"])))
    asserts.equals(env, ["-Xwarning-level=UNUSED:warning"], kotlinc_options_to_flags(_kopts(x_warning_level = {"UNUSED": "warning"})))

    # value_to_flag dict path
    asserts.equals(env, ["-Werror"], kotlinc_options_to_flags(_kopts(warn = "error")))
    asserts.equals(env, ["-nowarn"], kotlinc_options_to_flags(_kopts(warn = "off")))
    asserts.equals(env, ["-no-stdlib"], kotlinc_options_to_flags(_kopts(include_stdlibs = "none")))

    # derive.info path (repeated_values_for)
    asserts.equals(env, ["-Xsuppress-warning=SOME"], kotlinc_options_to_flags(_kopts(x_suppress_warning = ["SOME"])))

    # defaults produce no flags; None provider is falsy
    asserts.equals(env, [], kotlinc_options_to_flags(_kopts()))
    asserts.true(env, not kotlinc_options_to_flags(None))

    return unittest.end(env)

kotlinc_flags_test = unittest.make(_kotlinc_flags_impl)

def _javac_flags_impl(ctx):
    env = unittest.begin(ctx)

    # value_to_flag dict path
    asserts.equals(env, ["--release 17"], javac_options_to_flags(struct(release = "17")))
    asserts.equals(env, ["-Werror"], javac_options_to_flags(struct(warn = "error")))
    asserts.equals(env, ["-nowarn"], javac_options_to_flags(struct(warn = "off")))

    # derive.repeated_values_for path
    asserts.equals(env, ["-Xlint:all", "-Xlint:cast"], javac_options_to_flags(struct(x_lint = ["all", "cast"])))
    asserts.equals(env, ["--add-exports=java.base/sun.nio.ch"], javac_options_to_flags(struct(add_exports = ["java.base/sun.nio.ch"])))

    # derive.format_key_value_for path
    asserts.equals(env, ["-Akey=v"], javac_options_to_flags(struct(annotation_processor_options = {"key": "v"})))

    # None provider is falsy
    asserts.true(env, not javac_options_to_flags(None))

    return unittest.end(env)

javac_flags_test = unittest.make(_javac_flags_impl)

def opts_test_suite(name):
    unittest.suite(name, kotlinc_flags_test, javac_flags_test)
