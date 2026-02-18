load("@rules_testing//lib:analysis_test.bzl", "analysis_test")
load("@rules_testing//lib:test_suite.bzl", "test_suite")
load("@rules_testing//lib:util.bzl", "util")
load("@rules_java//java:defs.bzl", "java_import")
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

def _snapshot_flag_wiring_transitive_test_impl(env, target):
    def _has_snapshot_suffix(values, suffix):
        for value in values:
            if value.endswith(suffix):
                return True
        return False

    compile_action = env.expect.that_target(target).action_named("KotlinCompile")
    parsed_flags = flags_and_values_of(compile_action)
    parsed_flags.transform(
        desc = "transitive classpath snapshot flag wiring",
        map_each = lambda item: (
            item[0] == "--classpath_snapshots" and
            _has_snapshot_suffix(item[1], env.ctx.attr.want_direct_snapshot_suffix) and
            _has_snapshot_suffix(item[1], env.ctx.attr.want_transitive_snapshot_suffix)
        ),
    ).contains(True)

def _snapshot_flag_wiring_transitive_test(name):
    transitive_dep_name = name + "_transitive_dep"
    direct_dep_name = name + "_direct_dep"
    subject_name = name + "_subject"

    kt_jvm_library(
        name = transitive_dep_name,
        srcs = [util.empty_file(transitive_dep_name + ".kt")],
        tags = ["manual"],
    )
    kt_jvm_library(
        name = direct_dep_name,
        srcs = [util.empty_file(direct_dep_name + ".kt")],
        deps = [transitive_dep_name],
        tags = ["manual"],
    )
    kt_jvm_library(
        name = subject_name,
        srcs = [util.empty_file(subject_name + ".kt")],
        deps = [direct_dep_name],
        tags = ["manual"],
    )

    analysis_test(
        name = name,
        impl = _snapshot_flag_wiring_transitive_test_impl,
        target = subject_name,
        attr_values = {
            "want_direct_snapshot_suffix": direct_dep_name + ".classpath-snapshot",
            "want_transitive_snapshot_suffix": transitive_dep_name + ".classpath-snapshot",
        },
        attrs = {
            "want_direct_snapshot_suffix": attr.string(),
            "want_transitive_snapshot_suffix": attr.string(),
        },
    )

def _snapshot_flag_wiring_java_only_dep_test_impl(env, target):
    compile_action = env.expect.that_target(target).action_named("KotlinCompile")
    parsed_flags = flags_and_values_of(compile_action)
    parsed_flags.transform(
        desc = "java-only dependency snapshot wiring",
        map_each = lambda item: (
            item[0] == "--classpath_snapshots" and
            len(item[1]) > 0
        ),
    ).contains(True)

def _snapshot_flag_wiring_java_only_dep_test(name):
    java_dep_name = name + "_java_dep"
    subject_name = name + "_subject"

    java_import(
        name = java_dep_name,
        jars = [util.empty_file(java_dep_name + ".jar")],
        tags = ["manual"],
    )

    kt_jvm_library(
        name = subject_name,
        srcs = [util.empty_file(subject_name + ".kt")],
        deps = [java_dep_name],
        tags = ["manual"],
    )

    analysis_test(
        name = name,
        impl = _snapshot_flag_wiring_java_only_dep_test_impl,
        target = subject_name,
    )

def _snapshot_flag_wiring_exports_transitive_test_impl(env, target):
    def _has_snapshot_suffix(values, suffix):
        for value in values:
            if value.endswith(suffix):
                return True
        return False

    compile_action = env.expect.that_target(target).action_named("KotlinCompile")
    parsed_flags = flags_and_values_of(compile_action)
    parsed_flags.transform(
        desc = "exported transitive classpath snapshot flag wiring",
        map_each = lambda item: (
            item[0] == "--classpath_snapshots" and
            _has_snapshot_suffix(item[1], env.ctx.attr.want_exported_snapshot_suffix)
        ),
    ).contains(True)

def _snapshot_flag_wiring_exports_transitive_test(name):
    exported_dep_name = name + "_exported_dep"
    exporter_name = name + "_exporter"
    subject_name = name + "_subject"

    kt_jvm_library(
        name = exported_dep_name,
        srcs = [util.empty_file(exported_dep_name + ".kt")],
        tags = ["manual"],
    )
    kt_jvm_library(
        name = exporter_name,
        srcs = [util.empty_file(exporter_name + ".kt")],
        exports = [exported_dep_name],
        tags = ["manual"],
    )
    kt_jvm_library(
        name = subject_name,
        srcs = [util.empty_file(subject_name + ".kt")],
        deps = [exporter_name],
        tags = ["manual"],
    )

    analysis_test(
        name = name,
        impl = _snapshot_flag_wiring_exports_transitive_test_impl,
        target = subject_name,
        attr_values = {
            "want_exported_snapshot_suffix": exported_dep_name + ".classpath-snapshot",
        },
        attrs = {
            "want_exported_snapshot_suffix": attr.string(),
        },
    )

def snapshot_action_test_suite(name):
    test_suite(
        name = name,
        tests = [
            _snapshot_action_test,
            _snapshot_flag_wiring_test,
            _snapshot_flag_wiring_transitive_test,
            _snapshot_flag_wiring_java_only_dep_test,
            _snapshot_flag_wiring_exports_transitive_test,
        ],
    )
