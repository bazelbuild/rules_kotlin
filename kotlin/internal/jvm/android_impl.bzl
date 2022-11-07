# Copyright 2018 The Bazel Authors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
load(
    "//kotlin/internal/jvm:compile.bzl",
    "export_only_providers",
    _compiler_toolchains = "compiler_toolchains_exposed",
    _get_kt_dep_infos = "get_kt_dep_infos",
    _jvm_deps = "jvm_deps_exposed",
    _kt_jvm_produce_output_jar_actions = "kt_jvm_produce_output_jar_actions",
)
load(
    "//kotlin/internal:defs.bzl",
    _TOOLCHAIN_TYPE = "TOOLCHAIN_TYPE",
)
load(
    "//kotlin/internal/jvm:impl.bzl",
    _write_launcher_action_exposed = "write_launcher_action_exposed",
)
load(
    "//kotlin/internal/jvm:external_java_utils.bzl",
    _resolve_package_from_label = "resolve_package_from_label",
)
load(
    "//kotlin/internal/jvm:associates.bzl",
    _associate_utils = "associate_utils",
)

load(
    ":android_resources.bzl",
    _process_resources = "process_resources",
    _process_resources_for_android_local_test  = "process_resources_for_android_local_test",
)
load(
    ":utils.bzl",
    "get_transitive_prerequisites",
    "utils"
)


def _collect_transitive_native_libs(ctx):
    infos = get_transitive_prerequisites(ctx, AndroidNativeLibsInfo)

    return depset(transitive = [
        i.native_libs
        for i in infos
    ])

def _collect_proguard_configs(ctx):
    """
    Collect transitive proguard configurations from dependencies
    """
    infos = get_transitive_prerequisites(ctx, ProguardSpecProvider, ["deps", "exports", "runtime_deps", "plugins"])

    return depset(ctx.files.proguard_specs, transitive = [
        i.specs
        for i in infos
    ])

def _make_legacy_android_provider(android_ide_info):
    # Create the ClassJar "object" for the target.android.idl.output field.
    if android_ide_info.idl_class_jar:
        idl_class_jar = struct(
            class_jar = android_ide_info.idl_class_jar,
            ijar = None,
            source_jar = android_ide_info.idl_source_jar,
        )
    else:
        idl_class_jar = None

    return struct(
        aar = android_ide_info.aar,
        apk = android_ide_info.signed_apk,
        apks_under_test = android_ide_info.apks_under_test,
        defines_resources = android_ide_info.defines_android_resources,
        idl = struct(
            import_root = android_ide_info.idl_import_root,
            sources = android_ide_info.idl_srcs,
            generated_java_files = android_ide_info.idl_generated_java_files,
            output = idl_class_jar,
        ),
        java_package = android_ide_info.java_package,
        manifest = android_ide_info.manifest,
        merged_manifest = android_ide_info.generated_manifest,
        native_libs = android_ide_info.native_libs,
        resource_apk = android_ide_info.resource_apk,
        resource_jar = android_ide_info.resource_jar,
    )

def kt_android_library_impl(ctx):
    java_package = _resolve_package_from_label(ctx.label, ctx.attr.custom_package, fallback = False)

    resources = None
    localRClass = None
    generated_manifest = None

    resources = _process_resources(ctx, java_package)

    if ctx.attr.resource_files and resources.r_java:
        localRClass = utils.only(utils.list_or_depset_to_list(resources.r_java.compile_jars))

    if resources.merged_manifest:
        generated_manifest = resources.merged_manifest

    resources_providers = resources.providers

    # Setup the compile action.
    providers = kt_android_produce_jar_actions(ctx, "kt_jvm_library", localRClass)

    defines_resources = bool(
        ctx.attr.manifest or
        ctx.attr.resource_files or
        ctx.attr.assets or
        ctx.attr.assets_dir or
        ctx.attr.exports_manifest,
    )

    resource_jar = None
    if localRClass:
        resource_jar = _getLocalRClass(localRClass).outputs.jars[0]

    # TODO Add the rest of the missing fields to further improve IDE experience
    android_ide_info = AndroidIdeInfo(
        java_package,  # java_package
        ctx.file.manifest,  # manifest
        generated_manifest,  # generated_manifest
        # TODO add AIDL support: https://jira.sc-corp.net/browse/BUILD-707
        None,  # idl_import_root
        [],  # idl_srcs
        [],  # idl_generated_java_files
        None,  # idl_source_jar
        None,  # idl_class_jar
        defines_resources,  # defines_android_resources
        resource_jar,  # resource_jar
        None,  # resources_apk
        None,  # signed_apk, always empty for kt_android_library.
        None,  # aar
        [],  # apks_under_test, always empty for kt_android_library
        dict(),  # nativelibs, always empty for kt_android_library
    )

    files = [ctx.outputs.jar]
    if  providers.java.outputs.jdeps:
        files.append(providers.java.outputs.jdeps)

    result = struct(
        kt = providers.kt,
        android = _make_legacy_android_provider(android_ide_info),
        providers = resources_providers + [
            providers.java,
            providers.kt,
            providers.instrumented_files,
            AndroidNativeLibsInfo(_collect_transitive_native_libs(ctx)),
            ProguardSpecProvider(_collect_proguard_configs(ctx)),
            DefaultInfo(
                files = depset(files),
                runfiles = ctx.runfiles(
                    # explicitly include data files, otherwise they appear to be missing
                    files = ctx.files.data,
                    transitive_files = depset(order = "default"),
                    # continue to use collect_default until proper transitive data collecting is
                    # implemented.
                    collect_default = True,
                ),
            ),
        ],
    )
    return result

def _getAndroidSdkJar(ctx):
    android_jar = ctx.attr.android_sdk[AndroidSdkInfo].android_jar
    return JavaInfo(output_jar = android_jar, compile_jar = android_jar, neverlink = True)

def _getLocalRClass(rClass):
    return JavaInfo(output_jar = rClass, compile_jar = rClass, neverlink = True)


_SPLIT_STRINGS = [
    "src/test/java/",
    "src/test/kotlin/",
    "javatests/",
    "kotlin/",
    "java/",
    "test/",
]

def kt_android_local_test_impl(ctx):
    # Android resource processing
    java_package = _resolve_package_from_label(ctx.label, ctx.attr.custom_package, fallback = False)

    resources_ctx = _process_resources_for_android_local_test(ctx, java_package)
    resources_apk = resources_ctx.resources_apk
    resources_jar = resources_ctx.class_jar

    # Generate properties file telling Robolectric from where to load resources.
    test_config_file = ctx.actions.declare_file("_robolectric/" + ctx.label.name + "_test_config.properties")
    ctx.actions.write(
        test_config_file,
        content = "android_resource_apk=" + resources_apk.short_path,
    )

    # Setup the compile action.
    providers = kt_android_produce_jar_actions(
        ctx,
        "kt_jvm_library",
        extra_resources = {"com/android/tools/test_config.properties": test_config_file},
    )

    # Create test run action
    runtime_jars = depset(
        ctx.files._bazel_test_runner + [resources_jar] + [ctx.attr.android_sdk[AndroidSdkInfo].android_jar],
        transitive = [providers.java.transitive_runtime_jars],
    )
    coverage_runfiles = []
    if ctx.configuration.coverage_enabled:
        jacocorunner = ctx.toolchains[_TOOLCHAIN_TYPE].jacocorunner
        coverage_runfiles = jacocorunner.files.to_list()

    test_class = ctx.attr.test_class

    # If no test_class, do a best-effort attempt to infer one.
    if not bool(ctx.attr.test_class):
        for file in ctx.files.srcs:
            package_relative_path = file.path.replace(ctx.label.package + "/", "")
            if package_relative_path.split(".")[0] == ctx.attr.name:
                for splitter in _SPLIT_STRINGS:
                    elements = file.short_path.split(splitter, 1)
                    if len(elements) == 2:
                        test_class = elements[1].split(".")[0].replace("/", ".")
                        break

    coverage_metadata = _write_launcher_action_exposed(
        ctx,
        runtime_jars,
        main_class = ctx.attr.main_class,
        jvm_flags = [
            "-ea",
            "-Dbazel.test_suite=%s" % test_class,
            "-Drobolectric.offline=true",
            "-Drobolectric-deps.properties=" + _get_android_all_jars_properties_file(ctx).short_path,
        ] + ctx.attr.jvm_flags,
    )

    files = [ctx.outputs.jar]
    if  providers.java.outputs.jdeps:
        files.append(providers.java.outputs.jdeps)

    return struct(
        kt = providers.kt,
        providers = [
            providers.java,
            providers.kt,
            providers.instrumented_files,
            DefaultInfo(
                files = depset(files),
                runfiles = ctx.runfiles(
                    # Explicitly include data files, otherwise they appear to be missing
                    # Include resources apk required by Robolectric
                    files = ctx.files.data + [resources_apk],
                    transitive_files = depset(
                        order = "default",
                        transitive = [runtime_jars, depset(coverage_runfiles), depset(coverage_metadata)],
                        direct = ctx.files._java_runtime,
                    ),
                    # continue to use collect_default until proper transitive data collecting is
                    # implmented.
                    collect_default = True,
                ),
            ),
        ],
    )

def _get_android_all_jars_properties_file(ctx):
    runfiles = ctx.runfiles(collect_data = True).files.to_list()
    for run_file in runfiles:
        if run_file.basename == "robolectric-deps.properties":
            return run_file
    fail("'robolectric-deps.properties' not found in the deps of the rule.")

def kt_android_produce_jar_actions(ctx, rule_kind, rClass = None, extra_resources = {}):
    """Setup The actions to compile a jar and if any resources or resource_jars were provided to merge these in with the
    compilation output.

    Returns:
        see `kt_jvm_compile_action`.
    """

    toolchains = _compiler_toolchains(ctx)
    associates = _associate_utils.get_associates(ctx)
    dep_infos = _get_kt_dep_infos(
        toolchains,
        associates.targets,
        deps = ctx.attr.deps,
    )
    android_dep_infos = [_getAndroidSdkJar(ctx)]
    if rClass:
        android_dep_infos.append(_getLocalRClass(rClass))

    compile_deps = _jvm_deps(ctx, android_dep_infos + dep_infos, ctx.attr.runtime_deps)

    # Reset the field used to populate the returned JavaInfo provider as we don't want to include
    # other jars from AndroidResourceClassProvider.
    compile_deps.provided_deps.clear()
    compile_deps.provided_deps.extend(dep_infos)

    # Setup the compile action.
    return _kt_jvm_produce_output_jar_actions(
        ctx,
        rule_kind = rule_kind,
        compile_deps = compile_deps,
        extra_resources = extra_resources,
    ) if ctx.attr.srcs else export_only_providers(
        ctx = ctx,
        actions = ctx.actions,
        outputs = ctx.outputs,
        attr = ctx.attr,
    )
