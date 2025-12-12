load("@rules_testing//lib:analysis_test.bzl", "analysis_test")
load("@rules_testing//lib:truth.bzl", "matching")
load("//src/test/starlark:case.bzl", "suite")
load(":arrangement.bzl", "arrange")
load(":util.bzl", "abi_jar_of", "basename_of", "values_for_flag_of")

def _classpath_assertions_(env, target):
    action = env.expect.that_target(target).action_named(env.ctx.attr.on_action_mnemonic)

    # Use basename comparison to avoid configuration transition path issues
    # When config_settings is used, targets are built in a transitioned configuration
    # with a different output path (e.g., k8-fastbuild-ST-xxx instead of k8-fastbuild)

    # For inputs (File objects), use file_basename_equals matcher
    want_input_matchers = [
        matching.file_basename_equals(abi_jar_of(f.basename))
        for f in env.ctx.files.want_inputs
        if not f.basename.endswith("jdeps")
    ]
    action.inputs().contains_at_least_predicates(want_input_matchers)

    # For classpath (string paths), use str_endswith matcher to match by basename
    want_classpath_basenames = [abi_jar_of(f.basename) for f in env.ctx.files.want_classpath if not f.basename.endswith("jdeps")]
    values_for_flag_of(action, "--classpath").transform(desc = "basenames", map_each = basename_of).contains_at_least(want_classpath_basenames)

    not_want_basenames = [f.basename for f in env.ctx.files.not_want_classpath]
    values_for_flag_of(action, "--classpath").transform(desc = "basenames", map_each = basename_of).contains_none_of(not_want_basenames)

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
            "not_want_classpath": [],
            "on_action_mnemonic": "KotlinCompile",
            "want_classpath": [
                dependency_a,
                dependency_a_trans_dep_jar,
            ],
            "want_direct_dependencies": [
                dependency_a,
                dependency_a_trans_dep_jar,
            ],
            "want_inputs": [
                dependency_a,
                dependency_a_trans_dep_jar,
            ],
        },
        attrs = {
            "not_want_classpath": attr.label_list(providers = [DefaultInfo], allow_files = True),
            "on_action_mnemonic": attr.string(),
            "want_classpath": attr.label_list(providers = [DefaultInfo], allow_files = True),
            "want_direct_dependencies": attr.label_list(providers = [DefaultInfo], allow_files = True),
            "want_inputs": attr.label_list(providers = [DefaultInfo], allow_files = True),
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
            "not_want_classpath": [
                dependency_a_trans_dep_jar,
            ],
            "on_action_mnemonic": "KotlinCompile",
            "want_classpath": [
                dependency_a,
            ],
            "want_direct_dependencies": [
                dependency_a,
            ],
            "want_inputs": [
                dependency_a,
            ],
        },
        attrs = {
            "not_want_classpath": attr.label_list(providers = [DefaultInfo], allow_files = True),
            "on_action_mnemonic": attr.string(),
            "want_classpath": attr.label_list(providers = [DefaultInfo], allow_files = True),
            "want_direct_dependencies": attr.label_list(providers = [DefaultInfo], allow_files = True),
            "want_inputs": attr.label_list(providers = [DefaultInfo], allow_files = True),
        },
    )

def experimental_prune_transitive_deps_tests(name):
    suite(
        name,
        enabled = _test_classpath_experimental_prune_transitive_deps_True,
        disabled = _test_classpath_experimental_prune_transitive_deps_False,
    )
