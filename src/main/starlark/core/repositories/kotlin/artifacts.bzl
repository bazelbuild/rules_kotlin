"""A map of label to artifacts made available by the kotlinc github repo"""

KOTLINC_ARTIFACTS = struct(
    js = struct(
        plugin = {},
        runtime = {
            "kotlin-stdlib-js": "lib/kotlin-stdlib-js.jar",
            "kotlin-stdlib-js-sources": "lib/kotlin-stdlib-js-sources.jar",
            "kotlin-test-js": "lib/kotlin-test-js.jar",
            "kotlin-test-js-sources": "lib/kotlin-test-js-sources.jar",
        },
        compile = {},
    ),
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
            "js_engines": "lib/js_engines.jar",
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

KOTLINC_ARTIFACT_LIST = {
    label: file
    for lang in ["js", "jvm", "core"]
    for type in ["compile", "plugin", "runtime"]
    for (label, file) in getattr(getattr(KOTLINC_ARTIFACTS, lang), type).items()
}
