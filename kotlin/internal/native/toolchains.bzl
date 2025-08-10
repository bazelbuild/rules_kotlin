load("@bazel_skylib//rules/directory:providers.bzl", "DirectoryInfo")
load("//src/main/starlark/core/repositories/kotlin:artifacts.bzl", "KOTLIN_NATIVE_ARTIFACTS_AND_TARGETS")

KotlinNativeToolchainInfo = provider(fields = {
    "konan_home": "The path to konan.home directory",
    "konan_properties": "The kokna.properties file corresponding to this distribution",
    "konan_home_files": "A depset containing all the files in konan.home",
    "targets": "A list of Kotlin native targets this toolchain supports",
    "target": "The target chosen for compilation and to be passed as --target",
})

def _kt_native_toolchain_impl(ctx):
    konan_home_info = ctx.attr.konan_home[DirectoryInfo]
    konan_properties = konan_home_info.get_file("konan/konan.properties")
    konan_home = konan_home_info

    return [
        platform_common.ToolchainInfo(
            kotlin_native_info = KotlinNativeToolchainInfo(
                konan_home = konan_home,
                konan_home_files = depset(transitive = [konan_home_info.transitive_files]),
                konan_properties = konan_properties,
                targets = ctx.attr.targets,
                target = ctx.attr.target,
            ),
        ),
    ]

kt_native_toolchain = rule(
    implementation = _kt_native_toolchain_impl,
    attrs = {
        "konan_home": attr.label(
            providers = [DirectoryInfo],
            doc = "The directory containing konan.home",
        ),
        "targets": attr.string_list(
            doc = "The list of targets supported by this toolchain. Each target can be passed to kotlinc-native with -target option",
            default = [],
        ),
        "target": attr.string(
            doc = "The Kotlin native target for this toolchain. This corresponds to the --target argument passed to the kotlin native compiler",
        ),
    },
    provides = [platform_common.ToolchainInfo],
)

def kt_configure_native_toolchains():
    """Defines and registers the default toolchains for the kotlin-native compiler for all the platforms and targets supported."""
    native.toolchain_type(
        name = "kt_native_toolchain_type",
        visibility = ["//visibility:public"],
    )

    # Create toolchains for each exec platform and their supported targets
    for exec_platform, artifacts_and_targets in KOTLIN_NATIVE_ARTIFACTS_AND_TARGETS.items():
        exec_compatible_with = artifacts_and_targets.exec_compatible_with
        for target_constraint_tuple, kotlin_native_targets in artifacts_and_targets.targets.items():
            target_os, target_cpu = target_constraint_tuple

            # Create a unique name for this toolchain
            toolchain_suffix = "{}_to_{}_{}".format(
                exec_platform,
                Label(target_os).name,
                Label(target_cpu).name,
            )
            toolchain_name = "default_kt_native_toolchain_{}".format(toolchain_suffix)
            toolchain_impl = "default_kt_native_{}".format(toolchain_suffix)
            default_target = artifacts_and_targets.targets[target_constraint_tuple][0]

            # Create the toolchain implementation
            kt_native_toolchain(
                name = toolchain_impl,
                konan_home = "@com_github_jetbrains_kotlin_native_{}//:konan_home".format(exec_platform),
                targets = kotlin_native_targets,
                # TODO: provide a way to override this for this target platform with a config setting
                target = default_target,
            )

            # Register the toolchain
            native.toolchain(
                name = toolchain_name,
                exec_compatible_with = exec_compatible_with,
                target_compatible_with = [target_os, target_cpu],
                toolchain = ":" + toolchain_impl,
                toolchain_type = ":kt_native_toolchain_type",
            )
