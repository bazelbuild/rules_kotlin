load("@rules_java//java:defs.bzl", "java_library")
licenses(["notice"])

java_library(
    name = "maven_artifact",
    visibility = [
        "//third_party/jvm:__subpackages__",
    ],
    exports = [
        "//external:jar/io_bazel_rules_kotlin_org/apache/maven/maven_artifact",
    ],
    runtime_deps = [
        "//third_party/jvm/org/apache/commons:commons_lang3",
        "//third_party/jvm/org/codehaus/plexus:plexus_utils",
    ],
)

java_library(
    name = "maven_model",
    visibility = [
        "//third_party/jvm:__subpackages__",
    ],
    exports = [
        "//external:jar/io_bazel_rules_kotlin_org/apache/maven/maven_model",
    ],
    runtime_deps = [
        "//third_party/jvm/org/apache/commons:commons_lang3",
        "//third_party/jvm/org/codehaus/plexus:plexus_utils",
    ],
)

java_library(
    name = "maven_plugin_api",
    visibility = [
        "//third_party/jvm:__subpackages__",
    ],
    exports = [
        "//external:jar/io_bazel_rules_kotlin_org/apache/maven/maven_plugin_api",
    ],
    runtime_deps = [
        ":maven_artifact",
        ":maven_model",
        "//third_party/jvm/org/eclipse/sisu:org_eclipse_sisu_plexus",
    ],
)
