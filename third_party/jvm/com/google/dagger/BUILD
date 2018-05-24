licenses(["notice"])
java_library(
    name = "dagger",
    exports = [
        "//external:jar/io_bazel_rules_kotlin_com/google/dagger/dagger"
    ],
    runtime_deps = [
        "//third_party/jvm/javax/inject:javax_inject"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "dagger_compiler",
    exports = [
        "//external:jar/io_bazel_rules_kotlin_com/google/dagger/dagger_compiler"
    ],
    runtime_deps = [
        "//third_party/jvm/com/google/guava:guava",
        ":dagger",
        ":dagger_producers"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "dagger_producers",
    exports = [
        "//external:jar/io_bazel_rules_kotlin_com/google/dagger/dagger_producers"
    ],
    runtime_deps = [
        "//third_party/jvm/com/google/guava:guava",
        ":dagger"
    ],
    visibility = [
        "//visibility:public"
    ]
)


