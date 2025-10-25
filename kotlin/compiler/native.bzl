load("//kotlin/internal:defs.bzl", "KT_NATIVE_COMPILER_REPO_PREFIX")

def kt_configure_native_compiler():
    # Ideally this would not be needed here and be available from the toolchain
    # but the builder uses java_binary and jvm_flags are propagated for the runfile paths to it directly
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
