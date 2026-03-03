load("@rules_java//java/common:java_info.bzl", "JavaInfo")
load("@rules_testing//lib:analysis_test.bzl", "analysis_test")
load("@rules_testing//lib:util.bzl", "util")
load("//kotlin:jvm.bzl", "kt_jvm_library")

_ABI_STRIPPING_FLAG = str(Label("//src/test/starlark/compile:use_abi_stripping_toolchain"))

def _java_compile_has_associate_class_jars_impl(env, target):
    """Verify the Javac action receives associate CLASS jars (not just ABI jars).

    When experimental_remove_private_classes_in_abi_jars is enabled:
    - The associate's JavaInfo.compile_jars contains ABI jars with internal classes stripped.
    - compile_deps.associate_jars contains the full class jars.
    - compile.bzl wraps associate_jars in synthetic JavaInfos and adds them to
      java_common.compile() deps so javac can resolve internal symbols (e.g. Dagger).

    Without the fix, only the stripped ABI jars would reach javac (via compile_deps.deps).
    With the fix, the full class jars are also present.
    """
    got_target = env.expect.that_target(target)

    # java_common.compile() registers an action with mnemonic "Javac"
    javac_action = got_target.action_named("Javac")

    # With experimental_remove_private_classes_in_abi_jars enabled, associate_jars
    # contains the class jars (java_outputs[].class_jar), which are distinct from
    # the ABI jars (compile_jars). The fix adds these class jars to java_common.compile()
    # deps via synthetic JavaInfos.
    associate_target = env.ctx.attr.associate_target
    associate_class_jar_paths = [
        output.class_jar.short_path
        for output in associate_target[JavaInfo].java_outputs
    ]
    javac_action.inputs().contains_at_least(associate_class_jar_paths)

def _test_java_compile_has_associate_class_jars(name):
    """Mixed Kotlin/Java target with associates passes associate class jars to javac."""
    util.helper_target(
        kt_jvm_library,
        name = name + "_associate_lib",
        srcs = [util.empty_file(name + "_Internal.kt")],
    )

    util.helper_target(
        kt_jvm_library,
        name = name + "_main_lib",
        srcs = [
            util.empty_file(name + "_Main.kt"),
            util.empty_file(name + "_Generated.java"),
        ],
        associates = [name + "_associate_lib"],
    )

    analysis_test(
        name = name,
        impl = _java_compile_has_associate_class_jars_impl,
        target = name + "_main_lib",
        config_settings = {
            _ABI_STRIPPING_FLAG: True,
        },
        attrs = {
            "associate_target": attr.label(providers = [JavaInfo]),
        },
        attr_values = {
            "associate_target": ":" + name + "_associate_lib",
        },
    )

def test_suite(name):
    _test_java_compile_has_associate_class_jars(
        name = "test_java_compile_has_associate_class_jars",
    )

    native.test_suite(
        name = name,
        tests = [
            "test_java_compile_has_associate_class_jars",
        ],
    )
