licenses(["notice"])
java_library(
    name = "junit_jupiter_api",
    exports = [
        "//external:jar/io_bazel_rules_kotlin_org/junit/jupiter/junit_jupiter_api"
    ],
    runtime_deps = [
        "//third_party/jvm/org/apiguardian:apiguardian_api",
        "//third_party/jvm/org/junit/platform:junit_platform_commons",
        "//third_party/jvm/org/opentest4j:opentest4j"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "junit_jupiter_engine",
    exports = [
        "//external:jar/io_bazel_rules_kotlin_org/junit/jupiter/junit_jupiter_engine"
    ],
    runtime_deps = [
        "//third_party/jvm/org/apiguardian:apiguardian_api",
        "//third_party/jvm/org/junit/platform:junit_platform_engine",
        ":junit_jupiter_api"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "junit_jupiter_params",
    exports = [
        "//external:jar/io_bazel_rules_kotlin_org/junit/jupiter/junit_jupiter_params"
    ],
    runtime_deps = [
        "//third_party/jvm/org/apiguardian:apiguardian_api",
        ":junit_jupiter_api"
    ],
    visibility = [
        "//visibility:public"
    ]
)


