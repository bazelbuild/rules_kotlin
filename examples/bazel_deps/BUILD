load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_binary" , "kt_jvm_test")

kt_jvm_binary(
    name = "example",
    main_class = "x.ExampleKt",
    srcs = glob(["src/main/**/*.kt"]),
    deps = [
        "//third_party/jvm/org/jetbrains/kotlinx:kotlinx_coroutines_core",
        "//third_party/jvm/org/jetbrains/kotlinx:kotlinx_coroutines_jdk8",
    ],
)

kt_jvm_test(
    name = "tests",
    srcs = glob(["src/test/x/ExampleTest.kt"]),
    # Must specify a single test class or a junit Suite
    # For java_test https://github.com/bazelbuild/bazel/issues/1017 has background
    test_class = "x.ExampleTest",
    deps = [
        ":example",
        "//third_party/jvm/org/jetbrains/kotlinx:kotlinx_coroutines_core",
        "//third_party/jvm/junit:junit",
    ],
)
