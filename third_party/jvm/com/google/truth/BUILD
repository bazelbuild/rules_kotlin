licenses(["notice"])

java_library(
    name = "truth",
    visibility = [
        "//visibility:public",
    ],
    exports = [
        "//external:jar/io_bazel_rules_kotlin_com/google/truth/truth",
    ],
    runtime_deps = [
        "//third_party/jvm/com/google/auto/value:auto_value_annotations",
        "//third_party/jvm/com/google/errorprone:error_prone_annotations",
        "//third_party/jvm/com/google/guava",
        "//third_party/jvm/com/googlecode/java_diff_utils:diffutils",
        "//third_party/jvm/junit",
        "//third_party/jvm/org/checkerframework:checker_compat_qual",
    ],
)
