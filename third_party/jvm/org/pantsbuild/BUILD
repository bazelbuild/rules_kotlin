licenses(["notice"])

java_library(
    name = "jarjar",
    visibility = [
        "//visibility:public",
    ],
    exports = [
        "//external:jar/io_bazel_rules_kotlin_org/pantsbuild/jarjar",
    ],
    runtime_deps = [
        "//third_party/jvm/org/apache/ant",
        "//third_party/jvm/org/apache/maven:maven_plugin_api",
        "//third_party/jvm/org/ow2/asm",
        "//third_party/jvm/org/ow2/asm:asm_commons",
    ],
)
