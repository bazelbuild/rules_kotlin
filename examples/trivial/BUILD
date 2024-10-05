load("@rules_java//java:defs.bzl", "java_library")
load("@rules_kotlin//kotlin:lint.bzl", "ktlint_config")

package(default_visibility = ["//visibility:public"])

java_library(
    name = "java_deps",
    exports = [
        "@maven//:com_expedia_graphql_kotlin",
        "@maven//:com_expedia_graphql_kotlin_schema_generator",
        "@maven//:com_graphql_java_graphql_java",
        "@maven//:org_apache_logging_log4j_log4j_core",
        "@maven//:org_apiguardian_apiguardian_api",
        "@maven//:org_opentest4j_opentest4j",
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
