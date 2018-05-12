load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_test")

kt_jvm_library(
    name = "foo",
    srcs = ["Foo.kt"],
    internal_coverage_instrumented = True,
)

kt_jvm_test(
    name = "foo_test",
    srcs = ["FooTest.kt"],
    test_class = "simple.FooTest",
    deps = [
        ":foo",
        "@io_bazel_rules_kotlin_junit_junit//jar",
    ],
    size = "small",
    internal_coverage_enabled = True,
)

filegroup(
    name = "cases",
    srcs = [
        ":foo_test",
    ],
    visibility=["//tests/integrationtests:__subpackages__"],
    testonly = True,
)
