load("@rules_testing//lib:analysis_test.bzl", "analysis_test")
load("@rules_testing//lib:test_suite.bzl", "test_suite")
load("@rules_testing//lib:util.bzl", "util")
load("//kotlin:core.bzl", "define_kt_toolchain")
load("//kotlin:jvm.bzl", "kt_jvm_library")
load("//src/test/starlark:truth.bzl", "flags_and_values_of")

_TOOLCHAIN_FILE_FIELDS = (
    ("btapi_build_tools_impl", "btapi_build_tools_impl_basename"),
    ("btapi_kotlin_compiler_embeddable", "btapi_kotlin_compiler_embeddable_basename"),
    ("btapi_kotlin_daemon_client", "btapi_kotlin_daemon_client_basename"),
    ("btapi_kotlin_stdlib", "btapi_kotlin_stdlib_basename"),
    ("btapi_kotlin_reflect", "btapi_kotlin_reflect_basename"),
    ("btapi_kotlin_coroutines", "btapi_kotlin_coroutines_basename"),
    ("btapi_annotations", "btapi_annotations_basename"),
    ("internal_jvm_abi_gen", "internal_jvm_abi_gen_basename"),
    ("internal_skip_code_gen", "internal_skip_code_gen_basename"),
    ("internal_jdeps_gen", "internal_jdeps_gen_basename"),
    ("internal_kapt", "internal_kapt_basename"),
)

_REQUIRED_COMPILE_FLAGS = [
    "--btapi_build_tools_impl",
    "--btapi_kotlin_compiler_embeddable",
    "--btapi_kotlin_daemon_client",
    "--btapi_kotlin_stdlib",
    "--btapi_kotlin_reflect",
    "--btapi_kotlin_coroutines",
    "--btapi_annotations",
    "--internal_jvm_abi_gen",
    "--internal_skip_code_gen",
    "--internal_kapt",
    "--internal_jdeps",
]

def _assert_field_basename(toolchain, field_name, expected_basename):
    artifact = getattr(toolchain, field_name, None)
    if artifact == None:
        fail("toolchain field '%s' was not set" % field_name)
    if artifact.basename != expected_basename:
        fail(
            "toolchain field '%s' expected basename '%s' but was '%s' (%s)" % (
                field_name,
                expected_basename,
                artifact.basename,
                artifact.short_path,
            ),
        )

def _toolchain_override_wiring_test_impl(env, target):
    env.expect.that_target(target).has_provider(platform_common.ToolchainInfo)
    toolchain = target[platform_common.ToolchainInfo]
    for field_name, attr_name in _TOOLCHAIN_FILE_FIELDS:
        _assert_field_basename(toolchain, field_name, getattr(env.ctx.attr, attr_name))

def _compile_action_runtime_flag_wiring_test_impl(env, target):
    action = env.expect.that_target(target).action_named("KotlinCompile")
    parsed_flags = flags_and_values_of(action)
    parsed_flags.transform(
        desc = "runtime/plugin flag keys",
        map_each = lambda item: item[0],
    ).contains_at_least(_REQUIRED_COMPILE_FLAGS)

def _toolchain_override_wiring_test(name):
    toolchain_name = name + "_toolchain"

    btapi_build_tools_impl_basename = name + "_btapi_build_tools_impl.jar"
    btapi_kotlin_compiler_embeddable_basename = name + "_btapi_kotlin_compiler_embeddable.jar"
    btapi_kotlin_daemon_client_basename = name + "_btapi_kotlin_daemon_client.jar"
    btapi_kotlin_stdlib_basename = name + "_btapi_kotlin_stdlib.jar"
    btapi_kotlin_reflect_basename = name + "_btapi_kotlin_reflect.jar"
    btapi_kotlin_coroutines_basename = name + "_btapi_kotlin_coroutines.jar"
    btapi_annotations_basename = name + "_btapi_annotations.jar"
    internal_jvm_abi_gen_basename = name + "_internal_jvm_abi_gen.jar"
    internal_skip_code_gen_basename = name + "_internal_skip_code_gen.jar"
    internal_jdeps_gen_basename = name + "_internal_jdeps_gen.jar"
    internal_kapt_basename = name + "_internal_kapt.jar"

    define_kt_toolchain(
        name = toolchain_name,
        btapi_build_tools_impl = util.empty_file(btapi_build_tools_impl_basename),
        btapi_kotlin_compiler_embeddable = util.empty_file(btapi_kotlin_compiler_embeddable_basename),
        btapi_kotlin_daemon_client = util.empty_file(btapi_kotlin_daemon_client_basename),
        btapi_kotlin_stdlib = util.empty_file(btapi_kotlin_stdlib_basename),
        btapi_kotlin_reflect = util.empty_file(btapi_kotlin_reflect_basename),
        btapi_kotlin_coroutines = util.empty_file(btapi_kotlin_coroutines_basename),
        btapi_annotations = util.empty_file(btapi_annotations_basename),
        internal_jvm_abi_gen = util.empty_file(internal_jvm_abi_gen_basename),
        internal_skip_code_gen = util.empty_file(internal_skip_code_gen_basename),
        internal_jdeps_gen = util.empty_file(internal_jdeps_gen_basename),
        internal_kapt = util.empty_file(internal_kapt_basename),
    )

    analysis_test(
        name = name,
        impl = _toolchain_override_wiring_test_impl,
        target = toolchain_name + "_impl",
        attr_values = {
            "btapi_annotations_basename": btapi_annotations_basename,
            "btapi_build_tools_impl_basename": btapi_build_tools_impl_basename,
            "btapi_kotlin_compiler_embeddable_basename": btapi_kotlin_compiler_embeddable_basename,
            "btapi_kotlin_coroutines_basename": btapi_kotlin_coroutines_basename,
            "btapi_kotlin_daemon_client_basename": btapi_kotlin_daemon_client_basename,
            "btapi_kotlin_reflect_basename": btapi_kotlin_reflect_basename,
            "btapi_kotlin_stdlib_basename": btapi_kotlin_stdlib_basename,
            "internal_jdeps_gen_basename": internal_jdeps_gen_basename,
            "internal_jvm_abi_gen_basename": internal_jvm_abi_gen_basename,
            "internal_kapt_basename": internal_kapt_basename,
            "internal_skip_code_gen_basename": internal_skip_code_gen_basename,
        },
        attrs = {
            "btapi_annotations_basename": attr.string(),
            "btapi_build_tools_impl_basename": attr.string(),
            "btapi_kotlin_compiler_embeddable_basename": attr.string(),
            "btapi_kotlin_coroutines_basename": attr.string(),
            "btapi_kotlin_daemon_client_basename": attr.string(),
            "btapi_kotlin_reflect_basename": attr.string(),
            "btapi_kotlin_stdlib_basename": attr.string(),
            "internal_jdeps_gen_basename": attr.string(),
            "internal_jvm_abi_gen_basename": attr.string(),
            "internal_kapt_basename": attr.string(),
            "internal_skip_code_gen_basename": attr.string(),
        },
    )

def _compile_action_runtime_flag_wiring_test(name):
    subject_name = name + "_subject"
    kt_jvm_library(
        name = subject_name,
        srcs = [util.empty_file(subject_name + ".kt")],
        tags = ["manual"],
    )

    analysis_test(
        name = name,
        impl = _compile_action_runtime_flag_wiring_test_impl,
        target = subject_name,
    )

def toolchain_runtime_override_test_suite(name):
    test_suite(
        name = name,
        tests = [
            _toolchain_override_wiring_test,
            _compile_action_runtime_flag_wiring_test,
        ],
    )
