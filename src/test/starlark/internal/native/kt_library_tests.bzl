load("@rules_testing//lib:analysis_test.bzl", "analysis_test")
load("@rules_testing//lib:test_suite.bzl", "test_suite")
load("//kotlin/internal:defs.bzl", "KtKlibInfo")
load("//kotlin/internal/native:library.bzl", "kt_library")

def _common_assertions(env, target):
    # Assertions common to all kt_library tests
    target_subject = env.expect.that_target(target)
    target_subject.has_provider(KtKlibInfo)

    action_subject = target_subject.action_named("KotlinKlibCompile")
    action_subject.argv().contains_at_least(["--rule_kind", "kt_library", "--output_klib", "--konan_home"])

def _test_kt_library_basic_impl(env, target):
    _common_assertions(env, target)
    target_subject = env.expect.that_target(target)
    action_subject = target_subject.action_named("KotlinKlibCompile")
    action_subject.inputs().contains("src/test/starlark/internal/native/Basic.kt")

def _test_kt_library_basic(name):
    kt_library(name = "basic", srcs = ["Basic.kt"], tags = ["manual"])

    analysis_test(name, target = "basic", impl = _test_kt_library_basic_impl)

def _test_kt_library_deps_impl(env, target):
    _common_assertions(env, target)

    target_subject = env.expect.that_target(target)
    action_subject = target_subject.action_named("KotlinKlibCompile")
    action_subject.inputs().contains("src/test/starlark/internal/native/Second.kt")

    # the klib from first compilation is passed as input
    action_subject.inputs().contains("src/test/starlark/internal/native/first.klib")

    # and it's passed through the arguments
    action_subject.argv().contains_at_least(["--klibs"])

def _test_kt_library_deps(name):
    kt_library(name = "first", srcs = ["First.kt"], tags = ["manual"])

    kt_library(name = "second", srcs = ["Second.kt"], deps = [":first"], tags = ["manual"])

    analysis_test(name, target = "second", impl = _test_kt_library_deps_impl)

def _test_kt_library_runfiles_impl(env, target):
    _common_assertions(env, target)

    target_subject = env.expect.that_target(target)
    target_subject.data_runfiles().contains_at_least(["_main/src/test/starlark/internal/native/foo.txt"])

def _test_kt_library_runfiles(name):
    native.filegroup(
        name = "runfiles1",
        srcs = ["foo.txt"],
    )
    kt_library(name = "lib_with_data", srcs = ["First.kt"], data = [":runfiles1"])

    analysis_test(name, target = "lib_with_data", impl = _test_kt_library_runfiles_impl)

def _test_kt_library_transitive_runfiles_impl(env, target):
    _common_assertions(env, target)

    target_subject = env.expect.that_target(target)
    target_subject.data_runfiles().contains_at_least(["_main/src/test/starlark/internal/native/foo.txt"])

def _test_kt_library_transitive_runfiles(name):
    native.filegroup(
        name = "runfiles2",
        srcs = ["foo.txt"],
    )
    kt_library(name = "lib2_with_data", srcs = ["First.kt"], data = [":runfiles2"])

    kt_library(name = "final_lib", srcs = ["Second.kt"], deps = [":lib2_with_data"])

    analysis_test(name, target = "final_lib", impl = _test_kt_library_transitive_runfiles_impl)

def kt_library_test_suite(name):
    test_suite(
        name = name,
        tests = [
            # assert compilation of single target with no deps works
            _test_kt_library_basic,
            # Assert compilation with deps works
            _test_kt_library_deps,
            # Assert runfiles are available
            _test_kt_library_runfiles,
            # Assert transitive runfiles are propagated
            _test_kt_library_transitive_runfiles,
        ],
    )
