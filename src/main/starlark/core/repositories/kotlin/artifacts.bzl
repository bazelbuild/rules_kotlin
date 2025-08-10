"""A map of label to artifacts made available by the kotlinc github repo"""

KOTLINC_ARTIFACTS = struct(
    jvm = struct(
        plugin = {
            "allopen-compiler-plugin": "lib/allopen-compiler-plugin.jar",
            "assignment-compiler-plugin": "lib/assignment-compiler-plugin.jar",
            "kotlin-imports-dumper-compiler-plugin": "lib/kotlin-imports-dumper-compiler-plugin.jar",
            "kotlin-serialization-compiler-plugin": "lib/kotlin-serialization-compiler-plugin.jar",
            "kotlinx-serialization-compiler-plugin": "lib/kotlinx-serialization-compiler-plugin.jar",
            "lombok-compiler-plugin": "lib/lombok-compiler-plugin.jar",
            "mutability-annotations-compat": "lib/mutability-annotations-compat.jar",
            "noarg-compiler-plugin": "lib/noarg-compiler-plugin.jar",
            "sam-with-receiver-compiler-plugin": "lib/sam-with-receiver-compiler-plugin.jar",
            "parcelize-compiler-plugin": "lib/parcelize-compiler.jar",
        },
        runtime = {
            "jvm-abi-gen": "lib/jvm-abi-gen.jar",
            "kotlin-stdlib": "lib/kotlin-stdlib.jar",
            "kotlin-stdlib-jdk7": "lib/kotlin-stdlib-jdk7.jar",
            "kotlin-stdlib-jdk7-sources": "lib/kotlin-stdlib-jdk7-sources.jar",
            "kotlin-stdlib-jdk8": "lib/kotlin-stdlib-jdk8.jar",
            "kotlin-stdlib-jdk8-sources": "lib/kotlin-stdlib-jdk8-sources.jar",
            "kotlin-stdlib-sources": "lib/kotlin-stdlib-sources.jar",
            "kotlin-test-junit": "lib/kotlin-test-junit.jar",
            "kotlin-test-junit-sources": "lib/kotlin-test-junit-sources.jar",
            "kotlin-test-junit5": "lib/kotlin-test-junit5.jar",
            "kotlin-test-junit5-sources": "lib/kotlin-test-junit5-sources.jar",
            "kotlin-test-testng": "lib/kotlin-test-testng.jar",
            "parcelize-runtime": "lib/parcelize-runtime.jar",
        },
        compile = {},
    ),
    core = struct(
        plugin = {},
        runtime = {
            "kotlin-reflect": "lib/kotlin-reflect.jar",
            "kotlin-reflect-sources": "lib/kotlin-reflect-sources.jar",
            "kotlin-script-runtime": "lib/kotlin-script-runtime.jar",
            "kotlin-script-runtime-sources": "lib/kotlin-script-runtime-sources.jar",
            "kotlin-test": "lib/kotlin-test.jar",
            "kotlin-test-sources": "lib/kotlin-test-sources.jar",
            "kotlin-test-testng-sources": "lib/kotlin-test-testng-sources.jar",
            "kotlin-preloader": "lib/kotlin-preloader.jar",
        },
        compile = {
            "android-extensions-compiler": "lib/android-extensions-compiler.jar",
            "android-extensions-runtime": "lib/android-extensions-runtime.jar",
            "annotations": "lib/annotations-13_0.jar",
            "kotlin-annotation-processing": "lib/kotlin-annotation-processing.jar",
            "kotlin-annotation-processing-cli": "lib/kotlin-annotation-processing-cli.jar",
            "kotlin-annotation-processing-compiler": "lib/kotlin-annotation-processing-compiler.jar",
            "kotlin-annotation-processing-runtime": "lib/kotlin-annotation-processing-runtime.jar",
            "kotlin-annotations-jvm": "lib/kotlin-annotations-jvm.jar",
            "kotlin-annotations-jvm-sources": "lib/kotlin-annotations-jvm-sources.jar",
            "kotlin-compiler": "lib/kotlin-compiler.jar",
            "kotlin-daemon": "lib/kotlin-daemon.jar",
            "kotlin-daemon-client": "lib/kotlin-daemon-client.jar",
            "kotlin-main-kts": "lib/kotlin-main-kts.jar",
            "kotlin-runner": "lib/kotlin-runner.jar",
            "kotlin-scripting-common": "lib/kotlin-scripting-common.jar",
            "kotlin-scripting-compiler": "lib/kotlin-scripting-compiler.jar",
            "kotlin-scripting-compiler-impl": "lib/kotlin-scripting-compiler-impl.jar",
            "kotlin-scripting-jvm": "lib/kotlin-scripting-jvm.jar",
            "kotlinx-coroutines-core-jvm": "lib/kotlinx-coroutines-core-jvm.jar",
            "parcelize-compiler": "lib/parcelize-compiler.jar",
            "scripting-compiler": "lib/scripting-compiler.jar",
            "trove4j": "lib/trove4j.jar",
        },
    ),
)

KOTLIN_NATIVE_ARTIFACTS_AND_TARGETS = {
    "linux_x86_64": struct(
        plugin = {},
        compile = {
            "kotlin-native-linux-x86_64": "konan/lib/kotlin-native.jar",
            "trove4j-linux-x86_64": "konan/lib/trove4j.jar",
        },
        runtime = {},
        exec_compatible_with = ["@platforms//os:linux", "@platforms//cpu:x86_64"],
        # the targets have been extracted here manually by either running kotlinc-native -list-targets for the relevant distribution
        # or listing entries under <konan.home>/targets/, and then map Bazel platforms for it so that we can create the relevant toolchains
        # with the right target_compatible_with
        targets = {
            ("@platforms//os:android", "@platforms//cpu:armv7"): ["android_arm32"],
            ("@platforms//os:android", "@platforms//cpu:arm64"): ["android_arm64"],
            ("@platforms//os:android", "@platforms//cpu:x86_64"): ["android_x64"],
            ("@platforms//os:linux", "@platforms//cpu:armv7"): ["linux_arm32_hfp"],
            ("@platforms//os:linux", "@platforms//cpu:x86_64"): ["linux_x64"],
            ("@platforms//os:windows", "@platforms//cpu:x86_64"): ["mingw_x64"],
        },
    ),
    "macos_x86_64": struct(
        plugin = {},
        compile = {
            "kotlin-native-macos-x86_64": "konan/lib/kotlin-native.jar",
            "trove4j-macos-x86_64": "konan/lib/trove4j.jar",
        },
        runtime = {},
        exec_compatible_with = ["@platforms//os:macos", "@platforms//cpu:x86_64"],
        targets = {
            ("@platforms//os:android", "@platforms//cpu:armv7"): ["android_arm32"],
            ("@platforms//os:android", "@platforms//cpu:arm64"): ["android_arm64"],
            ("@platforms//os:android", "@platforms//cpu:x86_64"): ["android_x64"],
            ("@platforms//os:android", "@platforms//cpu:x86_32"): ["android_x86"],
            ("@platforms//os:ios", "@platforms//cpu:arm64"): ["ios_arm64", "ios_simulator_arm64"],
            ("@platforms//os:ios", "@platforms//cpu:x86_64"): ["ios_x64"],
            ("@platforms//os:linux", "@platforms//cpu:armv7"): ["linux_arm32_hfp"],
            ("@platforms//os:linux", "@platforms//cpu:arm64"): ["linux_arm64"],
            ("@platforms//os:linux", "@platforms//cpu:x86_64"): ["linux_x64"],
            ("@platforms//os:macos", "@platforms//cpu:arm64"): ["macos_arm64"],
            ("@platforms//os:macos", "@platforms//cpu:x86_64"): ["macos_x64"],
            ("@platforms//os:windows", "@platforms//cpu:x86_64"): ["mingw_x64"],
            ("@platforms//os:tvos", "@platforms//cpu:arm64"): ["tvos_arm64", "tvos_simulator_arm64"],
            ("@platforms//os:tvos", "@platforms//cpu:x86_64"): ["tvos_x64"],
            ("@platforms//os:watchos", "@platforms//cpu:armv7k"): ["watchos_arm32"],
            ("@platforms//os:watchos", "@platforms//cpu:arm64"): ["watchos_arm64", "watchos_simulator_arm64"],
            ("@platforms//os:watchos", "@platforms//cpu:arm64_32"): ["watchos_device_arm64"],
            ("@platforms//os:watchos", "@platforms//cpu:x86_64"): ["watchos_x64"],
        },
    ),
    "macos_aarch64": struct(
        plugin = {},
        compile = {
            "kotlin-native-macos_aarch64": "konan/lib/kotlin-native.jar",
            "trove4j-macos_aarch64": "konan/lib/trove4j.jar",
        },
        runtime = {},
        exec_compatible_with = ["@platforms//os:macos", "@platforms//cpu:arm64"],
        targets = {
            ("@platforms//os:linux", "@platforms//cpu:x86_64"): ["linux_x64"],
            ("@platforms//os:linux", "@platforms//cpu:arm64"): ["linux_arm64"],
            ("@platforms//os:windows", "@platforms//cpu:x86_64"): ["mingw_x64"],
            ("@platforms//os:android", "@platforms//cpu:x86_32"): ["android_x86"],
            ("@platforms//os:android", "@platforms//cpu:x86_64"): ["android_x64"],
            ("@platforms//os:android", "@platforms//cpu:armv7"): ["android_arm32"],
            ("@platforms//os:android", "@platforms//cpu:arm64"): ["android_arm64"],
            ("@platforms//os:macos", "@platforms//cpu:x86_64"): ["macos_x64"],
            ("@platforms//os:macos", "@platforms//cpu:arm64"): ["macos_arm64"],
            ("@platforms//os:ios", "@platforms//cpu:arm64"): ["ios_arm64", "ios_simulator_arm64"],
            ("@platforms//os:ios", "@platforms//cpu:x86_64"): ["ios_x64"],
            ("@platforms//os:watchos", "@platforms//cpu:armv7k"): ["watchos_arm32"],
            ("@platforms//os:watchos", "@platforms//cpu:arm64"): ["watchos_arm64", "watchos_simulator_arm64"],
            ("@platforms//os:watchos", "@platforms//cpu:arm64_32"): ["watchos_device_arm64"],
            ("@platforms//os:watchos", "@platforms//cpu:x86_64"): ["watchos_x64"],
            ("@platforms//os:tvos", "@platforms//cpu:arm64"): ["tvos_arm64", "tvos_simulator_arm64"],
            ("@platforms//os:tvos", "@platforms//cpu:x86_64"): ["tvos_x64"],
        },
    ),
    "windows_x86_64": struct(
        plugin = {},
        compile = {
            "kotlin-native-windows_x86_64": "konan/lib/kotlin-native.jar",
            "trove4j-windows_x86_64": "konan/lib/trove4j.jar",
        },
        runtime = {},
        exec_compatible_with = ["@platforms//os:windows", "@platforms//cpu:x86_64"],
        targets = {
            ("@platforms//os:windows", "@platforms//cpu:x86_64"): ["mingw_x64"],
            ("@platforms//os:android", "@platforms//cpu:armv7"): ["android_arm32"],
            ("@platforms//os:android", "@platforms//cpu:arm64"): ["android_arm64"],
            ("@platforms//os:android", "@platforms//cpu:x86_32"): ["android_x86"],
            ("@platforms//os:android", "@platforms//cpu:x86_64"): ["android_x64"],
            ("@platforms//os:linux", "@platforms//cpu:x86_64"): ["linux_64"],
        },
    ),
}

KOTLINC_ARTIFACT_LIST = {
    label: file
    for lang in ["jvm", "core"]
    for type in ["compile", "plugin", "runtime"]
    for (label, file) in getattr(getattr(KOTLINC_ARTIFACTS, lang), type).items()
}

KOTLIN_NATIVE_ARTIFACT_LIST = {
    label: file
    for platform in KOTLIN_NATIVE_ARTIFACTS_AND_TARGETS.keys()
    for type in ["compile", "plugin", "runtime"]
    for (label, file) in getattr(KOTLIN_NATIVE_ARTIFACTS_AND_TARGETS[platform], type).items()
}
