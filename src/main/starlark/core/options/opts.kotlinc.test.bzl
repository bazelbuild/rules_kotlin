load("@bazel_skylib//lib:unittest.bzl", "analysistest", "unittest")
load(":opts.kotlinc.bzl", "KotlincOptions", "kotlinc_options_to_flags")

def _kt_kotlinc_options_test_impl(ctx):
    env = analysistest.begin(ctx)

    target_under_test = analysistest.target_under_test(env)

    want = ctx.attr.want_flags

    got = kotlinc_options_to_flags(target_under_test[KotlincOptions])

    extra = [f for f in got if f not in want]
    missing = [f for f in want if f not in got]

    fails = []
    if extra:
        fails.append("not want %s" % extra)
    if missing:
        fails.append("want %s" % missing)

    if fails:
        unittest.fail(env, "\n".join(fails + ["Wanted %s" % want, "Got %s" % got]))

    return analysistest.end(env)

kt_kotlinc_options_test = analysistest.make(
    _kt_kotlinc_options_test_impl,
    attrs = {
        "want_flags": attr.string_list(
            mandatory = True,
        ),
    },
)
