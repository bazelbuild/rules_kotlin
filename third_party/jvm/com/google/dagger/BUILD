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
        "//third_party/jvm/com/google/googlejavaformat:google_java_format",
        "//third_party/jvm/com/google/guava:guava",
        "//third_party/jvm/com/squareup:javapoet",
        "//third_party/jvm/javax/annotation:jsr250_api",
        "//third_party/jvm/javax/inject:javax_inject",
        ":dagger",
        ":dagger_producers",
        ":dagger_spi"
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
        "//third_party/jvm/javax/inject:javax_inject",
        "//third_party/jvm/org/checkerframework:checker_compat_qual",
        ":dagger"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "dagger_spi",
    exports = [
        "//external:jar/io_bazel_rules_kotlin_com/google/dagger/dagger_spi"
    ],
    visibility = [
        "//visibility:public"
    ]
)


