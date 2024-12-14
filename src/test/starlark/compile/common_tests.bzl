load("//src/main/starlark/core/compile:common.bzl", "KtJvmInfo")
load("//src/main/starlark/core/compile:rules.bzl", "core_kt_jvm_binary", "core_kt_jvm_library")
load("//src/test/starlark:case.bzl", "Want", "suite")

#load("//src/test/starlark:truth.bzl", "fail_messages_in", "flags_and_values_of")
load(":subjects.bzl", "java_info_subject_factory")

def map_to(attr):
    return {
        "desc": attr,
        "map_each": lambda o: getattr(o, attr, None),
    }

def _outputs(env, got):
    got_target = env.expect.that_target(got)

    # assert java outputs
    got_target.has_provider(JavaInfo)
    got_java_info = got_target.provider(JavaInfo, java_info_subject_factory)
    got_java_info.java_outputs().transform(**map_to("class_jar")).contains(env.ctx.file.class_jar)

    want_compile_jars = [
        j
        for d in env.ctx.attr.transitive_compile_deps
        if JavaInfo in d
        for j in d[JavaInfo].compile_jars.to_list()
    ]

    want_runtime_jars = [
        j
        for d in env.ctx.attr.transitive_runtime_deps
        if JavaInfo in d
        for j in d[JavaInfo].transitive_runtime_jars.to_list()
    ]

    got_java_info.transitive_compile_time_jars().contains_exactly([j.short_path for j in want_compile_jars])
    got_java_info.transitive_runtime_jars().contains_exactly([j.short_path for j in want_runtime_jars])
    got_java_info.source_jars().contains(env.ctx.file.source_jar)

    # get actions
    source = got_target.action_generating(env.ctx.file.source_jar.short_path)

    compile = got_target.action_generating(env.ctx.file.class_jar.short_path)
    compile.contains_at_least_inputs(
        [i[JavaInfo].compile_jars.to_list() for i in env.ctx.attr.inputs if JavaInfo in i] + env.ctx.files.inputs,
    )

def _test_neverlink_deps(test, rule_under_test, **kwargs):
    have = test.have(
        rule_under_test,
        name = "have",
        srcs = [test.artifact("hold.kt")],
        deps = [],
        neverlink = True,
        **kwargs
    )

    got_src = test.artifact("gave.kt")
    got = test.got(
        rule_under_test,
        name = "got",
        srcs = [got_src],
        deps = [have],
        **kwargs
    )
    test.claim(
        got = got,
        what = _outputs,
        wants = {
            "class_jar": Want(
                attr = attr.label(allow_single_file = True),
                value = got + "lib.jar",
            ),
            "source_jar": Want(
                attr = attr.label(allow_single_file = True),
                value = got + "lib.srcjar",
            ),
            "inputs": Want(
                attr = attr.label_list(allow_empty = True, allow_files = True),
                value = [got_src, have + "lib.jar"],
            ),
            "transitive_compile_deps": Want(
                attr = attr.label_list(providers = [[JavaInfo], [KtJvmInfo]]),
                value = [got, have],
            ),
            "transitive_runtime_deps": Want(
                attr = attr.label_list(providers = [[JavaInfo], [KtJvmInfo]]),
                value = [got],
            ),
        },
    )

def _test_deps_core(test, rule_under_test, **kwargs):
    have = test.have(
        rule_under_test,
        name = "have",
        srcs = [
            test.artifact("hold.kt"),
        ],
        deps = [
        ],
        **kwargs
    )

    got = test.got(
        rule_under_test,
        name = "got",
        srcs = [
            test.artifact("gave.kt"),
        ],
        deps = [
            have,
        ],
        **kwargs
    )
    test.claim(
        got = got,
        what = _outputs,
        wants = {
            "class_jar": Want(
                attr = attr.label(allow_single_file = True),
                value = got + "lib.jar",
            ),
            "source_jar": Want(
                attr = attr.label(allow_single_file = True),
                value = got + "lib.srcjar",
            ),
            "inputs": Want(
                attr = attr.label_list(allow_empty = True, allow_files = True),
                value = [have + "lib.jar"],
            ),
            "transitive_compile_deps": Want(
                attr = attr.label_list(providers = [[JavaInfo], [KtJvmInfo]]),
                value = [
                    have,
                    got,
                ],
            ),
            "transitive_runtime_deps": Want(
                attr = attr.label_list(providers = [[JavaInfo], [KtJvmInfo]]),
                value = [got],
            ),
        },
    )

def test_no_deps_core(test, rule_under_test, **kwargs):
    have_src = test.artifact("a.kt")
    got = test.got(
        rule_under_test,
        name = "got",
        srcs = [
            have_src,
        ],
        deps = [
        ],
        **kwargs
    )
    test.claim(
        got = got,
        what = _outputs,
        wants = {
            "class_jar": Want(
                attr = attr.label(allow_single_file = True),
                value = got + "lib.jar",
            ),
            "source_jar": Want(
                attr = attr.label(allow_single_file = True),
                value = got + "lib.srcjar",
            ),
            "transitive_compile_deps": Want(
                attr = attr.label_list(providers = [[JavaInfo], [KtJvmInfo]]),
                value = [got],
            ),
            "transitive_runtime_deps": Want(
                attr = attr.label_list(providers = [[JavaInfo], [KtJvmInfo]]),
                value = [got],
            ),
            "inputs": Want(
                attr = attr.label_list(allow_empty = True, allow_files = True),
                value = [have_src],
            ),
        },
    )

def test_deps_core_kt_jvm_binary(test):
    _test_deps_core(test, core_kt_jvm_binary, main_class = "Foo")

def test_neverlink_deps_core_kt_jvm_binary(test):
    _test_neverlink_deps(test, core_kt_jvm_binary, main_class = "Foo")

def test_no_deps_core_kt_jvm_binary(test):
    test_no_deps_core(test, core_kt_jvm_binary, main_class = "Foo")

def test_deps_core_kt_jvm_library(test):
    _test_deps_core(test, core_kt_jvm_library)

def test_neverlink_deps_core_kt_jvm_library(test):
    _test_neverlink_deps(test, core_kt_jvm_library)

def test_no_deps_core_kt_jvm_library(test):
    test_no_deps_core(test, core_kt_jvm_library)

def test_suite(name):
    suite(
        name,
        test_deps_core_kt_jvm_binary,
        test_deps_core_kt_jvm_library,
        test_neverlink_deps_core_kt_jvm_library,
        test_neverlink_deps_core_kt_jvm_binary,
        test_no_deps_core_kt_jvm_binary,
        test_no_deps_core_kt_jvm_library,
    )
