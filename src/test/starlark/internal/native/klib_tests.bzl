load("@rules_testing//lib:analysis_test.bzl", "analysis_test")
load("@rules_testing//lib:test_suite.bzl", "test_suite")
load("//kotlin/internal:defs.bzl", "KtKlibInfo")
load("//kotlin/internal/native:klib.bzl", "kt_klib_library")

def _common_assertions(env, target):
    # Assertions common to all kt_klib_library tests
    target_subject = env.expect.that_target(target)
    target_subject.has_provider(KtKlibInfo)

    action_subject = target_subject.action_named("KotlinKlibCompile")
    action_subject.argv().contains_at_least(["--rule_kind", "kt_klib_library", "--output_klib", "--konan_home"])

def _test_kt_klib_basic_impl(env, target):
    _common_assertions(env, target)
    target_subject = env.expect.that_target(target)
    action_subject = target_subject.action_named("KotlinKlibCompile")
    action_subject.inputs().contains("src/test/starlark/internal/native/Basic.kt")

def _test_kt_klib_basic(name):
    kt_klib_library(name = "basic", srcs = ["Basic.kt"], tags = ["manual"])

    analysis_test(name, target = "basic", impl = _test_kt_klib_basic_impl)

def _test_kt_klib_deps_impl(env, target):
    _common_assertions(env, target)

    target_subject = env.expect.that_target(target)
    action_subject = target_subject.action_named("KotlinKlibCompile")
    action_subject.inputs().contains("src/test/starlark/internal/native/Second.kt")

    # the klib from first compilation is passed as input
    action_subject.inputs().contains("src/test/starlark/internal/native/first.klib")

    # and it's passed through the arguments
    action_subject.argv().contains_at_least(["--klibs"])

def _test_kt_klib_deps(name):
    kt_klib_library(name = "first", srcs = ["First.kt"], tags = ["manual"])

    kt_klib_library(name = "second", srcs = ["Second.kt"], deps = [":first"], tags = ["manual"])

    analysis_test(name, target = "second", impl = _test_kt_klib_deps_impl)

def kt_klib_test_suite(name):
    test_suite(
        name = name,
        tests = [
            _test_kt_klib_basic,
            _test_kt_klib_deps,
        ],
    )
