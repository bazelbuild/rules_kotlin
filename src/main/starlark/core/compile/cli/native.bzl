load("@rules_java//java/common:java_info.bzl", "JavaInfo")
load("//kotlin/internal:defs.bzl", "KT_NATIVE_COMPILER_REPO_PREFIX", "KtJvmInfo")
load("//src/main/starlark/core/compile:common.bzl", "NATIVE_TYPE")
load("//src/main/starlark/core/repositories/kotlin:artifacts.bzl", "KOTLIN_NATIVE_TARGETS")

KotlinNativeCompileInfo = provider(
    doc = "Provies the necessary info about the Kotlin native CLI toolchain",
    fields = {
        "native_java_infos": "A list of JavaInfo objects that represent the kotlin-native CLI toolchain",
        "jvm_flags": "A list of JVM flags to be used with kotlinc-native CLI toolchain",
    },
)

def _kotlin_native_cli_toolchain_impl(ctx):
    native_java_infos = [j[JavaInfo] for j in ctx.attr.native_jars]
    return [
        platform_common.ToolchainInfo(
            kotlin_native_compile_info = KotlinNativeCompileInfo(
                native_java_infos = native_java_infos,
                jvm_flags = ctx.attr.jvm_flags,
            ),
        ),
    ]

kotlin_native_cli_toolchain = rule(
    implementation = _kotlin_native_cli_toolchain_impl,
    attrs = {
        "native_jars": attr.label_list(
            doc = "One ore more jars that are required to run the kotlinc-native CLI",
            providers = [KtJvmInfo],
        ),
        "jvm_flags": attr.string_list(
            doc = "A list of JVM flags to be used in binaries to use the kotlinc-native CLI toolchain",
            default = [],
        ),
    },
)

def define_native_cli_toolchains():
    # Ideally this would not be needed and be available from the toolchain
    # but the builder uses java_binary and jvm_flags are propagated to it directly
    # so it doesn't seem straightforward to do that, so this alias target allows us
    # to reference it in the data dependencies there
    native.alias(
        name = "kotlin-native",
        actual = select({
            "@bazel_tools//src/conditions:linux_x86_64": "@" + KT_NATIVE_COMPILER_REPO_PREFIX + "_" + "linux_x86_64//:kotlin-native",
            "@bazel_tools//src/conditions:darwin": "@" + KT_NATIVE_COMPILER_REPO_PREFIX + "_" + "macos_x86_64//:kotlin-native",
            "@bazel_tools//src/conditions:windows": "@" + KT_NATIVE_COMPILER_REPO_PREFIX + "_" + "windows_x86_64//:kotlin-native",
            "@bazel_tools//src/conditions:darwin_arm64": "@" + KT_NATIVE_COMPILER_REPO_PREFIX + "_" + "macos_aarch64//:kotlin-native",
        }),
        visibility = ["//src:__subpackages__"],
    )
    for exec_platform, targets in KOTLIN_NATIVE_TARGETS.items():
        exec_compatible_with = targets.exec_compatible_with

        for target_constraint_tuple in targets.targets.keys():
            target_os, target_cpu = target_constraint_tuple

            # Create a unique name for this toolchain
            toolchain_suffix = "{}_to_{}_{}".format(
                exec_platform,
                Label(target_os).name,
                Label(target_cpu).name,
            )
            toolchain_name = "default_kt_native_toolchain_{}".format(toolchain_suffix)
            toolchain_impl = "default_kt_native_{}".format(toolchain_suffix)

            kotlin_native = Label("@" + KT_NATIVE_COMPILER_REPO_PREFIX + "_" + exec_platform + "//:kotlin-native")

            # Create the toolchain implementation
            kotlin_native_cli_toolchain(
                name = toolchain_impl,
                native_jars = [
                    kotlin_native,
                    "@" + KT_NATIVE_COMPILER_REPO_PREFIX + "_" + exec_platform + "//:trove4j",
                ],
            )

            # Register the toolchain
            native.toolchain(
                name = toolchain_name,
                exec_compatible_with = exec_compatible_with,
                target_compatible_with = [target_os, target_cpu],
                toolchain = ":" + toolchain_impl,
                toolchain_type = NATIVE_TYPE,
            )
