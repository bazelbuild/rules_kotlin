licenses(["notice"])

java_library(
    name = "dagger",
    visibility = [
        "//visibility:public",
    ],
    exports = [
        "//external:jar/io_bazel_rules_kotlin_com/google/dagger/dagger",
    ],
    runtime_deps = [
        "//third_party/jvm/javax/inject:javax_inject",
    ],
)

java_library(
    name = "dagger_compiler",
    visibility = [
        "//visibility:public",
    ],
    exports = [
        "//external:jar/io_bazel_rules_kotlin_com/google/dagger/dagger_compiler",
    ],
    runtime_deps = [
        ":dagger",
        ":dagger_producers",
        ":dagger_spi",
        "//third_party/jvm/com/google/googlejavaformat:google_java_format",
        "//third_party/jvm/com/google/guava",
        "//third_party/jvm/com/google/guava:failureaccess",
        "//third_party/jvm/com/google/protobuf:protobuf_java",
        "//third_party/jvm/com/squareup:javapoet",
        "//third_party/jvm/javax/annotation:jsr250_api",
        "//third_party/jvm/javax/inject:javax_inject",
        "//third_party/jvm/net/ltgt/gradle/incap",
        "//third_party/jvm/org/checkerframework:checker_compat_qual",
    ],
)

java_library(
    name = "dagger_producers",
    visibility = [
        "//visibility:public",
    ],
    exports = [
        "//external:jar/io_bazel_rules_kotlin_com/google/dagger/dagger_producers",
    ],
    runtime_deps = [
        ":dagger",
        "//third_party/jvm/com/google/guava",
        "//third_party/jvm/com/google/guava:failureaccess",
        "//third_party/jvm/javax/inject:javax_inject",
        "//third_party/jvm/org/checkerframework:checker_compat_qual",
    ],
)

java_library(
    name = "dagger_spi",
    visibility = [
        "//third_party/jvm:__subpackages__",
    ],
    exports = [
        "//external:jar/io_bazel_rules_kotlin_com/google/dagger/dagger_spi",
    ],
    runtime_deps = [
        ":dagger",
        ":dagger_producers",
        "//third_party/jvm/com/google/guava",
        "//third_party/jvm/com/google/guava:failureaccess",
        "//third_party/jvm/com/squareup:javapoet",
        "//third_party/jvm/javax/inject:javax_inject",
    ],
)
