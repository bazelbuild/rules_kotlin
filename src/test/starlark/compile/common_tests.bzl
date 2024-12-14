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

    want_jars = [
        j
        for d in env.ctx.attr.transitive_deps
        if JavaInfo in d
        for j in d[JavaInfo].compile_jars.to_list()
    ]

    got_java_info.transitive_compile_time_jars().contains_at_least(want_jars)
    got_java_info.source_jars().contains(env.ctx.file.source_jar)

    # get actions
    source = got_target.action_generating(env.ctx.file.source_jar.short_path)

    compile = got_target.action_generating(env.ctx.file.class_jar.short_path)
    compile.contains_at_least_inputs(want_jars)

def test_deps_core_kt_jvm_library(test):
    have = test.have(
        core_kt_jvm_library,
        name = "have",
        srcs = [
            test.artifact("hold.kt"),
        ],
        deps = [
        ],
    )

    got = test.got(
        core_kt_jvm_library,
        name = "got",
        srcs = [
            test.artifact("gave.kt"),
        ],
        deps = [
            have,
        ],
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
            "transitive_deps": Want(
                attr = attr.label_list(providers = [[JavaInfo], [KtJvmInfo]]),
                value = [
                    have,
                ],
            ),
        },
    )

def test_deps_core_kt_jvm_binary(test):
    have = test.have(
        core_kt_jvm_binary,
        name = "have",
        srcs = [
            test.artifact("hold.kt"),
        ],
        deps = [
        ],
    )

    got = test.got(
        core_kt_jvm_binary,
        name = "got",
        srcs = [
            test.artifact("gave.kt"),
        ],
        deps = [
            have,
        ],
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
            "transitive_deps": Want(
                attr = attr.label_list(providers = [[JavaInfo], [KtJvmInfo]]),
                value = [
                    have,
                ],
            ),
        },
    )

def test_no_deps_core_kt_jvm_library(test):
    got = test.got(
        core_kt_jvm_library,
        name = "got",
        srcs = [
            test.artifact("a.kt"),
        ],
        deps = [
        ],
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
            "transitive_deps": Want(
                attr = attr.label_list(providers = [[JavaInfo], [KtJvmInfo]]),
                value = [],
            ),
        },
    )

def test_no_deps_core_kt_jvm_binary(test):
    got = test.got(
        core_kt_jvm_binary,
        name = "got",
        srcs = [
            test.artifact("a.kt"),
        ],
        deps = [
        ],
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
            "transitive_deps": Want(
                attr = attr.label_list(providers = [[JavaInfo], [KtJvmInfo]]),
                value = [],
            ),
        },
    )

def test_suite(name):
    suite(
        name,
        test_no_deps_core_kt_jvm_library,
        test_deps_core_kt_jvm_library,
    )
