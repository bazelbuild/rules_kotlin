load("@rules_testing//lib:util.bzl", "util")

def _prepend(rule, name, **kwargs):
    util.helper_target(
        rule,
        name = name,
        **kwargs
    )
    return ":" + name

def case(namespace):
    return struct(
        name = namespace,
        have = lambda rule, name, **kwargs: _prepend(rule, namespace + "_" + name, **kwargs),
        artifact = lambda name, **kwargs: util.empty_file(
            name = namespace + "_" + name,
            **kwargs
        ),
        got = lambda rule, name, **kwargs: _prepend(rule, namespace + "_" + name, **kwargs),
        ref = lambda name: ":" + namespace + "_" + name,
    )

def suite(name, *tests):
    test_targets = []
    for test in tests:
        test_name = str(test).split(" ")[1]
        test_targets.append(":" + test_name)
        test(case(test_name))

    native.test_suite(
        name = name,
        tests = test_targets,
    )
