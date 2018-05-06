licenses(["notice"])
java_library(
    name = "moshi",
    exports = [
        "//external:jar/io_bazel_rules_kotlin_com/squareup/moshi/moshi"
    ],
    runtime_deps = [
        "//third_party/jvm/com/squareup/okio:okio"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "moshi_kotlin",
    exports = [
        "//external:jar/io_bazel_rules_kotlin_com/squareup/moshi/moshi_kotlin"
    ],
    runtime_deps = [
        "//third_party/jvm/org/jetbrains/kotlin:kotlin_reflect",
        "//third_party/jvm/org/jetbrains/kotlin:kotlin_stdlib",
        ":moshi"
    ],
    visibility = [
        "//visibility:public"
    ]
)


