load("@apple_rules_lint//lint:defs.bzl", "get_lint_config")
load("//kotlin/internal/lint:ktlint_fix.bzl", "ktlint_fix")
load("//kotlin/internal/lint:ktlint_test.bzl", "ktlint_test")
load(
    ":jvm.bzl",
    _kt_jvm_binary = "kt_jvm_binary",
    _kt_jvm_library = "kt_jvm_library",
    _kt_jvm_test = "kt_jvm_test")

def _common_tests(name, srcs, tags):
    config = get_lint_config("kt-ktlint", tags)
    if not config:
        return

    srcs = srcs if srcs else []

    # Some kotlin rules allow mixing of kotlin and java sources. The
    # ktlint process doesn't handle them, so extract just the Kotlin source.
    kotlin_srcs = [src for src in srcs if src.endswith(".kt") or src.endswith(".kts")]

    if len(srcs) == 0:
        return

    ktlint_test(
        name = "%s-ktlint" % name,
        srcs = kotlin_srcs,
        config = config,
        size = "small",
        tags = [
            "kt-ktlint",
            "lint",
        ]
    )

    ktlint_fix(
        name = "%s-ktlint-fix" % name,
        srcs = kotlin_srcs,
        config = config,
    )

def kt_jvm_binary(name, srcs = None, **kwargs):
    _kt_jvm_binary(
        name = name,
        srcs = srcs,
        **kwargs,
    )

    _common_tests(name = name, srcs = srcs, tags = kwargs.get("tags", []))

def kt_jvm_library(name, srcs = None, **kwargs):
    _kt_jvm_library(
        name = name,
        srcs = srcs,
        **kwargs,
    )

    _common_tests(name = name, srcs = srcs, tags = kwargs.get("tags", []))

def kt_jvm_test(name, srcs = None, **kwargs):
    _kt_jvm_test(
        name = name,
        srcs = srcs,
        **kwargs,
    )

    _common_tests(name = name, srcs = srcs, tags = kwargs.get("tags", []))