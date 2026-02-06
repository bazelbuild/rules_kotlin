load("@rules_testing//lib:analysis_test.bzl", "analysis_test")
load("@rules_testing//lib:test_suite.bzl", "test_suite")
load("@rules_testing//lib:util.bzl", "util")
load("//kotlin:jvm.bzl", "kt_jvm_library")
load("//src/test/starlark:truth.bzl", "flags_and_values_of")

def _snapshot_action_test_impl(env, target):
    target_subject = env.expect.that_target(target)
    target_subject.action_named("KotlinClasspathSnapshot")

def _snapshot_flag_wiring_test_impl(env, target):
    compile_action = env.expect.that_target(target).action_named("KotlinCompile")
    parsed_flags = flags_and_values_of(compile_action)
    parsed_flags.transform(
        desc = "classpath snapshot flag wiring",
        map_each = lambda item: (
            item[0] == "--classpath_snapshots" and
            len(item[1]) == 1 and
            item[1][0].endswith(env.ctx.attr.want_snapshot_suffix)
        ),
    ).contains(True)

def _snapshot_action_test(name):
    dep_name = name + "_dep"
    subject_name = name + "_subject"
    kt_jvm_library(
        name = dep_name,
        srcs = [util.empty_file(dep_name + ".kt")],
        tags = ["manual"],
    )
    kt_jvm_library(
        name = subject_name,
        srcs = [util.empty_file(subject_name + ".kt")],
        deps = [dep_name],
        tags = ["manual"],
    )

    analysis_test(
        name = name,
        impl = _snapshot_action_test_impl,
        target = subject_name,
    )

def _snapshot_flag_wiring_test(name):
    dep_name = name + "_dep"
    subject_name = name + "_subject"
    kt_jvm_library(
        name = dep_name,
        srcs = [util.empty_file(dep_name + ".kt")],
        tags = ["manual"],
    )
    kt_jvm_library(
        name = subject_name,
        srcs = [util.empty_file(subject_name + ".kt")],
        deps = [dep_name],
        tags = ["manual"],
    )

    analysis_test(
        name = name,
        impl = _snapshot_flag_wiring_test_impl,
        target = subject_name,
        attr_values = {
            "want_snapshot_suffix": dep_name + ".classpath-snapshot",
        },
        attrs = {
            "want_snapshot_suffix": attr.string(),
        },
    )

def snapshot_action_test_suite(name):
    test_suite(
        name = name,
        tests = [
            _snapshot_action_test,
            _snapshot_flag_wiring_test,
        ],
    )
