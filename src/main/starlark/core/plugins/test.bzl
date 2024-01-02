load(":options.bzl", "kt_plugin_cfg")
load(":providers.bzl", "KspPluginInfo", "KtCompilerPluginInfo", "KtPluginConfiguration")
load("@rules_testing//lib:analysis_test.bzl", "analysis_test", "test_suite")
load("@rules_testing//lib:util.bzl", "util")

def _artifact_impl(ctx):
    out = ctx.actions.file("%s_artifact" % ctx.attr.name)
    ctx.actions.run(
        mnemonic = "Artifact",
        inputs = [],
        outputs = [out],
        executable = "",
        arguments = [args],
    )
    return struct(
        providers = [
            DefaultInfo(files = [out]),
        ],
    )

_artifact = rule(
    implementation = _artifact_impl,
)

def _plugin_stub_impl(ctx):
    info = None
    if ctx.attrs.type == "ksp":
        info = KspPluginInfo(
        )
    elif ctx.attrs.type == "compiler":
        info = KtCompilerPluginInfo(
        )
    elif ctx.attr.type == "java":
        info = JavaPluginInfo()

    return struct(
        providers = [
            info,
        ],
    )

_compiler_plugin_stub = rule(
    implementation = _compiler_plugin_stub_impl,
    attrs = {
        "type": attr.string(
            values = [
                "ksp",
                "compiler",
            ],
        ),
    },
)

def _provider_test_impl(env, target):
    env.expect.assert_that(target).provider(KtPluginOptions).contains(env.ctx.want_providers)

def _test_plugin_options(name):
    util.helper_target(
        _compiler_plugin_stub,
        name = "have_" + name,
        type = "ksp",
    )

    util.helper_target(
        _compiler_plugin_stub,
        name = "want_" + name,
        type = "ksp",
    )

    kt_plugin_options(
        name = "got_" + name,
        plugin = "have_" + name,
        options = {
            "-Dop": "koo",
        },
    )

    analysis_test(
        name = name,
        impl = _provider_test_impl,
        target = name + "_check",
        attr_values = {
            "want_providers": [":want_" + name],
        },
        attrs = {
            "want_providers": attr.label_list(
                providers = [KtPluginOptions],
            ),
        },
    )

def test_compile_configuration(name):
    def _compile_test_impl(env, target):
        env.expect.assert_that(target).has_action().inputs().contains(ctx.attr.files.inputs)
        env.expect.assert_that(target).has_action().arguments().contains(ctx.attr.want_flags)

    util.helper_target(
        _artifact,
        name = "have_artifact_" + name,
    )

    util.helper_target(
        _compiler_plugin_stub,
        name = "have_plugin_" + name,
        type = "java",
    )

    kt_plugin_options(
        name = "have_cfg_" + name,
        plugin = "have_plugin_" + name,
        options = [
            "-Dop=koo",
        ],
    )

    kt_jvm_library(
        name = "got_" + name,
        srcs = [
            ":have_artifact_" + name,
        ],
        deps = [
            ":have_" + name,
        ],
    )

    analysis_test(
        name = name,
        impl = _provider_test_impl,
        target = name + "_check",
        attr_values = {
            "want_flags": [
                "--compiler_plugin_options=kapt:-Dop=koo",
            ],
            "want_inputs": [
                ":have_artifact_" + name,
            ],
        },
        attrs = {
            "want_flags": attr.string_list(),
            "want_inputs": attr.label_list(providers = [DefaultInfo]),
        },
    )
