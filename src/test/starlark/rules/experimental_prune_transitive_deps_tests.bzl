load("@rules_testing//lib:analysis_test.bzl", "analysis_test")
load("//src/test/starlark:case.bzl", "suite")
load(":arrangement.bzl", "arrange")
load(":util.bzl", "abi_jar_of", "values_for_flag_of")

def _classpath_assertions_(env, target):
    action = env.expect.that_target(target).action_named(env.ctx.attr.on_action_mnemonic)
    action.inputs().contains_at_least([abi_jar_of(f.short_path) for f in env.ctx.files.want_inputs if not f.short_path.endswith("jdeps")])

    values_for_flag_of(action, "--classpath").contains_at_least([abi_jar_of(f.path) for f in env.ctx.files.want_classpath if not f.short_path.endswith("jdeps")])
    values_for_flag_of(action, "--classpath").contains_none_of([f.path for f in env.ctx.files.not_want_classpath])

def _test_classpath_experimental_prune_transitive_deps_False(test):
    (dependency_a_trans_dep_jar, dependency_a, main_target_library) = arrange(test)

    analysis_test(
        name = test.name,
        impl = _classpath_assertions_,
        target = main_target_library,
        config_settings = {
            str(Label("@rules_kotlin//kotlin/settings:experimental_prune_transitive_deps")): False,
            str(Label("@rules_kotlin//kotlin/settings:experimental_strict_associate_dependencies")): False,
        },
        attr_values = {
            "on_action_mnemonic": "KotlinCompile",
            "want_inputs": [
                dependency_a,
                dependency_a_trans_dep_jar,
            ],
            "want_direct_dependencies": [
                dependency_a,
                dependency_a_trans_dep_jar,
            ],
            "want_classpath": [
                dependency_a,
                dependency_a_trans_dep_jar,
            ],
            "not_want_classpath": [],
        },
        attrs = {
            "on_action_mnemonic": attr.string(),
            "want_inputs": attr.label_list(providers = [DefaultInfo], allow_files = True),
            "want_direct_dependencies": attr.label_list(providers = [DefaultInfo], allow_files = True),
            "want_classpath": attr.label_list(providers = [DefaultInfo], allow_files = True),
            "not_want_classpath": attr.label_list(providers = [DefaultInfo], allow_files = True),
        },
    )

def _test_classpath_experimental_prune_transitive_deps_True(test):
    (dependency_a_trans_dep_jar, dependency_a, main_target_library) = arrange(test)

    analysis_test(
        name = test.name,
        impl = _classpath_assertions_,
        target = main_target_library,
        config_settings = {
            str(Label("@rules_kotlin//kotlin/settings:experimental_prune_transitive_deps")): True,
            str(Label("@rules_kotlin//kotlin/settings:experimental_strict_associate_dependencies")): False,
        },
        attr_values = {
            "on_action_mnemonic": "KotlinCompile",
            "want_inputs": [
                dependency_a,
            ],
            "want_direct_dependencies": [
                dependency_a,
            ],
            "want_classpath": [
                dependency_a,
            ],
            "not_want_classpath": [
                dependency_a_trans_dep_jar,
            ],
        },
        attrs = {
            "on_action_mnemonic": attr.string(),
            "want_inputs": attr.label_list(providers = [DefaultInfo], allow_files = True),
            "want_direct_dependencies": attr.label_list(providers = [DefaultInfo], allow_files = True),
            "want_classpath": attr.label_list(providers = [DefaultInfo], allow_files = True),
            "not_want_classpath": attr.label_list(providers = [DefaultInfo], allow_files = True),
        },
    )

def experimental_prune_transitive_deps_tests(name):
    suite(
        name,
        enabled = _test_classpath_experimental_prune_transitive_deps_True,
        disabled = _test_classpath_experimental_prune_transitive_deps_False,
    )
