load("@rules_java//java:defs.bzl", "java_library")
licenses(["notice"])

java_library(
    name = "org_eclipse_sisu_inject",
    visibility = [
        "//third_party/jvm:__subpackages__",
    ],
    exports = [
        "//external:jar/io_bazel_rules_kotlin_org/eclipse/sisu/org_eclipse_sisu_inject",
    ],
)

java_library(
    name = "org_eclipse_sisu_plexus",
    visibility = [
        "//third_party/jvm:__subpackages__",
    ],
    exports = [
        "//external:jar/io_bazel_rules_kotlin_org/eclipse/sisu/org_eclipse_sisu_plexus",
    ],
    runtime_deps = [
        ":org_eclipse_sisu_inject",
        "//third_party/jvm/javax/enterprise:cdi_api",
        "//third_party/jvm/org/codehaus/plexus:plexus_classworlds",
        "//third_party/jvm/org/codehaus/plexus:plexus_component_annotations",
        "//third_party/jvm/org/codehaus/plexus:plexus_utils",
    ],
)
