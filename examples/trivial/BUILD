load("@rules_java//java:defs.bzl", "java_library")
load("@rules_kotlin//kotlin:lint.bzl", "ktlint_config")

package(default_visibility = ["//visibility:public"])

java_library(
    name = "java_deps",
    exports = [
        "@maven_rules_kotlin_example//:com_expedia_graphql_kotlin",
        "@maven_rules_kotlin_example//:com_expedia_graphql_kotlin_schema_generator",
        "@maven_rules_kotlin_example//:com_graphql_java_graphql_java",
        "@maven_rules_kotlin_example//:org_apache_logging_log4j_log4j_core",
        "@maven_rules_kotlin_example//:org_apiguardian_apiguardian_api",
        "@maven_rules_kotlin_example//:org_opentest4j_opentest4j",
    ],
)

filegroup(
    name = "editorconfig",
    srcs = [".editorconfig"],
)

ktlint_config(
    name = "ktlint_editorconfig",
    android_rules_enabled = False,
    editorconfig = "//:editorconfig",
    experimental_rules_enabled = False,
    visibility = ["//visibility:public"],
)

sh_test(
    name = "test_execution",
    srcs = ["test_execution.sh"],
    args = [
        "$(location //app:myapp)",
    ],
    data = [
        "//app:myapp",
        "@bazel_tools//tools/bash/runfiles",
    ],
    deps = [],
)
