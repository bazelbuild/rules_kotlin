load("@bazel_skylib//lib:structs.bzl", _structs = "structs")
load("@rules_java//java/common:java_info.bzl", "JavaInfo")
load("@rules_testing//lib:analysis_test.bzl", "analysis_test")
load("@rules_testing//lib:test_suite.bzl", "test_suite")
load("@rules_testing//lib:util.bzl", "util")
load("//kotlin/internal:defs.bzl", _KtJvmInfo = "KtJvmInfo")
load("//kotlin/internal/jvm:jvm_deps.bzl", _jvm_deps_utils = "jvm_deps_utils")

def _file(target):
    return target.files.to_list()[0]

def _setup(env, target):
    associate_deps_java_info = JavaInfo(
        compile_jar = _file(env.ctx.attr.associate_abi_jar),
        output_jar = _file(env.ctx.attr.associate_jar),
    )

    associate_deps = [
        {
            JavaInfo: associate_deps_java_info,
            _KtJvmInfo: _KtJvmInfo(
                module_name = "associate_name",
            ),
        },
    ]

    direct_deps = [
        {
            JavaInfo: JavaInfo(
                compile_jar = _file(env.ctx.attr.direct_dep_abi_jar),
                output_jar = _file(env.ctx.attr.direct_dep_jar),
            ),
        },
        {
            JavaInfo: associate_deps_java_info,
        },
    ]

    fake_ctx = struct(
        label = target.label,
        attr = struct(
            module_name = "",
            tags = [],
        ),
    )
    return struct(
        associate_deps = associate_deps,
        direct_deps = direct_deps,
        fake_ctx = fake_ctx,
        associate_deps_java_info = associate_deps_java_info,
    )

def _strict_abi_test_impl(env, target):
    # this target has these deps ie associate is both transitive and direct friend
    # Target---> direct -> associate
    #        \-> associate

    arrangment = _setup(env, target)

    strict_abi_configured_toolchains = struct(
        kt = struct(
            experimental_remove_private_classes_in_abi_jars = True,
            experimental_prune_transitive_deps = True,
            experimental_strict_associate_dependencies = True,
            jvm_stdlibs = JavaInfo(
                compile_jar = _file(env.ctx.attr.jvm_jar),
                output_jar = _file(env.ctx.attr.jvm_jar),
            ),
        ),
    )

    result = _jvm_deps_utils.jvm_deps(
        ctx = arrangment.fake_ctx,
        toolchains = strict_abi_configured_toolchains,
        associate_deps = arrangment.associate_deps,  # or None
        deps = arrangment.direct_deps,
    )

    classpath = env.expect.that_depset_of_files(result.compile_jars)

    # assert we have direct deps ABI jars and not the FULL FAT
    classpath.contains(_file(env.ctx.attr.direct_dep_abi_jar).short_path)
    classpath.not_contains(_file(env.ctx.attr.direct_dep_jar).short_path)

    # but FULL FAT associate jars and not the ABI associate jar
    classpath.contains(_file(env.ctx.attr.associate_jar).short_path)
    classpath.not_contains(_file(env.ctx.attr.associate_abi_jar).short_path)

def _fat_abi_test_impl(env, target):
    arrangment = _setup(env, target)

    fat_abi_configured_toolchains = struct(
        kt = struct(
            experimental_remove_private_classes_in_abi_jars = False,
            experimental_prune_transitive_deps = False,
            experimental_strict_associate_dependencies = False,
            jvm_stdlibs = JavaInfo(
                compile_jar = _file(env.ctx.attr.jvm_jar),
                output_jar = _file(env.ctx.attr.jvm_jar),
            ),
        ),
    )

    result = _jvm_deps_utils.jvm_deps(
        ctx = arrangment.fake_ctx,
        toolchains = fat_abi_configured_toolchains,
        associate_deps = arrangment.associate_deps,  # or None
        deps = arrangment.direct_deps,
    )

    classpath = env.expect.that_depset_of_files(result.compile_jars)

    # assert we have direct deps ABI jars and not the FULL FAT
    classpath.contains(_file(env.ctx.attr.direct_dep_abi_jar).short_path)
    classpath.not_contains(_file(env.ctx.attr.direct_dep_jar).short_path)

    # but ABI associate jars and not the FULL FAT associate jar
    classpath.contains(_file(env.ctx.attr.associate_abi_jar).short_path)
    classpath.not_contains(_file(env.ctx.attr.associate_jar).short_path)

def _transitive_from_exports_test_impl(env, target):
    arrangment_dict = _structs.to_dict(_setup(env, target))
    arrangment_dict.update(
        {"direct_deps": [
            {
                JavaInfo: JavaInfo(
                    exports = [arrangment_dict["associate_deps_java_info"]],
                    compile_jar = _file(env.ctx.attr.direct_dep_abi_jar),
                    output_jar = _file(env.ctx.attr.direct_dep_jar),
                ),
            },
            {
                JavaInfo: arrangment_dict["associate_deps_java_info"],
            },
        ]},
    )
    arrangment = struct(**arrangment_dict)

    strict_abi_configured_toolchains = struct(
        kt = struct(
            experimental_remove_private_classes_in_abi_jars = True,
            experimental_prune_transitive_deps = True,
            experimental_strict_associate_dependencies = True,
            jvm_stdlibs = JavaInfo(
                compile_jar = _file(env.ctx.attr.jvm_jar),
                output_jar = _file(env.ctx.attr.jvm_jar),
            ),
        ),
    )

    result = _jvm_deps_utils.jvm_deps(
        ctx = arrangment.fake_ctx,
        toolchains = strict_abi_configured_toolchains,
        associate_deps = arrangment.associate_deps,  # or None
        deps = arrangment.direct_deps,
    )

    classpath = env.expect.that_depset_of_files(result.compile_jars)

    # assert we have direct deps ABI jars and not the FULL FAT
    classpath.contains(_file(env.ctx.attr.direct_dep_abi_jar).short_path)
    classpath.not_contains(_file(env.ctx.attr.direct_dep_jar).short_path)

    # but FULL FAT associate jars and not the ABI associate jar
    classpath.contains(_file(env.ctx.attr.associate_jar).short_path)
    classpath.not_contains(_file(env.ctx.attr.associate_abi_jar).short_path)

def _transitive_from_associates_test_impl(env, target):
    ##
    # the one where target A associates with target B
    # target B has a dependency to target C
    # target A uses C, the world explodes
    # what this means is the associates transitive_compile_time_jars has this
    #            <generated file B.jar>,
    #            <generated file C.jar>,
    #            <source file various-sdk-libs.jar>
    # we will use the "direct jar" as a direct dep of the associate ie C
    ##
    associates_direct_deps = [
        JavaInfo(
            compile_jar = _file(env.ctx.attr.direct_dep_jar),
            output_jar = _file(env.ctx.attr.direct_dep_jar),
        ),
    ]

    associate_deps = [
        {
            JavaInfo: JavaInfo(
                compile_jar = _file(env.ctx.attr.associate_jar),
                output_jar = _file(env.ctx.attr.associate_jar),
                deps = associates_direct_deps,
            ),
            _KtJvmInfo: _KtJvmInfo(
                module_name = "associate_name",
            ),
        },
    ]

    fake_ctx = struct(
        label = target.label,
        attr = struct(
            module_name = "",
            tags = [],
        ),
    )

    nothing_configured_toolchains = struct(
        kt = struct(
            experimental_remove_private_classes_in_abi_jars = False,
            experimental_prune_transitive_deps = False,
            experimental_strict_associate_dependencies = False,
            jvm_stdlibs = JavaInfo(
                compile_jar = _file(env.ctx.attr.jvm_jar),
                output_jar = _file(env.ctx.attr.jvm_jar),
            ),
        ),
    )

    result = _jvm_deps_utils.jvm_deps(
        ctx = fake_ctx,
        toolchains = nothing_configured_toolchains,
        associate_deps = associate_deps,
        deps = [],
    )

    classpath = env.expect.that_depset_of_files(result.compile_jars)

    classpath.contains(_file(env.ctx.attr.direct_dep_jar).short_path)
    classpath.contains(_file(env.ctx.attr.associate_jar).short_path)

def _abi_test(name, impl):
    util.helper_target(
        native.filegroup,
        name = name + "_subject",
        srcs = [],
    )
    analysis_test(
        name = name,
        impl = impl,
        target = name + "_subject",
        attr_values = {
            "associate_abi_jar": util.empty_file(name + "associate_abi.jar"),
            "associate_jar": util.empty_file(name + "associate.jar"),
            "direct_dep_abi_jar": util.empty_file(name + "direct_dep_abi.jar"),
            "direct_dep_jar": util.empty_file(name + "direct_dep.jar"),
            "jvm_jar": util.empty_file(name + "jvm.jar"),
        },
        attrs = {
            "associate_abi_jar": attr.label(allow_files = True),
            "associate_jar": attr.label(allow_files = True),
            "direct_dep_abi_jar": attr.label(allow_files = True),
            "direct_dep_jar": attr.label(allow_files = True),
            "jvm_jar": attr.label(allow_files = True),
        },
    )

def _strict_abi_test(name):
    _abi_test(name, _strict_abi_test_impl)

def _fat_abi_test(name):
    _abi_test(name, _fat_abi_test_impl)

def _transitive_from_exports_test(name):
    _abi_test(name, _transitive_from_exports_test_impl)

def _transitive_from_associates_test(name):
    _abi_test(name, _transitive_from_associates_test_impl)

def _dep_infos_ordering_test_impl(env, target):
    """Test that user deps take precedence over stdlib in dep_infos ordering.

    This is a regression test for https://github.com/bazelbuild/rules_kotlin/issues/1368
    where the stdlib was placed first in the classpath, causing it to shadow user
    dependencies with conflicting classes (e.g., org.jetbrains.annotations.NotNull).
    """
    user_dep_java_info = JavaInfo(
        compile_jar = _file(env.ctx.attr.user_dep_jar),
        output_jar = _file(env.ctx.attr.user_dep_jar),
    )

    associate_java_info = JavaInfo(
        compile_jar = _file(env.ctx.attr.associate_jar),
        output_jar = _file(env.ctx.attr.associate_jar),
    )

    stdlib_java_info = JavaInfo(
        compile_jar = _file(env.ctx.attr.jvm_jar),
        output_jar = _file(env.ctx.attr.jvm_jar),
    )

    associate_deps = [
        {
            JavaInfo: associate_java_info,
            _KtJvmInfo: _KtJvmInfo(
                module_name = "associate_name",
            ),
        },
    ]

    user_deps = [
        {
            JavaInfo: user_dep_java_info,
        },
    ]

    fake_ctx = struct(
        label = target.label,
        attr = struct(
            module_name = "",
            tags = [],
        ),
    )

    toolchains = struct(
        kt = struct(
            experimental_remove_private_classes_in_abi_jars = False,
            experimental_prune_transitive_deps = False,
            experimental_strict_associate_dependencies = False,
            jvm_stdlibs = stdlib_java_info,
        ),
    )

    result = _jvm_deps_utils.jvm_deps(
        ctx = fake_ctx,
        toolchains = toolchains,
        associate_deps = associate_deps,
        deps = user_deps,
    )

    # Assert the ordering: result.deps should be [user_deps, associates, stdlib]
    env.expect.that_int(len(result.deps)).equals(3)

    # Verify compile_jars contains all deps in correct order
    classpath_list = result.compile_jars.to_list()
    user_dep_file = _file(env.ctx.attr.user_dep_jar)
    stdlib_file = _file(env.ctx.attr.jvm_jar)

    # Find indices of user dep and stdlib in classpath
    user_dep_idx = -1
    stdlib_idx = -1
    for idx, jar in enumerate(classpath_list):
        if jar == user_dep_file:
            user_dep_idx = idx
        if jar == stdlib_file:
            stdlib_idx = idx

    # Both should be found
    env.expect.that_bool(user_dep_idx >= 0).equals(True)
    env.expect.that_bool(stdlib_idx >= 0).equals(True)

    # User dep should come before stdlib in the classpath
    env.expect.that_bool(user_dep_idx < stdlib_idx).equals(True)

def _dep_infos_ordering_test(name):
    util.helper_target(
        native.filegroup,
        name = name + "_subject",
        srcs = [],
    )
    analysis_test(
        name = name,
        impl = _dep_infos_ordering_test_impl,
        target = name + "_subject",
        attr_values = {
            "associate_jar": util.empty_file(name + "associate.jar"),
            "jvm_jar": util.empty_file(name + "jvm.jar"),
            "user_dep_jar": util.empty_file(name + "user_dep.jar"),
        },
        attrs = {
            "associate_jar": attr.label(allow_files = True),
            "jvm_jar": attr.label(allow_files = True),
            "user_dep_jar": attr.label(allow_files = True),
        },
    )

def jvm_deps_test_suite(name):
    pass

    # TODO: update these test to use compiler.bzl classpath 
    # test_suite(
    #     name,
    #     tests = [
    #         _strict_abi_test,
    #         _fat_abi_test,
    #         _transitive_from_exports_test,
    #         _transitive_from_associates_test,
    #         _dep_infos_ordering_test,
    #     ],
    # )
